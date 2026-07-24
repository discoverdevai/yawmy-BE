package com.discoverdev.nms.admin.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Full parent/guardian detail with their linked children and each child's room. */
public record ParentDetailResponse(
    UUID id,
    String fullName,
    String email,
    String relationship,
    String imageUrl,
    String status,
    Instant joinedAt,
    List<LinkedChild> children
) {
    public record LinkedChild(UUID id, String fullName, String roomName) {}
}
