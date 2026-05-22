package com.discoverdev.nms.dailylog.dto;

import com.discoverdev.nms.dailylog.LogEntryCategory;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalTime;

public record CreateLogEntryRequest(
    @NotNull @PastOrPresent LocalDate date,
    @NotNull LogEntryCategory category,
    @NotNull LocalTime entryTime,
    @NotBlank @Size(max = 200) String title,
    @Size(max = 2000) String detail,
    @Min(0) Integer napMinutes,
    Boolean publish
) {}
