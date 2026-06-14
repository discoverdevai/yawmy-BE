package com.discoverdev.nms.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ReactionRequest(@NotBlank @Size(max = 16) String emoji) {}
