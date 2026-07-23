package com.discoverdev.nms.schedule.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record CreateSessionRequest(
    @NotBlank @Size(max = 100) String name,
    @NotNull UUID teacherId,
    @NotNull LocalDate date,
    @NotNull LocalTime startTime,
    @NotNull LocalTime endTime
) {}
