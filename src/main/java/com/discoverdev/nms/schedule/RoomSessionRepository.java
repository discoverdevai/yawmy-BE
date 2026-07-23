package com.discoverdev.nms.schedule;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomSessionRepository extends JpaRepository<RoomSession, UUID> {

    List<RoomSession> findByNurseryIdAndRoomIdAndSessionDateOrderByStartTimeAsc(
        UUID nurseryId, UUID roomId, LocalDate sessionDate);

    Optional<RoomSession> findByIdAndNurseryId(UUID id, UUID nurseryId);
}
