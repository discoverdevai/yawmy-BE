package com.discoverdev.nms.announcement.dto;

import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

public record UpdateAnnouncementRequest(
    @Size(min = 1, max = 200) String title,
    @Size(min = 1, max = 4000) String body,
    List<@Size(max = 500) String> links,
    List<UUID> attachmentMediaIds
) {}
