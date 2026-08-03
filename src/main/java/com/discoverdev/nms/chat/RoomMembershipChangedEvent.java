package com.discoverdev.nms.chat;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Something changed that could alter who belongs in a room's group chat: a child
 * moved, enrolled or left; a guardian was linked or unlinked; a teacher was
 * assigned or unassigned.
 *
 * The event carries only which rooms to re-examine, never what changed. The
 * listener recomputes membership from the database either way, so a publisher
 * that names one room too many costs a query and nothing else — while one that
 * tried to describe the delta would have to get the delta right.
 */
public record RoomMembershipChangedEvent(Set<UUID> roomIds) {

    public static RoomMembershipChangedEvent of(UUID... roomIds) {
        Set<UUID> ids = new HashSet<>(Arrays.asList(roomIds));
        ids.remove(null);
        return new RoomMembershipChangedEvent(ids);
    }

    public boolean isEmpty() {
        return roomIds == null || roomIds.isEmpty();
    }
}
