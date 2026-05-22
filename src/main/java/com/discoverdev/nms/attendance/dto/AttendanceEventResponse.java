package com.discoverdev.nms.attendance.dto;

import com.discoverdev.nms.attendance.AttendanceEvent;
import com.discoverdev.nms.attendance.AttendanceEventType;
import com.discoverdev.nms.attendance.AttendanceSource;
import java.time.Instant;
import java.util.UUID;

public record AttendanceEventResponse(
    UUID id,
    UUID childId,
    UUID roomId,
    AttendanceEventType type,
    AttendanceSource source,
    Instant recordedAt,
    UUID recordedByUserId,
    String recordedByName,
    Double lat,
    Double lng,
    Double accuracyM,
    String notes
) {
    public static AttendanceEventResponse of(AttendanceEvent e) {
        return new AttendanceEventResponse(
            e.getId(),
            e.getChild().getId(),
            e.getRoom() != null ? e.getRoom().getId() : null,
            e.getType(),
            e.getSource(),
            e.getRecordedAt(),
            e.getRecordedBy().getId(),
            e.getRecordedBy().getFullName(),
            e.getLat(),
            e.getLng(),
            e.getAccuracyM(),
            e.getNotes()
        );
    }
}
