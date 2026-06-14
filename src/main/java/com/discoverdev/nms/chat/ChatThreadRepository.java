package com.discoverdev.nms.chat;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatThreadRepository extends JpaRepository<ChatThread, UUID> {

    Optional<ChatThread> findByNurseryIdAndDirectKey(UUID nurseryId, String directKey);

    Optional<ChatThread> findByNurseryIdAndRoomIdAndType(
        UUID nurseryId, UUID roomId, ChatThreadType type);

    Optional<ChatThread> findByNurseryIdAndType(UUID nurseryId, ChatThreadType type);
}
