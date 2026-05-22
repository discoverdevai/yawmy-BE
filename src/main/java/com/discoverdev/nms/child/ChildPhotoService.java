package com.discoverdev.nms.child;

import com.discoverdev.nms.child.dto.ChildResponse;
import com.discoverdev.nms.media.MediaStorage;
import com.discoverdev.nms.tenant.TenantContext;
import com.discoverdev.nms.user.Role;
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
public class ChildPhotoService {

    private static final long MAX_BYTES = 5L * 1024 * 1024;
    private static final Set<String> ALLOWED_TYPES = Set.of(
        MediaType.IMAGE_JPEG_VALUE, MediaType.IMAGE_PNG_VALUE, "image/webp"
    );

    private final ChildRepository childRepository;
    private final GuardianLinkRepository guardianLinkRepository;
    private final MediaStorage storage;

    public ChildPhotoService(
        ChildRepository childRepository,
        GuardianLinkRepository guardianLinkRepository,
        MediaStorage storage
    ) {
        this.childRepository = childRepository;
        this.guardianLinkRepository = guardianLinkRepository;
        this.storage = storage;
    }

    @Transactional
    public ChildResponse upload(UUID childId, MultipartFile file) {
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
        Child child = childRepository.findByIdAndNurseryId(childId, TenantContext.currentNurseryId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Child not found"));

        String ext = extensionFor(contentType);
        String key = "children/" + child.getId() + "." + ext;
        try {
            if (child.getPhotoKey() != null && !child.getPhotoKey().equals(key)) {
                storage.delete(child.getPhotoKey());
            }
            storage.store(key, file.getInputStream(), file.getSize(), contentType);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to store file", e);
        }
        child.setPhotoKey(key);
        return ChildResponse.of(childRepository.save(child));
    }

    @Transactional
    public void delete(UUID childId) {
        Child child = childRepository.findByIdAndNurseryId(childId, TenantContext.currentNurseryId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Child not found"));
        if (child.getPhotoKey() == null) return;
        try {
            storage.delete(child.getPhotoKey());
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to delete file", e);
        }
        child.setPhotoKey(null);
        childRepository.save(child);
    }

    @Transactional(readOnly = true)
    public PhotoStream loadFor(UUID childId, User requester) {
        Child child = childRepository.findById(childId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Photo not found"));
        if (!canSee(child, requester)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Photo not found");
        }
        if (child.getPhotoKey() == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Photo not found");
        }
        try {
            Resource resource = storage.load(child.getPhotoKey());
            return new PhotoStream(resource, contentTypeFor(child.getPhotoKey()));
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to read file", e);
        }
    }

    private boolean canSee(Child child, User requester) {
        if (requester.getNursery() == null) return false;
        UUID requesterNursery = requester.getNursery().getId();
        UUID childNursery = child.getNursery().getId();
        Role role = requester.getRole();
        if ((role == Role.ADMIN || role == Role.TEACHER) && requesterNursery.equals(childNursery)) {
            return true;
        }
        if (role == Role.PARENT) {
            return guardianLinkRepository.existsByChildIdAndGuardianId(child.getId(), requester.getId());
        }
        return false;
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

    public record PhotoStream(Resource resource, String contentType) {}
}
