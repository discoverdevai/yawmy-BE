package com.discoverdev.nms.admin.dto;

import com.discoverdev.nms.user.Locale;
import com.discoverdev.nms.user.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateInviteRequest(
    @NotBlank @Email @Size(max = 254) String email,
    @NotBlank @Size(max = 200) String fullName,
    @NotNull Role role,
    Locale locale,
    @Size(max = 32) String phone
) {}
