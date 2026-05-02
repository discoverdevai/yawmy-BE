package com.discoverdev.nms.admin.dto;

import com.discoverdev.nms.user.Locale;
import com.discoverdev.nms.user.Role;
import com.discoverdev.nms.user.User;
import java.util.UUID;

public record AdminUserResponse(
    UUID id,
    String email,
    String fullName,
    Role role,
    Locale locale,
    boolean enabled
) {
    public static AdminUserResponse of(User user) {
        return new AdminUserResponse(
            user.getId(),
            user.getEmail(),
            user.getFullName(),
            user.getRole(),
            user.getLocale(),
            user.isEnabled()
        );
    }
}