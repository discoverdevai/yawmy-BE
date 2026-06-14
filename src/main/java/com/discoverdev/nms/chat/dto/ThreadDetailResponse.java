package com.discoverdev.nms.chat.dto;

import com.discoverdev.nms.chat.ChatThread;
import com.discoverdev.nms.chat.ChatThreadType;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ThreadDetailResponse(
    UUID id,
    ChatThreadType type,
    UUID roomId,
    String roomName,
    List<ParticipantRow> participants,
    boolean muted,
    UUID lastReadMessageId,
    Instant createdAt
) {
    public record ParticipantRow(
        UUID userId, String name, String email, String role,
        Instant joinedAt, Instant leftAt, boolean muted
    ) {}

    public static ThreadDetailResponse of(
        ChatThread t, List<ParticipantRow> participants,
        boolean callerMuted, UUID callerLastRead
    ) {
        return new ThreadDetailResponse(
            t.getId(),
            t.getType(),
            t.getRoom() != null ? t.getRoom().getId() : null,
            t.getRoom() != null ? t.getRoom().getName() : null,
            participants,
            callerMuted,
            callerLastRead,
            t.getCreatedAt()
        );
    }
}
