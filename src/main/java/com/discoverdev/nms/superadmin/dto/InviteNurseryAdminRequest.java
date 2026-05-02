package com.discoverdev.nms.superadmin.dto;

import com.discoverdev.nms.user.Locale;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record InviteNurseryAdminRequest(
    @NotBlank @Email @Size(max = 254) String email,
    @NotBlank @Size(max = 200) String fullName,
    Locale locale,
    @Size(max = 32) String phone
) {}
