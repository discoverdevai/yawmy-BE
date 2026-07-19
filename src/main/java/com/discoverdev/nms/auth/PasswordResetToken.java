package com.discoverdev.nms.auth;

import com.discoverdev.nms.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(
    name = "password_reset_tokens",
    indexes = {
        @Index(name = "idx_pw_reset_token_hash", columnList = "token_hash", unique = true),
        @Index(name = "idx_pw_reset_user_id", columnList = "user_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PasswordResetToken {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Opaque reset token handed to the client; used on the final reset call.
    @Column(name = "token_hash", nullable = false, unique = true, length = 128)
    private String tokenHash;

    // Hashed 4-digit OTP that is emailed to the user and entered on the verify screen.
    @Column(name = "otp_hash", length = 128)
    private String otpHash;

    // OTP expiry (short — e.g. 30s). expiresAt refers to the OTP, not the whole session.
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    // Set once the OTP is verified; the final reset requires this to be non-null.
    @Column(name = "verified_at")
    private Instant verifiedAt;

    @Column(name = "used_at")
    private Instant usedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public boolean isUsed() {
        return usedAt != null;
    }

    /** OTP expiry check. */
    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    public boolean isVerified() {
        return verifiedAt != null;
    }
}
