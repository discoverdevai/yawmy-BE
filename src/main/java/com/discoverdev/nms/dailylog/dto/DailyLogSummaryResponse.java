package com.discoverdev.nms.dailylog.dto;

import com.discoverdev.nms.dailylog.DailyLog;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record DailyLogSummaryResponse(
    UUID id,
    UUID childId,
    String childName,
    LocalDate logDate,
    long totalEntries,
    long publishedEntries,
    Instant updatedAt
) {
    public static DailyLogSummaryResponse of(DailyLog log, long total, long published) {
        return new DailyLogSummaryResponse(
            log.getId(),
            log.getChild().getId(),
            log.getChild().getFullName(),
            log.getLogDate(),
            total,
            published,
            log.getUpdatedAt()
        );
    }
}
