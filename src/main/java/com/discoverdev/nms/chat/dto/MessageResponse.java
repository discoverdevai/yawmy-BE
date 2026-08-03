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
    /**
     * Everything an attachment bubble needs before the bytes arrive: a document
     * row shows its name and size, a voice note sizes its progress bar from the
     * duration.
     */
    public record AttachmentRef(
        UUID id,
        String contentType,
        String fileUrl,
        String type,
        String filename,
        long sizeBytes,
        Long durationMs
    ) {}

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
        String replyPreview = m.getReplyTo() != null ? previewOf(m.getReplyTo(), 80) : null;
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
            mi.getType().name(),
            mi.getOriginalFilename(),
            mi.getSizeBytes(),
            mi.getDurationMs()
        );
    }

    /**
     * One line standing in for a message — in a reply quote, or as the chat
     * list's last-message preview. A voice note has no text, and quoting it as an
     * empty box tells the reader nothing, so attachment-only messages describe
     * themselves instead.
     */
    public static String previewOf(ChatMessage m, int max) {
        if (m == null) return null;
        if (m.getDeletedAt() != null) return "Message deleted";
        String body = m.getBody();
        if (body != null && !body.isBlank()) return truncate(body, max);
        List<MediaItem> attachments = m.getAttachments();
        if (attachments == null || attachments.isEmpty()) return null;
        int count = attachments.size();
        String label = switch (attachments.get(0).getType()) {
            case AUDIO -> "Voice message";
            case PHOTO -> count > 1 ? count + " photos" : "Photo";
            case VIDEO -> count > 1 ? count + " videos" : "Video";
            case DOCUMENT -> {
                String name = attachments.get(0).getOriginalFilename();
                yield name != null && !name.isBlank() ? name : "Document";
            }
        };
        return truncate(label, max);
    }

    private static String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max) + "…";
    }
}
