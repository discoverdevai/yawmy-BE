package com.discoverdev.nms.admin.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

/** Update a teacher's own fields. All optional — only provided ones change. */
public record UpdateStaffRequest(
    @Size(min = 1, max = 200) String fullName,
    @Email @Size(max = 255) String email,
    String staffRole
) {}
