package com.discoverdev.nms.event.dto;

import com.discoverdev.nms.announcement.AnnouncementScope;
import com.discoverdev.nms.event.EventType;
import com.discoverdev.nms.event.NurseryEvent;
import java.time.Instant;
import java.util.UUID;

public record EventResponse(
    UUID id,
    AnnouncementScope scope,
    UUID roomId,
    String roomName,
    EventType eventType,
    String title,
    String description,
    String location,
    Instant startsAt,
    Instant endsAt,
    String recurrenceRule,
    UUID authorId,
    String authorName,
    boolean savedByMe,
    Instant savedByMeAt,
    Instant createdAt,
    Instant updatedAt
) {
    public static EventResponse of(NurseryEvent e, boolean savedByMe, Instant savedAt) {
        return new EventResponse(
            e.getId(),
            e.getScope(),
            e.getRoom() != null ? e.getRoom().getId() : null,
            e.getRoom() != null ? e.getRoom().getName() : null,
            e.getEventType(),
            e.getTitle(),
            e.getDescription(),
            e.getLocation(),
            e.getStartsAt(),
            e.getEndsAt(),
            e.getRecurrenceRule(),
            e.getAuthor().getId(),
            e.getAuthor().getFullName(),
            savedByMe,
            savedAt,
            e.getCreatedAt(),
            e.getUpdatedAt()
        );
    }
}
