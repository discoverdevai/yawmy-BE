package com.discoverdev.nms.chat;

import java.util.List;
import java.util.UUID;

public record ChatMessageSentEvent(
    UUID nurseryId,
    UUID threadId,
    UUID messageId,
    UUID senderUserId,
    ChatThreadType threadType,
    List<UUID> recipientUserIds
) {}
