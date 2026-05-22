package com.discoverdev.nms.incident.dto;

import com.discoverdev.nms.incident.IncidentReport;
import java.time.Instant;
import java.util.UUID;

public record IncidentResponse(
    UUID id,
    UUID childId,
    String childName,
    UUID roomId,
    String roomName,
    UUID authorId,
    String authorName,
    Instant incidentAt,
    String description,
    String actionTaken,
    String injuryArea,
    long totalGuardians,
    long acknowledgedCount,
    Instant createdAt,
    Instant updatedAt
) {
    public static IncidentResponse of(IncidentReport r, long totalGuardians, long acknowledgedCount) {
        return new IncidentResponse(
            r.getId(),
            r.getChild().getId(),
            r.getChild().getFullName(),
            r.getRoom() != null ? r.getRoom().getId() : null,
            r.getRoom() != null ? r.getRoom().getName() : null,
            r.getAuthor().getId(),
            r.getAuthor().getFullName(),
            r.getIncidentAt(),
            r.getDescription(),
            r.getActionTaken(),
            r.getInjuryArea(),
            totalGuardians,
            acknowledgedCount,
            r.getCreatedAt(),
            r.getUpdatedAt()
        );
    }
}
