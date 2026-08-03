package com.discoverdev.nms.announcement;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AnnouncementRepository extends JpaRepository<Announcement, UUID> {

    Optional<Announcement> findByIdAndNurseryId(UUID id, UUID nurseryId);

    List<Announcement> findByNurseryIdOrderByCreatedAtDesc(UUID nurseryId);

    List<Announcement> findByNurseryIdAndCreatedAtBetweenOrderByCreatedAtDesc(
        UUID nurseryId, Instant from, Instant to);

    List<Announcement> findByNurseryIdAndRoomIdOrderByCreatedAtDesc(UUID nurseryId, UUID roomId);

    @Query("""
        select a from Announcement a
        where a.nursery.id = :nurseryId
          and (a.scope = com.discoverdev.nms.announcement.AnnouncementScope.NURSERY
               or a.room.id in :roomIds)
        order by a.createdAt desc
        """)
    List<Announcement> findVisibleToRooms(
        @Param("nurseryId") UUID nurseryId,
        @Param("roomIds") Collection<UUID> roomIds);

    /**
     * The announcements carrying this attachment. Normally one, but the same
     * media can be reused across posts, and being able to read any one of them
     * is enough to be allowed the file.
     */
    @Query("""
        select a from Announcement a
        join a.attachments m
        where m.id = :mediaId and a.nursery.id = :nurseryId
        """)
    List<Announcement> findByAttachmentId(
        @Param("mediaId") UUID mediaId,
        @Param("nurseryId") UUID nurseryId);
}
