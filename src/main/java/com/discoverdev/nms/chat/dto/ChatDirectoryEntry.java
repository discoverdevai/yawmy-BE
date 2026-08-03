package com.discoverdev.nms.chat.dto;

import com.discoverdev.nms.chat.ChatThreadType;
import java.time.Instant;
import java.util.UUID;

/**
 * One row of a user's chat list.
 *
 * The list is derived from who the caller is actually related to right now —
 * their rooms, their children's rooms, the nursery's admins — not from the
 * threads that happen to exist. A contact they have never messaged appears with
 * `threadId` null and no counters; the thread is created the moment they send
 * something. That is also why a reassignment needs no cleanup: the teacher of a
 * room the child left simply stops being related, so the row stops being derived.
 */
public record ChatDirectoryEntry(
    ChatThreadType kind,
    /** Set for DIRECT rows: the person on the other side. */
    UUID peerUserId,
    /** Set for DIRECT rows: PARENT / TEACHER / ADMIN. */
    String peerRole,
    /** Set for ROOM_GROUP rows. */
    UUID roomId,
    String title,
    /** Secondary line: a staff job title, "Parent of …", or the member count. */
    String subtitle,
    String avatarUrl,
    /** Null until the first message — everything below it is null/0 too. */
    UUID threadId,
    String lastMessagePreview,
    Instant lastMessageAt,
    long unreadCount,
    boolean muted
) {
    /** A contact with no conversation yet. */
    public static ChatDirectoryEntry pending(
        ChatThreadType kind, UUID peerUserId, String peerRole, UUID roomId,
        String title, String subtitle, String avatarUrl
    ) {
        return new ChatDirectoryEntry(
            kind, peerUserId, peerRole, roomId, title, subtitle, avatarUrl,
            null, null, null, 0L, false
        );
    }

    public ChatDirectoryEntry withThread(
        UUID threadId, String preview, Instant lastMessageAt, long unread, boolean muted
    ) {
        return new ChatDirectoryEntry(
            kind, peerUserId, peerRole, roomId, title, subtitle, avatarUrl,
            threadId, preview, lastMessageAt, unread, muted
        );
    }
}
