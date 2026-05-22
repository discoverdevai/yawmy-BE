package com.discoverdev.nms.child.dto;

import com.discoverdev.nms.child.EmergencyContact;
import java.time.Instant;
import java.util.UUID;

public record EmergencyContactResponse(
    UUID id,
    UUID childId,
    String fullName,
    String relationship,
    String phone,
    Instant createdAt,
    Instant updatedAt
) {
    public static EmergencyContactResponse of(EmergencyContact c) {
        return new EmergencyContactResponse(
            c.getId(),
            c.getChild().getId(),
            c.getFullName(),
            c.getRelationship(),
            c.getPhone(),
            c.getCreatedAt(),
            c.getUpdatedAt()
        );
    }
}
