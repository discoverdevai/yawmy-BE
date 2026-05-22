package com.discoverdev.nms.event.dto;

import com.discoverdev.nms.announcement.AnnouncementScope;
import com.discoverdev.nms.event.EventType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;

public record CreateEventRequest(
    @NotNull AnnouncementScope scope,
    UUID roomId,
    @NotNull EventType eventType,
    @NotBlank @Size(max = 200) String title,
    @Size(max = 2000) String description,
    @Size(max = 200) String location,
    @NotNull Instant startsAt,
    @NotNull Instant endsAt,
    @Size(max = 500) String recurrenceRule
) {}
