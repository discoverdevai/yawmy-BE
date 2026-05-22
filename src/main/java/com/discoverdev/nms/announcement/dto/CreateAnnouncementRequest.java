package com.discoverdev.nms.announcement.dto;

import com.discoverdev.nms.announcement.AnnouncementScope;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

public record CreateAnnouncementRequest(
    @NotNull AnnouncementScope scope,
    UUID roomId,
    @NotBlank @Size(max = 200) String title,
    @NotBlank @Size(max = 4000) String body,
    List<@Size(max = 500) String> links,
    List<UUID> attachmentMediaIds
) {}
