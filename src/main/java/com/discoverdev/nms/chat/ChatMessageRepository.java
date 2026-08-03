package com.discoverdev.nms.chat;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, UUID> {

    List<ChatMessage> findByThreadIdAndDeletedAtIsNullOrderByCreatedAtDesc(UUID threadId, Limit limit);

    List<ChatMessage> findByThreadIdAndCreatedAtLessThanAndDeletedAtIsNullOrderByCreatedAtDesc(
        UUID threadId, Instant before, Limit limit);

    /** Forward cursor — what arrived since `after`, oldest first, for the polling fallback. */
    List<ChatMessage> findByThreadIdAndCreatedAtGreaterThanAndDeletedAtIsNullOrderByCreatedAtAsc(
        UUID threadId, Instant after, Limit limit);

    Optional<ChatMessage> findTopByThreadIdAndDeletedAtIsNullOrderByCreatedAtDesc(UUID threadId);

    long countByThreadIdAndCreatedAtGreaterThanAndDeletedAtIsNull(UUID threadId, Instant after);

    /**
     * Unread for one reader. Their own messages are excluded — sending something
     * and immediately seeing "1 unread" against your own words is nonsense, and
     * that is what counting by timestamp alone produces.
     */
    long countByThreadIdAndCreatedAtGreaterThanAndSenderIdNotAndDeletedAtIsNull(
        UUID threadId, Instant after, UUID senderId);

    long countByThreadIdAndDeletedAtIsNull(UUID threadId);

    /**
     * Whether this media is attached to a message in a thread the user is still
     * in. Deleted messages are excluded — deleting a message has to put its photo
     * out of reach too, or "delete" only removes the caption.
     */
    @Query("""
        select count(m) > 0 from ChatMessage m
        join m.attachments a
        where a.id = :mediaId
          and m.deletedAt is null
          and exists (
            select 1 from ChatParticipant p
            where p.thread = m.thread
              and p.user.id = :userId
              and p.leftAt is null
          )
        """)
    boolean existsVisibleAttachment(@Param("mediaId") UUID mediaId, @Param("userId") UUID userId);
}
