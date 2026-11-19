package com.discoverdev.nms.push;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PushTokenRepository extends JpaRepository<PushToken, UUID> {

    Optional<PushToken> findByToken(String token);

    List<PushToken> findByUserId(UUID userId);

    List<PushToken> findByUserIdIn(Collection<UUID> userIds);
}
