package com.discoverdev.nms.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record VerifyResetOtpRequest(
    @NotBlank String token,
    @NotBlank String otp
) {}
