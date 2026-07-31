package com.discoverdev.nms.media;

import com.discoverdev.nms.child.Child;
import com.discoverdev.nms.media.dto.BulkUploadResponse;
import com.discoverdev.nms.media.dto.MediaItemResponse;
import com.discoverdev.nms.nursery.Nursery;
import com.discoverdev.nms.nursery.Room;
import com.discoverdev.nms.user.User;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionException;
import org.springframework.web.multipart.MultipartFile;

@Component
public class MediaWriter {

    static final long MAX_PHOTO_BYTES = 5L * 1024 * 1024;
    static final long MAX_VIDEO_BYTES = 50L * 1024 * 1024;
    static final long MAX_DOC_BYTES = 25L * 1024 * 1024;

    static final Map<String, String> PHOTO_EXT = Map.of(
        "image/jpeg", "jpg",
        "image/png", "png",
        "image/webp", "webp"
    );

    static final Map<String, String> VIDEO_EXT = Map.of(
        "video/mp4", "mp4",
        "video/quicktime", "mov"
    );

    // Documents: PDF, Excel (xlsx/xls) and Word (docx/doc).
    static final Map<String, String> DOC_EXT = Map.of(
        "application/pdf", "pdf",
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "xlsx",
        "application/vnd.ms-excel", "xls",
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "docx",
        "application/msword", "doc"
    );

    static final Set<String> ALL_TYPES;
    static {
        ALL_TYPES = new java.util.HashSet<>();
        ALL_TYPES.addAll(PHOTO_EXT.keySet());
        ALL_TYPES.addAll(VIDEO_EXT.keySet());
        ALL_TYPES.addAll(DOC_EXT.keySet());
    }

    private static final Logger log = LoggerFactory.getLogger(MediaWriter.class);

    private final MediaItemRepository repository;
    private final MediaItemPersister persister;
    private final MediaStorage storage;
    private final ApplicationEventPublisher events;

    public MediaWriter(
        MediaItemRepository repository,
        MediaItemPersister persister,
        MediaStorage storage,
        ApplicationEventPublisher events
    ) {
        this.repository = repository;
        this.persister = persister;
        this.storage = storage;
        this.events = events;
    }

    public BulkUploadResponse writeForChild(
        Nursery nursery, Child child, List<MultipartFile> files, String caption, User uploader
    ) {
        return write(nursery, child, null, files, caption, uploader);
    }

    public BulkUploadResponse writeForRoom(
        Nursery nursery, Room room, List<MultipartFile> files, String caption, User uploader
    ) {
        return write(nursery, null, room, files, caption, uploader);
    }

    /** Nursery-scoped media not tied to a child or room (e.g. announcement attachments). */
    public BulkUploadResponse writeForNursery(
        Nursery nursery, List<MultipartFile> files, String caption, User uploader
    ) {
        return write(nursery, null, null, files, caption, uploader);
    }

    private BulkUploadResponse write(
        Nursery nursery, Child child, Room room,
        List<MultipartFile> files, String caption, User uploader
    ) {
        List<MediaItemResponse> uploaded = new ArrayList<>();
        List<BulkUploadResponse.FailedUpload> failed = new ArrayList<>();
        List<UUID> savedIds = new ArrayList<>();

        for (MultipartFile file : files) {
            String name = file.getOriginalFilename() != null ? file.getOriginalFilename() : "(unnamed)";
            try {
                if (file.isEmpty()) {
                    failed.add(new BulkUploadResponse.FailedUpload(name, "empty file"));
                    continue;
                }
                String contentType = file.getContentType();
                if (contentType == null || !ALL_TYPES.contains(contentType)) {
                    failed.add(new BulkUploadResponse.FailedUpload(name,
                        "unsupported content type: " + contentType));
                    continue;
                }
                MediaType type;
                String ext;
                long limit;
                if (PHOTO_EXT.containsKey(contentType)) {
                    type = MediaType.PHOTO;
                    ext = PHOTO_EXT.get(contentType);
                    limit = MAX_PHOTO_BYTES;
                } else if (VIDEO_EXT.containsKey(contentType)) {
                    type = MediaType.VIDEO;
                    ext = VIDEO_EXT.get(contentType);
                    limit = MAX_VIDEO_BYTES;
                } else {
                    type = MediaType.DOCUMENT;
                    ext = DOC_EXT.get(contentType);
                    limit = MAX_DOC_BYTES;
                }
                if (file.getSize() > limit) {
                    failed.add(new BulkUploadResponse.FailedUpload(name,
                        String.format("exceeds %d MB limit", limit / (1024 * 1024))));
                    continue;
                }
                UUID storageId = UUID.randomUUID();
                String key = "media/" + nursery.getId() + "/" + storageId + "." + ext;
                storage.store(key, file.getInputStream(), file.getSize(), contentType);

                MediaItem item = MediaItem.builder()
                    .nursery(nursery)
                    .uploader(uploader)
                    .child(child)
                    .room(room)
                    .type(type)
                    .storageKey(key)
                    .contentType(contentType)
                    .sizeBytes(file.getSize())
                    .caption(caption)
                    .build();
                MediaItem saved;
                try {
                    saved = persister.saveNow(item);
                } catch (RuntimeException e) {
                    // The bytes are already on disk. Drop them so a row the
                    // database refused doesn't leave an unreferenced file behind.
                    discard(key);
                    throw e;
                }
                uploaded.add(MediaItemResponse.of(saved));
                savedIds.add(saved.getId());
            } catch (IOException e) {
                failed.add(new BulkUploadResponse.FailedUpload(name, "storage failure: " + e.getMessage()));
            } catch (RuntimeException e) {
                failed.add(new BulkUploadResponse.FailedUpload(name, reasonFor(name, e)));
            }
        }

        if (!savedIds.isEmpty()) {
            events.publishEvent(new MediaUploadedEvent(
                nursery.getId(),
                child != null ? child.getId() : null,
                room != null ? room.getId() : null,
                savedIds,
                uploader.getId()
            ));
        }
        return new BulkUploadResponse(uploaded, failed);
    }

    /**
     * Why a single file failed, in words the uploader can act on. The apps show
     * this string verbatim, and a database message would hand them the failing
     * SQL statement and the schema — so those get logged, not returned.
     */
    private String reasonFor(String name, RuntimeException e) {
        if (e instanceof DataAccessException || e instanceof TransactionException) {
            log.error("Database rejected the row for '{}': {}", name, e.getMessage(), e);
            return "could not be saved — please try again";
        }
        String message = e.getMessage();
        return message == null || message.isBlank() ? "could not be uploaded" : message;
    }

    /** Best-effort cleanup — a leftover blob must never fail the upload itself. */
    private void discard(String key) {
        try {
            storage.delete(key);
        } catch (IOException | RuntimeException e) {
            log.warn("Could not remove orphaned upload {}: {}", key, e.getMessage());
        }
    }

    public void softDelete(MediaItem item) {
        item.setDeletedAt(Instant.now());
        repository.save(item);
    }
}
