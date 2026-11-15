package com.discoverdev.nms.auth;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InviteRepository extends JpaRepository<Invite, UUID> {

    Optional<Invite> findByTokenHash(String tokenHash);

    // Newest invite for a user — to derive pending vs. expired status.
    Optional<Invite> findFirstByUserIdOrderByCreatedAtDesc(UUID userId);

    @Modifying
    @Query("update Invite i set i.usedAt = :now where i.user.id = :userId and i.usedAt is null")
    int closePendingForUser(@Param("userId") UUID userId, @Param("now") Instant now);
}
