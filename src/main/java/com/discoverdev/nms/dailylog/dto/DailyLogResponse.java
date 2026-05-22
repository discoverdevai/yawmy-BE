package com.discoverdev.nms.dailylog.dto;

import com.discoverdev.nms.dailylog.DailyLog;
import com.discoverdev.nms.dailylog.LogEntry;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record DailyLogResponse(
    UUID id,
    UUID childId,
    String childName,
    LocalDate logDate,
    int totalEntries,
    int publishedEntries,
    List<LogEntryResponse> entries,
    Instant createdAt,
    Instant updatedAt
) {
    public static DailyLogResponse of(DailyLog log, List<LogEntry> entries) {
        long published = entries.stream().filter(LogEntry::isPublished).count();
        return new DailyLogResponse(
            log.getId(),
            log.getChild().getId(),
            log.getChild().getFullName(),
            log.getLogDate(),
            entries.size(),
            (int) published,
            entries.stream().map(LogEntryResponse::of).toList(),
            log.getCreatedAt(),
            log.getUpdatedAt()
        );
    }
}
