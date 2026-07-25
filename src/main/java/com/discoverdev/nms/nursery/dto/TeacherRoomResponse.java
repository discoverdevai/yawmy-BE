package com.discoverdev.nms.nursery.dto;

import com.discoverdev.nms.nursery.Room;
import java.util.UUID;

/**
 * One of the signed-in teacher's assigned rooms, as shown on Home.
 *
 * {@code childCount} counts only ENROLLED children — it is the room's roster
 * size, which the app also uses as the denominator for attendance.
 */
public record TeacherRoomResponse(
    UUID id,
    String name,
    String imageUrl,
    long childCount
) {
    public static TeacherRoomResponse of(Room room, long childCount) {
        return new TeacherRoomResponse(
            room.getId(),
            room.getName(),
            room.getImageUrl(),
            childCount
        );
    }
}
