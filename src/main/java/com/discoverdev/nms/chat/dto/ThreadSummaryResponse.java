package com.discoverdev.nms.chat.dto;

import com.discoverdev.nms.chat.ChatThread;
import com.discoverdev.nms.chat.ChatThreadType;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ThreadSummaryResponse(
    UUID id,
    ChatThreadType type,
    UUID roomId,
    String roomName,
    List<ParticipantRef> participants,
    String lastMessagePreview,
    Instant lastMessageAt,
    long unreadCount,
    boolean muted
) {
    public record ParticipantRef(UUID userId, String name, String role) {}

    public static ThreadSummaryResponse of(
        ChatThread t,
        List<ParticipantRef> participants,
        String lastPreview,
        long unread,
        boolean muted
    ) {
        return new ThreadSummaryResponse(
            t.getId(),
            t.getType(),
            t.getRoom() != null ? t.getRoom().getId() : null,
            t.getRoom() != null ? t.getRoom().getName() : null,
            participants,
            lastPreview,
            t.getLastMessageAt(),
            unread,
            muted
        );
    }
}
