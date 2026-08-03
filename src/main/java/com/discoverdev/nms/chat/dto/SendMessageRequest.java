package com.discoverdev.nms.chat.dto;

import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

/**
 * A voice note or a photo is a message in its own right, so `body` is optional.
 * A message with neither text nor attachment is nothing at all, though — which is
 * what {@code hasContent} rules out, since @NotBlank alone would also reject the
 * voice notes.
 */
public record SendMessageRequest(
    @Size(max = 4000) String body,
    UUID replyToMessageId,
    List<UUID> attachmentMediaIds
) {
    public boolean hasContent() {
        boolean hasText = body != null && !body.isBlank();
        boolean hasAttachment = attachmentMediaIds != null && !attachmentMediaIds.isEmpty();
        return hasText || hasAttachment;
    }
}
