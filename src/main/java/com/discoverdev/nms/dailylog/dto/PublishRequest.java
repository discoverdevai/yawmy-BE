package com.discoverdev.nms.dailylog.dto;

import java.util.List;
import java.util.UUID;

public record PublishRequest(List<UUID> entryIds) {}
