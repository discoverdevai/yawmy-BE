package com.discoverdev.nms.auth;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InviteRepository extends JpaRepository<Invite, UUID> {

    Optional<Invite> findByTokenHash(String tokenHash);

    // Newest invite for a user — to derive pending vs. expired status.
    Optional<Invite> findFirstByUserIdOrderByCreatedAtDesc(UUID userId);
}
