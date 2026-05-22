package com.discoverdev.nms.attendance;

import java.time.Instant;
import java.util.UUID;

public record AttendanceEventRecordedEvent(
    UUID nurseryId,
    UUID childId,
    UUID eventId,
    AttendanceEventType type,
    AttendanceSource source,
    Instant recordedAt,
    UUID recordedByUserId
) {}
