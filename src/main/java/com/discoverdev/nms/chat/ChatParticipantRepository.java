package com.discoverdev.nms.chat;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ChatParticipantRepository extends JpaRepository<ChatParticipant, UUID> {

    Optional<ChatParticipant> findByThreadIdAndUserId(UUID threadId, UUID userId);

    List<ChatParticipant> findByThreadId(UUID threadId);

    List<ChatParticipant> findByThreadIdAndLeftAtIsNull(UUID threadId);

    boolean existsByThreadIdAndUserIdAndLeftAtIsNull(UUID threadId, UUID userId);

    @Query("""
        select p from ChatParticipant p
        where p.user.id = :userId
          and p.thread.nursery.id = :nurseryId
          and p.leftAt is null
        """)
    List<ChatParticipant> findActiveByUserAndNursery(UUID userId, UUID nurseryId);
}
