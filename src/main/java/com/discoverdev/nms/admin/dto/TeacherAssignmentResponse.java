package com.discoverdev.nms.admin.dto;

import com.discoverdev.nms.nursery.TeacherRoomAssignment;
import java.time.Instant;
import java.util.UUID;

public record TeacherAssignmentResponse(
    UUID assignmentId,
    UUID teacherId,
    String teacherName,
    String teacherEmail,
    boolean teacherEnabled,
    UUID roomId,
    String roomName,
    Instant createdAt
) {
    public static TeacherAssignmentResponse of(TeacherRoomAssignment a) {
        return new TeacherAssignmentResponse(
            a.getId(),
            a.getTeacher().getId(),
            a.getTeacher().getFullName(),
            a.getTeacher().getEmail(),
            a.getTeacher().isEnabled(),
            a.getRoom().getId(),
            a.getRoom().getName(),
            a.getCreatedAt()
        );
    }
}
