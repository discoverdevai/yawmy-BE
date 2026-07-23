package com.discoverdev.nms.admin.dto;

import com.discoverdev.nms.user.Role;
import java.util.UUID;

/**
 * A staff member (teacher) for the enrollment screen.
 * {@code status} is one of:
 *   ACTIVE             — accepted the invite (account enabled)
 *   PENDING            — invited, link still valid, not yet accepted
 *   INVITATION_EXPIRED — invited, link expired, not accepted
 */
public record StaffMemberResponse(
    UUID id,
    String fullName,
    String email,
    Role role,
    String staffRole,
    String jobTitle,
    String imageUrl,
    String status
) {}
