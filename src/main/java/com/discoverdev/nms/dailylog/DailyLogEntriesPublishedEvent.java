package com.discoverdev.nms.dailylog;

import java.util.List;
import java.util.UUID;

public record DailyLogEntriesPublishedEvent(
    UUID nurseryId,
    UUID childId,
    UUID dailyLogId,
    List<UUID> entryIds,
    UUID publishedByUserId
) {}
