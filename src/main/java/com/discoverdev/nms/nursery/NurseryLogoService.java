package com.discoverdev.nms.nursery;

import com.discoverdev.nms.admin.dto.NurseryResponse;
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
public class NurseryLogoService {

    private static final long MAX_BYTES = 5L * 1024 * 1024;
    private static final Set<String> ALLOWED_TYPES = Set.of(
        MediaType.IMAGE_JPEG_VALUE, MediaType.IMAGE_PNG_VALUE, "image/webp"
    );

    private final NurseryRepository nurseryRepository;
    private final MediaStorage storage;

    public NurseryLogoService(NurseryRepository nurseryRepository, MediaStorage storage) {
        this.nurseryRepository = nurseryRepository;
        this.storage = storage;
    }

    @Transactional
    public NurseryResponse upload(MultipartFile file) {
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
        Nursery nursery = load(TenantContext.currentNurseryId());

        String ext = extensionFor(contentType);
        String key = "nurseries/" + nursery.getId() + "." + ext;
        try {
            if (nursery.getLogoKey() != null && !nursery.getLogoKey().equals(key)) {
                storage.delete(nursery.getLogoKey());
            }
            storage.store(key, file.getInputStream(), file.getSize(), contentType);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to store file", e);
        }
        nursery.setLogoKey(key);
        nursery.setLogoUrl("/api/files/nursery/" + nursery.getId() + "/logo");
        return NurseryResponse.of(nurseryRepository.save(nursery));
    }

    @Transactional(readOnly = true)
    public LogoStream loadFor(UUID nurseryId, User requester) {
        if (requester.getNursery() == null || !requester.getNursery().getId().equals(nurseryId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Logo not found");
        }
        Nursery nursery = load(nurseryId);
        if (nursery.getLogoKey() == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Logo not found");
        }
        try {
            Resource resource = storage.load(nursery.getLogoKey());
            return new LogoStream(resource, contentTypeFor(nursery.getLogoKey()));
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to read file", e);
        }
    }

    private Nursery load(UUID id) {
        return nurseryRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Nursery not found"));
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

    public record LogoStream(Resource resource, String contentType) {}
}