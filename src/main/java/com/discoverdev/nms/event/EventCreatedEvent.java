package com.discoverdev.nms.event;

import com.discoverdev.nms.announcement.AnnouncementScope;
import java.time.Instant;
import java.util.UUID;

public record EventCreatedEvent(
    UUID nurseryId,
    UUID eventId,
    AnnouncementScope scope,
    UUID roomId,
    Instant startsAt,
    UUID authorUserId
) {}
