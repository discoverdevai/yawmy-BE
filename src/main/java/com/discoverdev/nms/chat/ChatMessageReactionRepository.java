package com.discoverdev.nms.chat;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatMessageReactionRepository extends JpaRepository<ChatMessageReaction, UUID> {

    Optional<ChatMessageReaction> findByMessageIdAndUserIdAndEmoji(
        UUID messageId, UUID userId, String emoji);

    List<ChatMessageReaction> findByMessageId(UUID messageId);

    List<ChatMessageReaction> findByMessageIdIn(java.util.Collection<UUID> messageIds);
}
