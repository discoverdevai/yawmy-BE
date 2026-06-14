package com.discoverdev.nms.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

public record SendMessageRequest(
    @NotBlank @Size(max = 4000) String body,
    UUID replyToMessageId,
    List<UUID> attachmentMediaIds
) {}
