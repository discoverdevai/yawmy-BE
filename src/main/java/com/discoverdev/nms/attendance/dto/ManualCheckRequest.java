package com.discoverdev.nms.attendance.dto;

import jakarta.validation.constraints.Size;

public record ManualCheckRequest(@Size(max = 500) String notes) {}
