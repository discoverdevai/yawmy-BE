package com.discoverdev.nms.dailylog.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * One child's daily-log state for a date, as the teacher app's Logs list shows it.
 *
 * Unlike the room feed, this includes children with no log at all — they are
 * exactly the NOT_STARTED rows the teacher still has to write.
 *
 * {@code status}:
 *   NOT_STARTED — no log, or a log with no entries. {@code logId} is null.
 *   DRAFT       — entries saved, none published yet. Parent sees nothing.
 *   COMPLETED   — at least one entry published, so the parent can see the day.
 */
public record ChildLogStatusResponse(
    UUID childId,
    String childName,
    String photoUrl,
    /** The app shows the child's age beside their name, and derives it from this. */
    LocalDate dob,
    UUID logId,
    String status,
    long totalEntries,
    long publishedEntries,
    Instant updatedAt
) {
    public static final String NOT_STARTED = "NOT_STARTED";
    public static final String DRAFT = "DRAFT";
    public static final String COMPLETED = "COMPLETED";
}
