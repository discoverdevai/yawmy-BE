package com.discoverdev.nms.announcement.dto;

import java.time.Instant;
import java.util.UUID;

public record AnnouncementSeenRow(
    UUID userId,
    String userName,
    String userEmail,
    String role,
    Instant seenAt
) {}
