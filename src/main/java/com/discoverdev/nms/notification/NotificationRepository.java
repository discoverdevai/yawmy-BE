package com.discoverdev.nms.notification;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    Optional<Notification> findByIdAndRecipientId(UUID id, UUID recipientId);

    List<Notification> findByRecipientIdAndDeletedAtIsNullOrderByCreatedAtDesc(UUID recipientId);

    List<Notification> findByRecipientIdAndReadAtIsNullAndDeletedAtIsNullOrderByCreatedAtDesc(
        UUID recipientId);

    long countByRecipientIdAndReadAtIsNullAndDeletedAtIsNull(UUID recipientId);

    @Modifying
    @Query("""
        update Notification n
        set n.readAt = :now
        where n.recipient.id = :userId
          and n.readAt is null
          and n.deletedAt is null
        """)
    int markAllRead(@Param("userId") UUID userId, @Param("now") java.time.Instant now);

    @Modifying
    @Query("""
        update Notification n
        set n.deletedAt = :now
        where n.recipient.id = :userId
          and n.deletedAt is null
        """)
    int softDeleteAll(@Param("userId") UUID userId, @Param("now") java.time.Instant now);
}
