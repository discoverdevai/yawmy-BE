package com.discoverdev.nms.admin.dto;

import com.discoverdev.nms.user.User;
import java.util.UUID;

/** A teacher option for the "assign teacher" dropdown. */
public record TeacherResponse(UUID id, String fullName, String email) {
    public static TeacherResponse of(User u) {
        return new TeacherResponse(u.getId(), u.getFullName(), u.getEmail());
    }
}
