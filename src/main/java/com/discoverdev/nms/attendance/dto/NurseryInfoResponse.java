package com.discoverdev.nms.attendance.dto;

import com.discoverdev.nms.nursery.Nursery;
import java.time.LocalTime;
import java.util.UUID;

public record NurseryInfoResponse(
    UUID id,
    String name,
    String logoUrl,
    String timezone,
    LocalTime hoursStart,
    LocalTime hoursEnd,
    Double geofenceLat,
    Double geofenceLng,
    Integer geofenceRadiusM
) {
    public static NurseryInfoResponse of(Nursery n) {
        return new NurseryInfoResponse(
            n.getId(),
            n.getName(),
            n.getLogoUrl(),
            n.getTimezone(),
            n.getHoursStart(),
            n.getHoursEnd(),
            n.getGeofenceLat(),
            n.getGeofenceLng(),
            n.getGeofenceRadiusM()
        );
    }
}
