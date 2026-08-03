package com.discoverdev.nms.chat;

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
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

/**
 * "Mute user" — one person choosing to stop being notified about another.
 *
 * Notifications only. The muted person's messages still appear in the thread, so
 * nothing is hidden from the conversation and nobody is silenced; the reader just
 * stops being interrupted. It is one-directional and private: being muted is not
 * visible to the muted user, and it does not mute the reader in return.
 *
 * Distinct from {@code ChatParticipant.muted}, which silences a whole thread
 * regardless of who is speaking.
 */
@Entity
@Table(
    name = "user_mutes",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_user_mute", columnNames = {"muter_id", "muted_id"}
    ),
    indexes = {
        @Index(name = "idx_mute_muter", columnList = "muter_id"),
        @Index(name = "idx_mute_pair", columnList = "muter_id, muted_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserMute {

    @Id
    @GeneratedValue
    private UUID id;

    /** The person who no longer wants to be notified. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "muter_id", nullable = false)
    private User muter;

    /** The person whose messages stop producing notifications. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "muted_id", nullable = false)
    private User muted;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
