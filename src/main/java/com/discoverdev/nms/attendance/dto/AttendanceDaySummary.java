package com.discoverdev.nms.attendance.dto;

import com.discoverdev.nms.attendance.AttendanceStatus;
import java.time.Instant;
import java.time.LocalDate;

public record AttendanceDaySummary(
    LocalDate date,
    AttendanceStatus status,
    Instant lastCheckInAt,
    Instant lastCheckOutAt
) {}
