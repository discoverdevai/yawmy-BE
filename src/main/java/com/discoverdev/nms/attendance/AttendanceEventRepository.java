package com.discoverdev.nms.attendance;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AttendanceEventRepository extends JpaRepository<AttendanceEvent, UUID> {

    List<AttendanceEvent> findByChildIdAndRecordedAtBetweenOrderByRecordedAtAsc(
        UUID childId, Instant from, Instant to);

    List<AttendanceEvent> findByNurseryIdAndRecordedAtBetween(
        UUID nurseryId, Instant from, Instant to);

    List<AttendanceEvent> findByRoomIdAndRecordedAtBetween(
        UUID roomId, Instant from, Instant to);
}
