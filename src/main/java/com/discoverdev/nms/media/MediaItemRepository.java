package com.discoverdev.nms.media;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MediaItemRepository extends JpaRepository<MediaItem, UUID> {

    Optional<MediaItem> findByIdAndDeletedAtIsNull(UUID id);

    Optional<MediaItem> findByIdAndNurseryIdAndDeletedAtIsNull(UUID id, UUID nurseryId);

    List<MediaItem> findByChildIdAndDeletedAtIsNullOrderByUploadedAtDesc(UUID childId);

    List<MediaItem> findByChildIdAndUploadedAtBetweenAndDeletedAtIsNullOrderByUploadedAtDesc(
        UUID childId, Instant from, Instant to);

    List<MediaItem> findByRoomIdAndDeletedAtIsNullOrderByUploadedAtDesc(UUID roomId);

    List<MediaItem> findByRoomIdAndUploadedAtBetweenAndDeletedAtIsNullOrderByUploadedAtDesc(
        UUID roomId, Instant from, Instant to);

    List<MediaItem> findByNurseryIdAndDeletedAtIsNullOrderByUploadedAtDesc(UUID nurseryId);

    List<MediaItem> findByNurseryIdAndUploadedAtBetweenAndDeletedAtIsNullOrderByUploadedAtDesc(
        UUID nurseryId, Instant from, Instant to);
}
