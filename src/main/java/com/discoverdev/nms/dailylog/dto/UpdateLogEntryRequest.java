package com.discoverdev.nms.dailylog.dto;

import com.discoverdev.nms.dailylog.LogEntryCategory;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.time.LocalTime;

public record UpdateLogEntryRequest(
    LogEntryCategory category,
    LocalTime entryTime,
    @Size(min = 1, max = 200) String title,
    @Size(max = 2000) String detail,
    @Min(0) Integer napMinutes
) {}
