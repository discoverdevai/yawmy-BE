package com.discoverdev.nms.auth.dto;

import com.discoverdev.nms.user.Role;
import java.time.Instant;

/**
 * Read-only view of an invite for the "open link -> valid vs expired screen" flow.
 * Does NOT consume the invite. {@code status} is one of VALID, EXPIRED, USED, INVALID.
 * For INVALID (unknown token) the identity fields are null.
 *
 * {@code nurseryName} lets the welcome screen greet the invitee with their nursery
 * ("You're welcome at ...") without needing a second lookup; it is null when the
 * invited user has no nursery attached (or for INVALID tokens).
 */
public record InviteValidationResponse(
    String status,
    String email,
    String fullName,
    Role role,
    String nurseryName,
    Instant expiresAt
) {
    public static InviteValidationResponse invalid() {
        return new InviteValidationResponse("INVALID", null, null, null, null, null);
    }
}