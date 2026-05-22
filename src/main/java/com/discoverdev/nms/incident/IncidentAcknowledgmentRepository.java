package com.discoverdev.nms.incident;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IncidentAcknowledgmentRepository extends JpaRepository<IncidentAcknowledgment, UUID> {

    Optional<IncidentAcknowledgment> findByIncidentIdAndGuardianId(UUID incidentId, UUID guardianId);

    boolean existsByIncidentIdAndGuardianId(UUID incidentId, UUID guardianId);

    long countByIncidentId(UUID incidentId);

    List<IncidentAcknowledgment> findByIncidentId(UUID incidentId);
}
