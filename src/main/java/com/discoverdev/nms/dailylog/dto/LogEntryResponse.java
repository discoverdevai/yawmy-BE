package com.discoverdev.nms.dailylog.dto;

import com.discoverdev.nms.dailylog.LogEntry;
import com.discoverdev.nms.dailylog.LogEntryCategory;
import com.discoverdev.nms.dailylog.MealAmount;
import com.discoverdev.nms.dailylog.MoodOption;
import com.discoverdev.nms.dailylog.NapQuality;
import java.time.Instant;
import java.time.LocalTime;
import java.util.UUID;

/**
 * One entry as both apps read it.
 *
 * The structured answers (mood, mealAmount, nap*) are the source of truth; the
 * *Label fields are the same values already worded for display, so the parent
 * app does not have to keep its own copy of the wording in sync. Only the
 * fields belonging to the entry's category are populated.
 */
public record LogEntryResponse(
    UUID id,
    UUID dailyLogId,
    LogEntryCategory category,
    LocalTime entryTime,
    String title,
    String detail,
    MoodOption mood,
    String moodLabel,
    MealAmount mealAmount,
    String mealAmountLabel,
    LocalTime napStartTime,
    LocalTime napEndTime,
    NapQuality napQuality,
    String napQualityLabel,
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
            e.getMood(),
            e.getMood() != null ? e.getMood().getLabel() : null,
            e.getMealAmount(),
            e.getMealAmount() != null ? e.getMealAmount().getLabel() : null,
            e.getNapStartTime(),
            e.getNapEndTime(),
            e.getNapQuality(),
            e.getNapQuality() != null ? e.getNapQuality().getLabel() : null,
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
