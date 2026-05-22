package com.discoverdev.nms.child.dto;

import com.discoverdev.nms.child.Child;
import com.discoverdev.nms.child.EnrollmentStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ChildResponse(
    UUID id,
    String fullName,
    LocalDate dob,
    UUID roomId,
    String roomName,
    String photoUrl,
    String allergies,
    String medicalNotes,
    String immunizations,
    EnrollmentStatus enrollmentStatus,
    Instant unenrolledAt,
    Instant createdAt,
    Instant updatedAt
) {
    public static ChildResponse of(Child c) {
        return new ChildResponse(
            c.getId(),
            c.getFullName(),
            c.getDob(),
            c.getRoom() != null ? c.getRoom().getId() : null,
            c.getRoom() != null ? c.getRoom().getName() : null,
            c.getPhotoKey() != null ? "/api/files/children/" + c.getId() + "/photo" : null,
            c.getAllergies(),
            c.getMedicalNotes(),
            c.getImmunizations(),
            c.getEnrollmentStatus(),
            c.getUnenrolledAt(),
            c.getCreatedAt(),
            c.getUpdatedAt()
        );
    }
}
