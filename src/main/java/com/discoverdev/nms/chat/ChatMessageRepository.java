package com.discoverdev.nms.chat;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, UUID> {

    List<ChatMessage> findByThreadIdAndDeletedAtIsNullOrderByCreatedAtDesc(UUID threadId, Limit limit);

    List<ChatMessage> findByThreadIdAndCreatedAtLessThanAndDeletedAtIsNullOrderByCreatedAtDesc(
        UUID threadId, Instant before, Limit limit);

    Optional<ChatMessage> findTopByThreadIdAndDeletedAtIsNullOrderByCreatedAtDesc(UUID threadId);

    long countByThreadIdAndCreatedAtGreaterThanAndDeletedAtIsNull(UUID threadId, Instant after);

    long countByThreadIdAndDeletedAtIsNull(UUID threadId);
}
