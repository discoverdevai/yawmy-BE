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
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class MediaWriter {

    static final long MAX_PHOTO_BYTES = 5L * 1024 * 1024;
    static final long MAX_VIDEO_BYTES = 50L * 1024 * 1024;

    static final Map<String, String> PHOTO_EXT = Map.of(
        "image/jpeg", "jpg",
        "image/png", "png",
        "image/webp", "webp"
    );

    static final Map<String, String> VIDEO_EXT = Map.of(
        "video/mp4", "mp4",
        "video/quicktime", "mov"
    );

    static final Set<String> ALL_TYPES;
    static {
        ALL_TYPES = new java.util.HashSet<>();
        ALL_TYPES.addAll(PHOTO_EXT.keySet());
        ALL_TYPES.addAll(VIDEO_EXT.keySet());
    }

    private final MediaItemRepository repository;
    private final MediaStorage storage;
    private final ApplicationEventPublisher events;

    public MediaWriter(
        MediaItemRepository repository,
        MediaStorage storage,
        ApplicationEventPublisher events
    ) {
        this.repository = repository;
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
                boolean isPhoto = PHOTO_EXT.containsKey(contentType);
                long limit = isPhoto ? MAX_PHOTO_BYTES : MAX_VIDEO_BYTES;
                if (file.getSize() > limit) {
                    failed.add(new BulkUploadResponse.FailedUpload(name,
                        String.format("exceeds %d MB limit", limit / (1024 * 1024))));
                    continue;
                }
                String ext = isPhoto ? PHOTO_EXT.get(contentType) : VIDEO_EXT.get(contentType);
                UUID storageId = UUID.randomUUID();
                String key = "media/" + nursery.getId() + "/" + storageId + "." + ext;
                storage.store(key, file.getInputStream(), file.getSize(), contentType);

                MediaItem item = MediaItem.builder()
                    .nursery(nursery)
                    .uploader(uploader)
                    .child(child)
                    .room(room)
                    .type(isPhoto ? MediaType.PHOTO : MediaType.VIDEO)
                    .storageKey(key)
                    .contentType(contentType)
                    .sizeBytes(file.getSize())
                    .caption(caption)
                    .build();
                MediaItem saved = repository.save(item);
                uploaded.add(MediaItemResponse.of(saved));
                savedIds.add(saved.getId());
            } catch (IOException e) {
                failed.add(new BulkUploadResponse.FailedUpload(name, "storage failure: " + e.getMessage()));
            } catch (RuntimeException e) {
                failed.add(new BulkUploadResponse.FailedUpload(name, e.getMessage()));
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

    public void softDelete(MediaItem item) {
        item.setDeletedAt(Instant.now());
        repository.save(item);
    }
}
