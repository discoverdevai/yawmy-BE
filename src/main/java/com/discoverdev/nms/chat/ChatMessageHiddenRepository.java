package com.discoverdev.nms.chat;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChatMessageHiddenRepository extends JpaRepository<ChatMessageHidden, UUID> {

    Optional<ChatMessageHidden> findByMessageIdAndUserId(UUID messageId, UUID userId);

    /**
     * Which of these messages this user has hidden. Asked once per page rather
     * than once per message — the alternative is a query per bubble.
     */
    @Query("""
        select h.message.id from ChatMessageHidden h
        where h.user.id = :userId and h.message.id in :messageIds
        """)
    List<UUID> findHiddenMessageIds(
        @Param("userId") UUID userId,
        @Param("messageIds") Collection<UUID> messageIds);

    boolean existsByMessageIdAndUserId(UUID messageId, UUID userId);
}
