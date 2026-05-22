package com.discoverdev.nms.notification.dto;

import java.time.LocalTime;

public record UpdatePreferencesRequest(
    LocalTime quietHoursStart,
    LocalTime quietHoursEnd,
    Boolean announcementsEnabled,
    Boolean clearQuietHours
) {}
