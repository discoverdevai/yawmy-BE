package com.discoverdev.nms.dailylog.dto;

import com.discoverdev.nms.dailylog.LogEntryCategory;
import com.discoverdev.nms.dailylog.MealAmount;
import com.discoverdev.nms.dailylog.MoodOption;
import com.discoverdev.nms.dailylog.NapQuality;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * One entry in a child's day.
 *
 * Which fields are required depends on {@code category}, so they cannot be
 * annotated here — TeacherDailyLogService#normalise checks the combination and
 * rejects it with 400 if the category's answer is missing. It also derives
 * {@code title} for the categories that have no name of their own, and
 * {@code entryTime} when the form does not ask for one.
 *
 *   MOOD        mood
 *   MEAL        title (the meal) + mealAmount
 *   NAP         napStartTime + napEndTime + napQuality
 *   MEDICATION  title (the medication) + entryTime
 *   ACTIVITY    title (the activity)
 *   DIAPER      nothing — the entry IS the record that a change happened, and
 *               entryTime is when the teacher marked it
 */
public record CreateLogEntryRequest(
    @NotNull @PastOrPresent LocalDate date,
    @NotNull LogEntryCategory category,
    LocalTime entryTime,
    @Size(max = 200) String title,
    @Size(max = 2000) String detail,
    MoodOption mood,
    MealAmount mealAmount,
    LocalTime napStartTime,
    LocalTime napEndTime,
    NapQuality napQuality,
    Boolean publish
) {}
