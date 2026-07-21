package com.discoverdev.nms.nursery;

import com.discoverdev.nms.admin.dto.RoomResponse;
import com.discoverdev.nms.child.ChildRepository;
import com.discoverdev.nms.child.EnrollmentStatus;
import com.discoverdev.nms.media.MediaStorage;
import com.discoverdev.nms.tenant.TenantContext;
import com.discoverdev.nms.user.User;
import java.io.IOException;
import java.util.Set;
import java.util.UUID;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class RoomImageService {

    private static final long MAX_BYTES = 5L * 1024 * 1024;
    private static final Set<String> ALLOWED_TYPES = Set.of(
        MediaType.IMAGE_JPEG_VALUE, MediaType.IMAGE_PNG_VALUE, "image/webp"
    );

    private final RoomRepository roomRepository;
    private final ChildRepository childRepository;
    private final MediaStorage storage;

    public RoomImageService(
        RoomRepository roomRepository, ChildRepository childRepository, MediaStorage storage
    ) {
        this.roomRepository = roomRepository;
        this.childRepository = childRepository;
        this.storage = storage;
    }

    @Transactional
    public RoomResponse upload(UUID roomId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File is required");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "File exceeds 5 MB limit");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType)) {
            throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                "Only JPEG, PNG, or WEBP images are accepted");
        }
        Room room = roomRepository.findByIdAndNurseryId(roomId, TenantContext.currentNurseryId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found"));

        String ext = extensionFor(contentType);
        String key = "rooms/" + room.getId() + "." + ext;
        try {
            if (room.getImageKey() != null && !room.getImageKey().equals(key)) {
                storage.delete(room.getImageKey());
            }
            storage.store(key, file.getInputStream(), file.getSize(), contentType);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to store file", e);
        }
        room.setImageKey(key);
        room.setImageUrl("/api/files/rooms/" + room.getId() + "/image");
        Room saved = roomRepository.save(room);
        long count = childRepository.countByRoomIdAndEnrollmentStatus(
            saved.getId(), EnrollmentStatus.ENROLLED);
        return RoomResponse.of(saved, count);
    }

    @Transactional(readOnly = true)
    public ImageStream loadFor(UUID roomId, User requester) {
        Room room = roomRepository.findById(roomId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Image not found"));
        if (requester.getNursery() == null
            || !requester.getNursery().getId().equals(room.getNursery().getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Image not found");
        }
        if (room.getImageKey() == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Image not found");
        }
        try {
            Resource resource = storage.load(room.getImageKey());
            return new ImageStream(resource, contentTypeFor(room.getImageKey()));
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to read file", e);
        }
    }

    private String extensionFor(String contentType) {
        return switch (contentType) {
            case MediaType.IMAGE_JPEG_VALUE -> "jpg";
            case MediaType.IMAGE_PNG_VALUE -> "png";
            case "image/webp" -> "webp";
            default -> "bin";
        };
    }

    private String contentTypeFor(String key) {
        int dot = key.lastIndexOf('.');
        if (dot < 0) return MediaType.APPLICATION_OCTET_STREAM_VALUE;
        return switch (key.substring(dot + 1).toLowerCase()) {
            case "jpg", "jpeg" -> MediaType.IMAGE_JPEG_VALUE;
            case "png" -> MediaType.IMAGE_PNG_VALUE;
            case "webp" -> "image/webp";
            default -> MediaType.APPLICATION_OCTET_STREAM_VALUE;
        };
    }

    public record ImageStream(Resource resource, String contentType) {}
}
