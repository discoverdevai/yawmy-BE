package com.discoverdev.nms.announcement.dto;

import com.discoverdev.nms.announcement.AnnouncementScope;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public record UpdateAnnouncementRequest(
    @Size(min = 1, max = 200) String title,
    @Size(min = 1, max = 4000) String body,
    LocalDate announcementDate,
    LocalTime announcementTime,
    AnnouncementScope scope,
    UUID roomId,
    List<@Size(max = 500) String> links,
    List<UUID> attachmentMediaIds
) {}
