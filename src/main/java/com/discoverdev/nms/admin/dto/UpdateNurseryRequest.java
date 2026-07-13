package com.discoverdev.nms.admin.dto;

import jakarta.validation.constraints.Size;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Set;

public record UpdateNurseryRequest(
    @Size(min = 1, max = 200) String name,
    @Size(max = 500) String logoUrl,
    @Size(max = 64) String timezone,
    LocalTime hoursStart,
    LocalTime hoursEnd,
    Set<DayOfWeek> workingDays
) {}