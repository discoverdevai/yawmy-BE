package com.discoverdev.nms.child;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChildRepository extends JpaRepository<Child, UUID> {

    /** Enrolled-children counts per room, for the rooms list. */
    interface RoomChildCount {
        UUID getRoomId();
        long getCount();
    }

    @Query("""
        select c.room.id as roomId, count(c) as count
        from Child c
        where c.nursery.id = :nurseryId and c.enrollmentStatus = :status and c.room.id is not null
        group by c.room.id
        """)
    List<RoomChildCount> countEnrolledGroupedByRoom(
        @Param("nurseryId") UUID nurseryId, @Param("status") EnrollmentStatus status);

    Optional<Child> findByIdAndNurseryId(UUID id, UUID nurseryId);

    List<Child> findByNurseryIdOrderByFullNameAsc(UUID nurseryId);

    List<Child> findByNurseryIdAndEnrollmentStatusOrderByFullNameAsc(
        UUID nurseryId, EnrollmentStatus status);

    long countByNurseryIdAndEnrollmentStatus(UUID nurseryId, EnrollmentStatus status);

    List<Child> findByNurseryIdAndRoomIdAndEnrollmentStatusOrderByFullNameAsc(
        UUID nurseryId, UUID roomId, EnrollmentStatus status);

    List<Child> findByNurseryIdAndRoomIdOrderByFullNameAsc(UUID nurseryId, UUID roomId);

    // Current occupancy of a room (enrolled children) — used to enforce capacity.
    long countByRoomIdAndEnrollmentStatus(UUID roomId, EnrollmentStatus status);
}
