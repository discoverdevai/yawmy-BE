package com.discoverdev.nms.event;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NurseryEventRepository extends JpaRepository<NurseryEvent, UUID> {

    Optional<NurseryEvent> findByIdAndNurseryId(UUID id, UUID nurseryId);

    List<NurseryEvent> findByNurseryIdOrderByStartsAtAsc(UUID nurseryId);

    List<NurseryEvent> findByNurseryIdAndStartsAtGreaterThanEqualOrderByStartsAtAsc(
        UUID nurseryId, Instant from);

    @Query("""
        select e from NurseryEvent e
        where e.nursery.id = :nurseryId
          and (e.scope = com.discoverdev.nms.announcement.AnnouncementScope.NURSERY
               or e.room.id in :roomIds)
        order by e.startsAt asc
        """)
    List<NurseryEvent> findVisibleToRooms(
        @Param("nurseryId") UUID nurseryId,
        @Param("roomIds") Collection<UUID> roomIds);

    @Query("""
        select e from NurseryEvent e
        where e.nursery.id = :nurseryId
          and (e.scope = com.discoverdev.nms.announcement.AnnouncementScope.NURSERY
               or e.room.id in :roomIds)
          and e.startsAt >= :from
        order by e.startsAt asc
        """)
    List<NurseryEvent> findUpcomingForRooms(
        @Param("nurseryId") UUID nurseryId,
        @Param("roomIds") Collection<UUID> roomIds,
        @Param("from") Instant from);
}
