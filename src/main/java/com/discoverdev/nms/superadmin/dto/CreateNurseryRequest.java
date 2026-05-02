package com.discoverdev.nms.superadmin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateNurseryRequest(
    @NotBlank @Size(min = 1, max = 200) String name
) {}
