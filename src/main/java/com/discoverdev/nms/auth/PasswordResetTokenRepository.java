package com.discoverdev.nms.auth;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {

    Optional<PasswordResetToken> findByTokenHash(String tokenHash);

    // Clear any prior reset attempts for the user before issuing a new OTP.
    void deleteByUserId(UUID userId);
}
