package com.discoverdev.nms.superadmin.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Reactivate a pending admin by their email — the value the root admin receives
 * in the reactivation-request notification, so no user id lookup is needed.
 */
public record ResendInviteRequest(
    @NotBlank @Email @Size(max = 254) String email
) {}
