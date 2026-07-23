package com.discoverdev.nms.schedule.dto;

import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/** All fields optional — only the ones provided are changed. */
public record UpdateSessionRequest(
    @Size(min = 1, max = 100) String name,
    UUID teacherId,
    LocalDate date,
    LocalTime startTime,
    LocalTime endTime
) {}
