package com.discoverdev.nms.event;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventSaveRepository extends JpaRepository<EventSave, UUID> {

    Optional<EventSave> findByEventIdAndUserId(UUID eventId, UUID userId);

    boolean existsByEventIdAndUserId(UUID eventId, UUID userId);

    List<EventSave> findByUserIdOrderBySavedAtDesc(UUID userId);

    void deleteByEventIdAndUserId(UUID eventId, UUID userId);
}
