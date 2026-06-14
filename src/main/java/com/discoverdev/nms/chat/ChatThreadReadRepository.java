package com.discoverdev.nms.chat;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatThreadReadRepository extends JpaRepository<ChatThreadRead, UUID> {

    Optional<ChatThreadRead> findByThreadIdAndUserId(UUID threadId, UUID userId);
}
