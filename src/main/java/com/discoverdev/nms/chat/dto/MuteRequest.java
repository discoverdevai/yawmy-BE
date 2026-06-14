package com.discoverdev.nms.chat.dto;

import jakarta.validation.constraints.NotNull;

public record MuteRequest(@NotNull Boolean muted) {}
