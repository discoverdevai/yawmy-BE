package com.discoverdev.nms.admin.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Result of a parent enrollment. Use each {@code children[].childId} to upload
 * that child's image via POST /api/admin/children/{childId}/photo.
 */
public record EnrollFamilyResponse(
    UUID parentId,
    String parentEmail,
    String inviteToken,
    Instant inviteExpiresAt,
    List<EnrolledChild> children
) {
    public record EnrolledChild(UUID childId, String fullName) {}
}
