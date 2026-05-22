package com.discoverdev.nms.incident.dto;

import java.time.Instant;
import java.util.UUID;

public record AcknowledgmentRow(
    UUID guardianId,
    String guardianName,
    String guardianEmail,
    String relationship,
    boolean acknowledged,
    Instant acknowledgedAt
) {}
