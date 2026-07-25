package com.discoverdev.nms.dailylog.dto;

import com.discoverdev.nms.dailylog.LogEntryCategory;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalTime;
import java.util.List;

/**
 * A child's whole daily log, as the teacher app's log form submits it.
 *
 * The form owns the complete state of the day, so this REPLACES the log's
 * entries rather than merging — which also makes the call idempotent, so the
 * app's offline outbox can retry a failed save without duplicating anything.
 *
 * {@code publish} false = "Save as Draft", true = "Publish". Note that a log
 * which already has published entries is never retracted by this endpoint —
 * see TeacherDailyLogService#upsertLog.
 */
public record UpsertDailyLogRequest(
    Boolean publish,
    @NotNull @Valid List<Entry> entries
) {
    public record Entry(
        @NotNull LogEntryCategory category,
        @NotNull LocalTime entryTime,
        @NotBlank @Size(max = 200) String title,
        @Size(max = 2000) String detail,
        @Min(0) Integer napMinutes
    ) {}
}
