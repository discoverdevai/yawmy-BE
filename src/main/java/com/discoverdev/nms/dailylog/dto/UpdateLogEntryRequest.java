package com.discoverdev.nms.dailylog.dto;

import com.discoverdev.nms.dailylog.LogEntryCategory;
import com.discoverdev.nms.dailylog.MealAmount;
import com.discoverdev.nms.dailylog.MoodOption;
import com.discoverdev.nms.dailylog.NapQuality;
import jakarta.validation.constraints.Size;
import java.time.LocalTime;

/** Partial edit of one entry — every field is optional, and a null leaves it alone. */
public record UpdateLogEntryRequest(
    LogEntryCategory category,
    LocalTime entryTime,
    @Size(min = 1, max = 200) String title,
    @Size(max = 2000) String detail,
    MoodOption mood,
    MealAmount mealAmount,
    LocalTime napStartTime,
    LocalTime napEndTime,
    NapQuality napQuality
) {}
