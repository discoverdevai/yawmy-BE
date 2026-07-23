package com.discoverdev.nms.schedule.dto;

import com.discoverdev.nms.schedule.RoomSession;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record SessionResponse(
    UUID id,
    UUID roomId,
    String name,
    LocalDate date,
    LocalTime startTime,
    LocalTime endTime,
    UUID teacherId,
    String teacherName
) {
    public static SessionResponse of(RoomSession s) {
        return new SessionResponse(
            s.getId(),
            s.getRoom().getId(),
            s.getName(),
            s.getSessionDate(),
            s.getStartTime(),
            s.getEndTime(),
            s.getTeacher().getId(),
            s.getTeacher().getFullName()
        );
    }
}
