package com.discoverdev.nms.chat;

import com.discoverdev.nms.nursery.Nursery;
import com.discoverdev.nms.nursery.Room;
import com.discoverdev.nms.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
    name = "chat_threads",
    indexes = {
        @Index(name = "idx_thread_nursery", columnList = "nursery_id"),
        @Index(name = "idx_thread_room", columnList = "room_id"),
        @Index(name = "idx_thread_direct_key", columnList = "nursery_id, direct_key", unique = true),
        @Index(name = "idx_thread_last", columnList = "last_message_at")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatThread {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "nursery_id", nullable = false)
    private Nursery nursery;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ChatThreadType type;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id")
    private Room room;

    @Column(name = "direct_key", length = 100)
    private String directKey;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "last_message_at")
    private Instant lastMessageAt;

    public static String buildDirectKey(UUID a, UUID b) {
        String s1 = a.toString();
        String s2 = b.toString();
        return s1.compareTo(s2) < 0 ? s1 + "|" + s2 : s2 + "|" + s1;
    }

    /**
     * Key for a member's ADMIN_SUPPORT thread. It rides on the same unique
     * (nursery_id, direct_key) index as DMs; the "support:" prefix cannot collide
     * with a direct key, which is always two UUIDs joined by a pipe.
     */
    public static String buildSupportKey(UUID memberId) {
        return "support:" + memberId;
    }
}
