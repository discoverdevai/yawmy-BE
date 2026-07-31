package com.discoverdev.nms.nursery.dto;

import com.discoverdev.nms.child.Child;
import java.time.LocalDate;
import java.util.UUID;

/**
 * One enrolled child in a room, for the teacher app's pickers and rosters.
 *
 * Deliberately thin: the app needs to identify and show a child, not read their
 * record. Anything medical (allergies, notes, immunizations) stays out — those
 * belong to the child profile endpoints, behind their own access checks.
 *
 * {@code id} is the child's UUID, which is what every other teacher endpoint is
 * keyed by and what the app shows as the child's ID.
 */
public record RoomChildResponse(
    UUID id,
    String fullName,
    String photoUrl,
    LocalDate dob
) {
    public static RoomChildResponse of(Child child) {
        return new RoomChildResponse(
            child.getId(),
            child.getFullName(),
            // Served by ChildPhotoFileController; null when no photo was uploaded.
            child.getPhotoKey() != null
                ? "/api/files/children/" + child.getId() + "/photo"
                : null,
            child.getDob()
        );
    }
}
