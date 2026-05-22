package com.discoverdev.nms.incident;

import java.time.Instant;
import java.util.UUID;

public record IncidentAcknowledgedEvent(
    UUID nurseryId,
    UUID childId,
    UUID incidentId,
    UUID guardianUserId,
    Instant acknowledgedAt
) {}
