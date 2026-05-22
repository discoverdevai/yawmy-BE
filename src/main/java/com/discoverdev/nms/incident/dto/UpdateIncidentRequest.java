package com.discoverdev.nms.incident.dto;

import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public record UpdateIncidentRequest(
    @PastOrPresent Instant incidentAt,
    @Size(min = 1, max = 2000) String description,
    @Size(max = 2000) String actionTaken,
    @Size(max = 100) String injuryArea
) {}
