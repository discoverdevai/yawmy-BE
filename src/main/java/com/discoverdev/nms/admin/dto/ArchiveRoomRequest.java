package com.discoverdev.nms.admin.dto;

import java.util.List;
import java.util.UUID;

/**
 * Archive a room that still has children. Every enrolled child in the room must
 * be listed with the destination room to move them to — the whole operation
 * (reassign all + archive) runs atomically in one transaction.
 */
public record ArchiveRoomRequest(List<Reassignment> reassignments) {

    public record Reassignment(UUID childId, UUID roomId) {}
}
