package com.discoverdev.nms.admin.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * A parent/guardian for the enrollment screen.
 * {@code relationship} is their role (e.g. Father / Mother) taken from their
 * guardian link. {@code status} is ACTIVE / PENDING / INVITATION_EXPIRED.
 */
public record ParentResponse(
    UUID id,
    String fullName,
    String email,
    String relationship,
    String imageUrl,
    String status,
    Instant joinedAt
) {}
