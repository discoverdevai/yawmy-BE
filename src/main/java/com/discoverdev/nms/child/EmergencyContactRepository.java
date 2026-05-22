package com.discoverdev.nms.child;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmergencyContactRepository extends JpaRepository<EmergencyContact, UUID> {

    List<EmergencyContact> findByChildIdOrderByCreatedAtAsc(UUID childId);

    Optional<EmergencyContact> findByIdAndChildId(UUID id, UUID childId);
}
