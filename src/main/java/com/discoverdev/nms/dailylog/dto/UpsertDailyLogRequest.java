package com.discoverdev.nms.dailylog.dto;

import com.discoverdev.nms.dailylog.LogEntryCategory;
import com.discoverdev.nms.dailylog.MealAmount;
import com.discoverdev.nms.dailylog.MoodOption;
import com.discoverdev.nms.dailylog.NapQuality;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

/**
 * A child's daily log, as the teacher app's log form submits it.
 *
 * {@code publish} false = "Save as Draft", true = "Publish". A log which
 * already has published entries is never retracted by this endpoint — see
 * TeacherDailyLogService#upsertLog.
 *
 * {@code append} decides what happens to what is already stored, and exists
 * because the form does not always hold the whole day:
 *
 *   false (default) — REPLACE. The form holds every entry, so the stored ones
 *                     are swapped for these. Idempotent, so a retried save
 *                     cannot duplicate anything. This is the case after the
 *                     teacher opens an existing draft to continue it.
 *   true            — APPEND. The teacher left an existing draft untouched and
 *                     wrote more logs beside it, so these are added to it. A
 *                     draft of 3 plus 4 new ones becomes 7.
 *
 * Appending is only honoured while the day is still a draft; publishing always
 * replaces, so publishing over a draft clears it and posts only what was sent.
 *
 * Per-entry requirements are the same as CreateLogEntryRequest, and are checked
 * the same way — by category, in the service.
 */
public record UpsertDailyLogRequest(
    Boolean publish,
    Boolean append,
    @NotNull @Valid List<Entry> entries
) {
    /**
     * {@code id} identifies an entry the client already has. Present → that
     * stored entry is updated in place, keeping its identity and any
     * publication it already carries. Absent → a genuinely new entry.
     *
     * This is what stops an update to a published day re-notifying parents
     * about entries the teacher never touched.
     */
    public record Entry(
        UUID id,
        @NotNull LogEntryCategory category,
        LocalTime entryTime,
        @Size(max = 200) String title,
        @Size(max = 2000) String detail,
        MoodOption mood,
        MealAmount mealAmount,
        LocalTime napStartTime,
        LocalTime napEndTime,
        NapQuality napQuality
    ) {}
}
