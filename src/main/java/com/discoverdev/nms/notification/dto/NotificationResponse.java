package com.discoverdev.nms.notification.dto;

import com.discoverdev.nms.notification.Notification;
import com.discoverdev.nms.notification.NotificationType;
import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(
    UUID id,
    NotificationType type,
    String title,
    String body,
    String entityType,
    UUID entityId,
    UUID childId,
    boolean read,
    Instant readAt,
    Instant createdAt
) {
    public static NotificationResponse of(Notification n) {
        return new NotificationResponse(
            n.getId(),
            n.getType(),
            n.getTitle(),
            n.getBody(),
            n.getEntityType(),
            n.getEntityId(),
            n.getChildId(),
            n.isRead(),
            n.getReadAt(),
            n.getCreatedAt()
        );
    }
}
