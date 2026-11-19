package com.discoverdev.nms.admin.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record UpdateRoomRequest(
    @Size(min = 1, max = 100) String name,
    @Min(1) Integer capacity,
    // Jira YC-86 (all optional). teacherIds / childIds, when given, are the room's full new lists.
    @Min(0) Integer ageMinMonths,
    @Min(0) Integer ageMaxMonths,
    java.util.List<java.util.UUID> teacherIds,
    java.util.List<java.util.UUID> childIds
) {}
