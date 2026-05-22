package com.discoverdev.nms.incident;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IncidentReportRepository extends JpaRepository<IncidentReport, UUID> {

    Optional<IncidentReport> findByIdAndNurseryId(UUID id, UUID nurseryId);

    List<IncidentReport> findByNurseryIdOrderByIncidentAtDesc(UUID nurseryId);

    List<IncidentReport> findByNurseryIdAndIncidentAtBetweenOrderByIncidentAtDesc(
        UUID nurseryId, Instant from, Instant to);

    List<IncidentReport> findByChildIdOrderByIncidentAtDesc(UUID childId);

    List<IncidentReport> findByRoomIdOrderByIncidentAtDesc(UUID roomId);

    List<IncidentReport> findByRoomIdAndIncidentAtBetweenOrderByIncidentAtDesc(
        UUID roomId, Instant from, Instant to);
}
