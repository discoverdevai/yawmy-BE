package com.discoverdev.nms.admin.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * Home-screen summary for the admin dashboard.
 * {@code weeklyAttendance} covers the current week Saturday..Thursday (Friday is off),
 * each entry = number of distinct children checked in that day.
 */
public record DashboardResponse(
    String nurseryName,
    String logoUrl,
    long totalChildren,
    long totalTeachers,
    long checkedInToday,
    long incidentsToday,
    List<DayAttendance> weeklyAttendance
) {
    public record DayAttendance(String day, LocalDate date, long present) {}
}
