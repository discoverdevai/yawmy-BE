package com.discoverdev.nms.attendance.dto;

import com.discoverdev.nms.attendance.AttendanceEvent;
import com.discoverdev.nms.attendance.AttendanceEventType;
import com.discoverdev.nms.attendance.AttendanceStatus;
import com.discoverdev.nms.child.Child;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record ChildAttendanceResponse(
    UUID childId,
    String childName,
    String photoUrl,
    UUID roomId,
    String roomName,
    LocalDate date,
    AttendanceStatus status,
    Instant lastCheckInAt,
    Instant lastCheckOutAt,
    List<AttendanceEventResponse> events
) {
    public static ChildAttendanceResponse of(Child child, LocalDate date, List<AttendanceEvent> events) {
        Instant lastIn = null;
        Instant lastOut = null;
        AttendanceStatus status = AttendanceStatus.NOT_ARRIVED;
        for (AttendanceEvent e : events) {
            if (e.getType() == AttendanceEventType.CHECK_IN) {
                lastIn = e.getRecordedAt();
                status = AttendanceStatus.PRESENT;
            } else if (e.getType() == AttendanceEventType.CHECK_OUT) {
                lastOut = e.getRecordedAt();
                status = AttendanceStatus.CHECKED_OUT;
            }
        }
        return new ChildAttendanceResponse(
            child.getId(),
            child.getFullName(),
            child.getPhotoKey() != null ? "/api/files/children/" + child.getId() + "/photo" : null,
            child.getRoom() != null ? child.getRoom().getId() : null,
            child.getRoom() != null ? child.getRoom().getName() : null,
            date,
            status,
            lastIn,
            lastOut,
            events.stream().map(AttendanceEventResponse::of).toList()
        );
    }
}
