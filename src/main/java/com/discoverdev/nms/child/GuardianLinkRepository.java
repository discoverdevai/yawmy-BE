package com.discoverdev.nms.child;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GuardianLinkRepository extends JpaRepository<GuardianLink, UUID> {

    List<GuardianLink> findByChildIdOrderByCreatedAtAsc(UUID childId);

    List<GuardianLink> findByGuardianIdOrderByCreatedAtAsc(UUID guardianId);

    Optional<GuardianLink> findByChildIdAndGuardianId(UUID childId, UUID guardianId);

    boolean existsByChildIdAndGuardianId(UUID childId, UUID guardianId);
}
