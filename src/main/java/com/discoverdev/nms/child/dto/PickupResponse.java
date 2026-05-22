package com.discoverdev.nms.child.dto;

import com.discoverdev.nms.child.AuthorizedPickup;
import java.time.Instant;
import java.util.UUID;

public record PickupResponse(
    UUID id,
    UUID childId,
    String fullName,
    String relationship,
    String phone,
    Instant createdAt,
    Instant updatedAt
) {
    public static PickupResponse of(AuthorizedPickup p) {
        return new PickupResponse(
            p.getId(),
            p.getChild().getId(),
            p.getFullName(),
            p.getRelationship(),
            p.getPhone(),
            p.getCreatedAt(),
            p.getUpdatedAt()
        );
    }
}
