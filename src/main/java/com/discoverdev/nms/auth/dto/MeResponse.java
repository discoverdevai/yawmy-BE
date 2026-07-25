package com.discoverdev.nms.auth.dto;

import com.discoverdev.nms.user.Locale;
import com.discoverdev.nms.user.Role;
import com.discoverdev.nms.user.User;
import java.util.UUID;

/**
 * The signed-in user's own profile.
 *
 * {@code role} is the system role (TEACHER / ADMIN / …) used for access
 * decisions; {@code jobTitle} is the human label shown in the UI
 * ("Lead Teacher") and is null for non-staff.
 */
public record MeResponse(
    UUID id,
    String email,
    String fullName,
    Role role,
    Locale locale,
    String imageUrl,
    String staffRole,
    String jobTitle
) {
    public static MeResponse of(User user) {
        return new MeResponse(
            user.getId(),
            user.getEmail(),
            user.getFullName(),
            user.getRole(),
            user.getLocale(),
            user.getImageUrl(),
            user.getStaffRole() != null ? user.getStaffRole().name() : null,
            user.getStaffRole() != null ? user.getStaffRole().getLabel() : null
        );
    }
}
