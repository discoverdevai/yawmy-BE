package com.discoverdev.nms.superadmin.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Super-admin invites a nursery admin in one call. The backend creates a bare
 * nursery (id only) and a disabled ADMIN user linked to it, then issues an invite.
 * The admin sets the nursery name/logo/location/hours/geofence after accepting.
 */
public record InviteAdminRequest(
    @NotBlank @Email @Size(max = 254) String email,
    @NotBlank @Size(max = 200) String fullName
) {}