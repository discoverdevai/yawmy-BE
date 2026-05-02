package com.discoverdev.nms.admin.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record UpdateRoomRequest(
    @Size(min = 1, max = 100) String name,
    @Min(1) Integer capacity
) {}
