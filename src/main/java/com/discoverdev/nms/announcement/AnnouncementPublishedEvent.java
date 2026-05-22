package com.discoverdev.nms.announcement;

import java.util.UUID;

public record AnnouncementPublishedEvent(
    UUID nurseryId,
    UUID announcementId,
    AnnouncementScope scope,
    UUID roomId,
    UUID authorUserId
) {}
