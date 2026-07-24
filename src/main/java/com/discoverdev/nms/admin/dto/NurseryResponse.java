package com.discoverdev.nms.admin.dto;

import com.discoverdev.nms.nursery.Nursery;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.util.Set;
import java.util.UUID;

public record NurseryResponse(
    UUID id,
    String name,
    String logoUrl,
    String timezone,
    LocalTime hoursStart,
    LocalTime hoursEnd,
    Set<DayOfWeek> workingDays,
    String locationText,
    Double geofenceLat,
    Double geofenceLng,
    Integer geofenceRadiusM,
    Instant joinedAt
) {
    public static NurseryResponse of(Nursery n, Instant joinedAt) {
        return new NurseryResponse(
            n.getId(),
            n.getName(),
            n.getLogoUrl(),
            n.getTimezone(),
            n.getHoursStart(),
            n.getHoursEnd(),
            n.getWorkingDays(),
            n.getLocationText(),
            n.getGeofenceLat(),
            n.getGeofenceLng(),
            n.getGeofenceRadiusM(),
            joinedAt
        );
    }
}