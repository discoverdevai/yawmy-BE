package com.discoverdev.nms.incident.dto;

import com.discoverdev.nms.incident.IncidentReport;
import java.time.Instant;
import java.util.UUID;

public record ParentIncidentResponse(
    UUID id,
    UUID childId,
    String childName,
    Instant incidentAt,
    String description,
    String actionTaken,
    String injuryArea,
    String authorName,
    boolean acknowledged,
    Instant acknowledgedAt,
    Instant createdAt
) {
    public static ParentIncidentResponse of(IncidentReport r, Instant ackedAt) {
        return new ParentIncidentResponse(
            r.getId(),
            r.getChild().getId(),
            r.getChild().getFullName(),
            r.getIncidentAt(),
            r.getDescription(),
            r.getActionTaken(),
            r.getInjuryArea(),
            r.getAuthor().getFullName(),
            ackedAt != null,
            ackedAt,
            r.getCreatedAt()
        );
    }
}
