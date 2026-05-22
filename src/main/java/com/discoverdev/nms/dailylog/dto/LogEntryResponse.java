package com.discoverdev.nms.dailylog.dto;

import com.discoverdev.nms.dailylog.LogEntry;
import com.discoverdev.nms.dailylog.LogEntryCategory;
import java.time.Instant;
import java.time.LocalTime;
import java.util.UUID;

public record LogEntryResponse(
    UUID id,
    UUID dailyLogId,
    LogEntryCategory category,
    LocalTime entryTime,
    String title,
    String detail,
    Integer napMinutes,
    UUID authorId,
    String authorName,
    Instant publishedAt,
    UUID publishedByUserId,
    Instant createdAt,
    Instant updatedAt
) {
    public static LogEntryResponse of(LogEntry e) {
        return new LogEntryResponse(
            e.getId(),
            e.getDailyLog().getId(),
            e.getCategory(),
            e.getEntryTime(),
            e.getTitle(),
            e.getDetail(),
            e.getNapMinutes(),
            e.getAuthor().getId(),
            e.getAuthor().getFullName(),
            e.getPublishedAt(),
            e.getPublishedBy() != null ? e.getPublishedBy().getId() : null,
            e.getCreatedAt(),
            e.getUpdatedAt()
        );
    }
}
