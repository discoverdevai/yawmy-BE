package com.discoverdev.nms.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record ResendResetOtpRequest(
    @NotBlank String token
) {}
