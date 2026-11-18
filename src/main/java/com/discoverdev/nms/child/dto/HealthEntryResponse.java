package com.discoverdev.nms.child.dto;

import com.discoverdev.nms.child.ChildHealthEntry;
import com.discoverdev.nms.child.HealthEntryType;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record HealthEntryResponse(UUID id, HealthEntryType type, String text, LocalDate recordedOn, Instant createdAt) {

    public static HealthEntryResponse of(ChildHealthEntry e) {
        return new HealthEntryResponse(e.getId(), e.getType(), e.getText(), e.getRecordedOn(), e.getCreatedAt());
    }
}
