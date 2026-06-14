package com.discoverdev.nms.chat.dto;

import com.discoverdev.nms.chat.ChatMessage;
import com.discoverdev.nms.chat.ChatMessageReaction;
import com.discoverdev.nms.media.MediaItem;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record MessageResponse(
    UUID id,
    UUID threadId,
    UUID senderId,
    String senderName,
    String body,
    UUID replyToMessageId,
    String replyToPreview,
    List<AttachmentRef> attachments,
    Map<String, List<UUID>> reactions,
    boolean outOfHours,
    Instant createdAt,
    Instant deletedAt
) {
    public record AttachmentRef(UUID id, String contentType, String fileUrl, String type) {}

    public static MessageResponse of(ChatMessage m, List<ChatMessageReaction> reactions) {
        List<AttachmentRef> refs = m.getAttachments() == null ? List.of() :
            m.getAttachments().stream().map(MessageResponse::refFor).toList();
        Map<String, List<UUID>> byEmoji = new HashMap<>();
        if (reactions != null) {
            for (ChatMessageReaction r : reactions) {
                byEmoji.computeIfAbsent(r.getEmoji(), k -> new java.util.ArrayList<>())
                    .add(r.getUser().getId());
            }
        }
        String replyPreview = m.getReplyTo() != null
            ? truncate(m.getReplyTo().getBody(), 80)
            : null;
        return new MessageResponse(
            m.getId(),
            m.getThread().getId(),
            m.getSender().getId(),
            m.getSender().getFullName(),
            m.getDeletedAt() != null ? null : m.getBody(),
            m.getReplyTo() != null ? m.getReplyTo().getId() : null,
            replyPreview,
            refs,
            byEmoji,
            m.isOutOfHours(),
            m.getCreatedAt(),
            m.getDeletedAt()
        );
    }

    private static AttachmentRef refFor(MediaItem mi) {
        return new AttachmentRef(
            mi.getId(),
            mi.getContentType(),
            "/api/files/media/" + mi.getId(),
            mi.getType().name()
        );
    }

    private static String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max) + "…";
    }
}
