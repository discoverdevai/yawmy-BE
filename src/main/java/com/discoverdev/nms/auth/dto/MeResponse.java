package com.discoverdev.nms.auth.dto;

import com.discoverdev.nms.user.Locale;
import com.discoverdev.nms.user.Role;
import com.discoverdev.nms.user.User;
import java.util.UUID;

public record MeResponse(
    UUID id,
    String email,
    String fullName,
    Role role,
    Locale locale
) {
    public static MeResponse of(User user) {
        return new MeResponse(
            user.getId(),
            user.getEmail(),
            user.getFullName(),
            user.getRole(),
            user.getLocale()
        );
    }
}
