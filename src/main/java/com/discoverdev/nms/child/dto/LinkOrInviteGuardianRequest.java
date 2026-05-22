package com.discoverdev.nms.child.dto;

import com.discoverdev.nms.user.Locale;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LinkOrInviteGuardianRequest(
    @NotBlank @Email @Size(max = 254) String email,
    @Size(max = 200) String fullName,
    @NotBlank @Size(max = 50) String relationship,
    Boolean isPrimary,
    Locale locale,
    @Size(max = 32) String phone
) {}
