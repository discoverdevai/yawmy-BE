package com.discoverdev.nms.event.dto;

import com.discoverdev.nms.event.EventType;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalTime;

public record UpdateEventRequest(
    EventType eventType,
    @Size(min = 1, max = 200) String title,
    @Size(max = 2000) String description,
    LocalDate eventDate,
    LocalTime eventTime
) {}
