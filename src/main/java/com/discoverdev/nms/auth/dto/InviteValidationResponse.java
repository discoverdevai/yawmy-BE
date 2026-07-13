package com.discoverdev.nms.auth.dto;

import com.discoverdev.nms.user.Role;
import java.time.Instant;

/**
 * Read-only view of an invite for the "open link -> valid vs expired screen" flow.
 * Does NOT consume the invite. {@code status} is one of VALID, EXPIRED, USED, INVALID.
 * For INVALID (unknown token) the identity fields are null.
 */
public record InviteValidationResponse(
    String status,
    String email,
    String fullName,
    Role role,
    Instant expiresAt
) {
    public static InviteValidationResponse invalid() {
        return new InviteValidationResponse("INVALID", null, null, null, null);
    }
}