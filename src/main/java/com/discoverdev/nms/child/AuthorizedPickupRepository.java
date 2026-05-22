package com.discoverdev.nms.child;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthorizedPickupRepository extends JpaRepository<AuthorizedPickup, UUID> {

    List<AuthorizedPickup> findByChildIdOrderByCreatedAtAsc(UUID childId);

    Optional<AuthorizedPickup> findByIdAndChildId(UUID id, UUID childId);
}
