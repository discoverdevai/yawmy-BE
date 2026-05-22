package com.discoverdev.nms.incident;

import java.time.Instant;
import java.util.UUID;

public record IncidentReportedEvent(
    UUID nurseryId,
    UUID childId,
    UUID incidentId,
    UUID authorUserId,
    Instant incidentAt
) {}
