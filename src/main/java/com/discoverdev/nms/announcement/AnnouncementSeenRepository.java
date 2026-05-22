package com.discoverdev.nms.announcement;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnnouncementSeenRepository extends JpaRepository<AnnouncementSeen, UUID> {

    Optional<AnnouncementSeen> findByAnnouncementIdAndUserId(UUID announcementId, UUID userId);

    boolean existsByAnnouncementIdAndUserId(UUID announcementId, UUID userId);

    long countByAnnouncementId(UUID announcementId);

    List<AnnouncementSeen> findByAnnouncementId(UUID announcementId);
}
