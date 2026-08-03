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
 * "Delete for me" — one person's decision to stop seeing a message.
 *
 * Kept apart from {@code ChatMessage.deletedAt}, which is the sender or an admin
 * removing a message for everyone. The two look the same in the menu and are not
 * the same act: hiding is private and reversible and leaves the other side's
 * conversation untouched, and a row here must never remove anything for anyone
 * else.
 */
@Entity
@Table(
    name = "chat_message_hidden",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_message_hidden", columnNames = {"message_id", "user_id"}
    ),
    indexes = {
        @Index(name = "idx_hidden_user", columnList = "user_id"),
        @Index(name = "idx_hidden_message", columnList = "message_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessageHidden {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "message_id", nullable = false)
    private ChatMessage message;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @CreationTimestamp
    @Column(name = "hidden_at", nullable = false, updatable = false)
    private Instant hiddenAt;
}
