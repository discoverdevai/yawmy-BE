package com.discoverdev.nms.child.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record HealthEntryRequest(
    @NotBlank @Size(max = 500) String text,
    @PastOrPresent LocalDate recordedOn
) {}
