package com.discoverdev.nms.admin.dto;

import com.discoverdev.nms.user.Locale;
import com.discoverdev.nms.user.Role;
import com.discoverdev.nms.user.StaffRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * staffRole is the job title (Lead Teacher, …) for TEACHER invites — pick a value
 * from GET /api/admin/staff/roles. Ignored for non-teacher invites.
 */
public record CreateInviteRequest(
    @NotBlank @Email @Size(max = 254) String email,
    @NotBlank @Size(max = 200) String fullName,
    @NotNull Role role,
    StaffRole staffRole,
    Locale locale,
    @Size(max = 32) String phone
) {}
