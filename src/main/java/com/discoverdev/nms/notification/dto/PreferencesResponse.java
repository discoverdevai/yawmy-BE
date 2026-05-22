package com.discoverdev.nms.notification.dto;

import com.discoverdev.nms.notification.NotificationPreference;
import java.time.LocalTime;

public record PreferencesResponse(
    LocalTime quietHoursStart,
    LocalTime quietHoursEnd,
    boolean announcementsEnabled
) {
    public static PreferencesResponse of(NotificationPreference p) {
        return new PreferencesResponse(
            p.getQuietHoursStart(),
            p.getQuietHoursEnd(),
            p.isAnnouncementsEnabled()
        );
    }

    public static PreferencesResponse defaults() {
        return new PreferencesResponse(null, null, true);
    }
}
