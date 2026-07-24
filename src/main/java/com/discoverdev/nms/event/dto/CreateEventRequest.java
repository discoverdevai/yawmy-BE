package com.discoverdev.nms.event.dto;

import com.discoverdev.nms.event.EventType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalTime;

public record CreateEventRequest(
    @NotNull EventType eventType,
    @NotBlank @Size(max = 200) String title,
    @Size(max = 2000) String description,
    @NotNull LocalDate eventDate,
    @NotNull LocalTime eventTime
) {}
