package com.discoverdev.nms.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Sent from the "invitation expired" screen: the invitee asks the super-admin(s)
 * to re-invite them. {@code email} is required, {@code message} optional.
 */
public record ReactivationRequest(
    @NotBlank @Email @Size(max = 254) String email,
    @Size(max = 1000) String message
) {}