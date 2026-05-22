package com.discoverdev.nms.child.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ContactRequest(
    @NotBlank @Size(max = 200) String fullName,
    @NotBlank @Size(max = 50) String relationship,
    @NotBlank @Size(max = 32) String phone
) {}
