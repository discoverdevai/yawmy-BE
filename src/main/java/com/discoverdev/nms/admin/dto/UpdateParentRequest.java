package com.discoverdev.nms.admin.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

/**
 * Update a parent's fields. All optional — only provided ones change.
 * {@code relationship} is applied to all of the parent's guardian links.
 */
public record UpdateParentRequest(
    @Size(min = 1, max = 200) String fullName,
    @Email @Size(max = 255) String email,
    @Size(max = 50) String relationship
) {}
