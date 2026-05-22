package com.discoverdev.nms.attendance;

import com.discoverdev.nms.nursery.Nursery;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;

public final class AttendanceDates {

    private AttendanceDates() {}

    public static ZoneId zoneOf(Nursery nursery) {
        String tz = nursery.getTimezone();
        try {
            return tz != null ? ZoneId.of(tz) : ZoneId.of("Africa/Cairo");
        } catch (Exception e) {
            return ZoneId.of("Africa/Cairo");
        }
    }

    public static Instant startOfDay(Nursery nursery, LocalDate date) {
        return date.atStartOfDay(zoneOf(nursery)).toInstant();
    }

    public static Instant startOfNextDay(Nursery nursery, LocalDate date) {
        return date.plusDays(1).atStartOfDay(zoneOf(nursery)).toInstant();
    }

    public static Instant endOfRangeInclusive(Nursery nursery, LocalDate dateInclusive) {
        return dateInclusive.plusDays(1).atStartOfDay(zoneOf(nursery)).toInstant();
    }

    public static LocalDate localDateOf(Nursery nursery, Instant instant) {
        return instant.atZone(zoneOf(nursery)).toLocalDate();
    }

    public static LocalTime localTimeOf(Nursery nursery, Instant instant) {
        return instant.atZone(zoneOf(nursery)).toLocalTime();
    }
}
