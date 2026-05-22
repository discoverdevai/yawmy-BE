package com.discoverdev.nms.media;

import java.util.List;
import java.util.UUID;

public record MediaUploadedEvent(
    UUID nurseryId,
    UUID childId,
    UUID roomId,
    List<UUID> mediaItemIds,
    UUID uploaderUserId
) {}
