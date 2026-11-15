package com.discoverdev.nms.media;

import com.discoverdev.nms.child.EnrollmentStatus;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class UnenrolledChildMediaPurge {

    private static final Logger log = LoggerFactory.getLogger(UnenrolledChildMediaPurge.class);
    private static final Duration RETENTION = Duration.ofDays(30);

    private final MediaItemRepository repository;
    private final MediaStorage storage;

    public UnenrolledChildMediaPurge(MediaItemRepository repository, MediaStorage storage) {
        this.repository = repository;
        this.storage = storage;
    }

    @Scheduled(cron = "0 30 3 * * *")
    @Transactional
    public void purge() {
        Instant now = Instant.now();
        List<MediaItem> expired = repository
            .findByChild_EnrollmentStatusAndChild_UnenrolledAtBeforeAndDeletedAtIsNull(
                EnrollmentStatus.UNENROLLED, now.minus(RETENTION));
        List<MediaItem> purged = new ArrayList<>();
        for (MediaItem item : expired) {
            try {
                storage.delete(item.getStorageKey());
            } catch (IOException | RuntimeException e) {
                log.warn("Could not delete media file {}: {}", item.getStorageKey(), e.getMessage());
                continue;
            }
            item.setDeletedAt(now);
            purged.add(item);
        }
        repository.saveAll(purged);
        if (!purged.isEmpty()) {
            log.info("Deleted {} media items of children unenrolled over {} days ago",
                purged.size(), RETENTION.toDays());
        }
    }
}
