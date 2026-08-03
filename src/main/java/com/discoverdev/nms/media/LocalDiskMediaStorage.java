package com.discoverdev.nms.media;

import com.discoverdev.nms.config.MediaProperties;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

// Registered by MediaStorageConfig rather than component-scanned: with @Component
// this and S3MediaStorage would both satisfy the single MediaStorage injection.
public class LocalDiskMediaStorage implements MediaStorage {

    private final Path root;

    public LocalDiskMediaStorage(MediaProperties props) {
        String configured = props.local() != null ? props.local().root() : null;
        this.root = Paths.get(configured != null && !configured.isBlank() ? configured : "uploads")
            .toAbsolutePath().normalize();
    }

    @Override
    public void store(String key, InputStream content, long contentLength, String contentType) throws IOException {
        Path target = resolveSafe(key);
        Files.createDirectories(target.getParent());
        Files.copy(content, target, StandardCopyOption.REPLACE_EXISTING);
    }

    @Override
    public Resource load(String key) throws IOException {
        Path target = resolveSafe(key);
        if (!Files.exists(target) || !Files.isRegularFile(target)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found");
        }
        return new PathResource(target);
    }

    @Override
    public void delete(String key) throws IOException {
        Path target = resolveSafe(key);
        Files.deleteIfExists(target);
    }

    @Override
    public boolean exists(String key) {
        try {
            Path target = resolveSafe(key);
            return Files.exists(target) && Files.isRegularFile(target);
        } catch (IOException e) {
            return false;
        }
    }

    private Path resolveSafe(String key) throws IOException {
        if (key == null || key.isBlank()) {
            throw new IOException("Empty storage key");
        }
        Path normalized = root.resolve(key).normalize();
        if (!normalized.startsWith(root)) {
            throw new IOException("Storage key escapes root: " + key);
        }
        return normalized;
    }
}
