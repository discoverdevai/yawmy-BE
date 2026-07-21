package com.discoverdev.nms.admin.dto;

import com.discoverdev.nms.nursery.Room;
import java.time.Instant;
import java.util.UUID;

public record RoomResponse(
    UUID id,
    String name,
    Integer capacity,
    String imageUrl,
    long childCount,
    boolean archived,
    Instant archivedAt,
    Instant createdAt
) {
    public static RoomResponse of(Room r, long childCount) {
        return new RoomResponse(
            r.getId(),
            r.getName(),
            r.getCapacity(),
            r.getImageUrl(),
            childCount,
            r.getArchivedAt() != null,
            r.getArchivedAt(),
            r.getCreatedAt()
        );
    }

    public static RoomResponse of(Room r) {
        return of(r, 0);
    }
}
