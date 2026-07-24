package com.discoverdev.nms.incident.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public record CreateIncidentRequest(
    @NotNull @PastOrPresent Instant incidentAt,
    @NotBlank @Size(max = 100) String incidentType,
    @NotBlank @Size(max = 2000) String description,
    @Size(max = 2000) String actionTaken,
    @Size(max = 100) String injuryArea
) {}
