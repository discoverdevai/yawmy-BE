package com.discoverdev.nms.child.dto;

import com.discoverdev.nms.child.GuardianLink;
import java.time.Instant;
import java.util.UUID;

public record GuardianLinkResponse(
    UUID linkId,
    UUID childId,
    UUID guardianId,
    String guardianName,
    String guardianEmail,
    String guardianPhone,
    boolean guardianEnabled,
    String relationship,
    boolean primary,
    boolean newInvite,
    UUID inviteId,
    String inviteToken,
    Instant inviteExpiresAt,
    Instant createdAt
) {
    public static GuardianLinkResponse linked(GuardianLink link) {
        return new GuardianLinkResponse(
            link.getId(),
            link.getChild().getId(),
            link.getGuardian().getId(),
            link.getGuardian().getFullName(),
            link.getGuardian().getEmail(),
            link.getGuardian().getPhone(),
            link.getGuardian().isEnabled(),
            link.getRelationship(),
            link.isPrimary(),
            false,
            null,
            null,
            null,
            link.getCreatedAt()
        );
    }

    public static GuardianLinkResponse invited(
        GuardianLink link, UUID inviteId, String inviteToken, Instant expiresAt
    ) {
        return new GuardianLinkResponse(
            link.getId(),
            link.getChild().getId(),
            link.getGuardian().getId(),
            link.getGuardian().getFullName(),
            link.getGuardian().getEmail(),
            link.getGuardian().getPhone(),
            link.getGuardian().isEnabled(),
            link.getRelationship(),
            link.isPrimary(),
            true,
            inviteId,
            inviteToken,
            expiresAt,
            link.getCreatedAt()
        );
    }
}
