package com.discoverdev.nms.user;

import com.discoverdev.nms.admin.dto.ImageUrlResponse;
import com.discoverdev.nms.media.MediaStorage;
import com.discoverdev.nms.tenant.TenantContext;
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

/** Profile image for a staff (teacher) user — same pattern as the room/nursery images. */
@Service
public class UserImageService {

    private static final long MAX_BYTES = 5L * 1024 * 1024;
    private static final Set<String> ALLOWED_TYPES = Set.of(
        MediaType.IMAGE_JPEG_VALUE, MediaType.IMAGE_PNG_VALUE, "image/webp"
    );

    private final UserRepository userRepository;
    private final MediaStorage storage;

    public UserImageService(UserRepository userRepository, MediaStorage storage) {
        this.userRepository = userRepository;
        this.storage = storage;
    }

    @Transactional
    public ImageUrlResponse upload(UUID userId, MultipartFile file) {
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
        User user = loadInNursery(userId, TenantContext.currentNurseryId());

        String ext = extensionFor(contentType);
        String key = "users/" + user.getId() + "." + ext;
        try {
            if (user.getImageKey() != null && !user.getImageKey().equals(key)) {
                storage.delete(user.getImageKey());
            }
            storage.store(key, file.getInputStream(), file.getSize(), contentType);
        } catch (IOException e) {
            // Reason, not just cause — Spring's error body is built from the
            // reason, so a bare string hides which storage problem this was.
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                "Failed to store file: " + e.getMessage(), e);
        }
        user.setImageKey(key);
        String url = "/api/files/users/" + user.getId() + "/image";
        user.setImageUrl(url);
        userRepository.save(user);
        return new ImageUrlResponse(url);
    }

    @Transactional(readOnly = true)
    public ImageStream loadFor(UUID userId, User requester) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Image not found"));
        if (requester.getNursery() == null || user.getNursery() == null
            || !requester.getNursery().getId().equals(user.getNursery().getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Image not found");
        }
        if (user.getImageKey() == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Image not found");
        }
        try {
            Resource resource = storage.load(user.getImageKey());
            return new ImageStream(resource, contentTypeFor(user.getImageKey()));
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to read file", e);
        }
    }

    private User loadInNursery(UUID userId, UUID nurseryId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Staff member not found"));
        if (user.getNursery() == null || !nurseryId.equals(user.getNursery().getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Staff member not found");
        }
        return user;
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
