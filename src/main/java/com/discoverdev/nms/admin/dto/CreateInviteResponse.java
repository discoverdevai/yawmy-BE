package com.discoverdev.nms.admin.dto;

import com.discoverdev.nms.user.Role;
import java.time.Instant;
import java.util.UUID;

public record CreateInviteResponse(
    UUID inviteId,
    UUID userId,
    String email,
    Role role,
    String token,
    Instant expiresAt
) {}
