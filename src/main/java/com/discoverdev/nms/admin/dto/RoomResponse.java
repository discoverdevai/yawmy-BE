package com.discoverdev.nms.admin.dto;

import com.discoverdev.nms.nursery.Room;
import java.time.Instant;
import java.util.UUID;

public record RoomResponse(
    UUID id,
    String name,
    Integer capacity,
    boolean archived,
    Instant archivedAt,
    Instant createdAt
) {
    public static RoomResponse of(Room r) {
        return new RoomResponse(
            r.getId(),
            r.getName(),
            r.getCapacity(),
            r.getArchivedAt() != null,
            r.getArchivedAt(),
            r.getCreatedAt()
        );
    }
}
