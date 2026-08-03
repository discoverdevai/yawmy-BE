package com.discoverdev.nms.media.dto;

import com.discoverdev.nms.media.MediaItem;
import com.discoverdev.nms.media.MediaType;
import java.time.Instant;
import java.util.UUID;

public record MediaItemResponse(
    UUID id,
    String scope,
    UUID childId,
    String childName,
    UUID roomId,
    String roomName,
    MediaType type,
    String contentType,
    long sizeBytes,
    String caption,
    String originalFilename,
    Long durationMs,
    String fileUrl,
    UUID uploaderId,
    String uploaderName,
    Instant uploadedAt
) {
    public static MediaItemResponse of(MediaItem m) {
        boolean isChildScope = m.getChild() != null;
        return new MediaItemResponse(
            m.getId(),
            isChildScope ? "CHILD" : "ROOM",
            isChildScope ? m.getChild().getId() : null,
            isChildScope ? m.getChild().getFullName() : null,
            !isChildScope && m.getRoom() != null ? m.getRoom().getId() : null,
            !isChildScope && m.getRoom() != null ? m.getRoom().getName() : null,
            m.getType(),
            m.getContentType(),
            m.getSizeBytes(),
            m.getCaption(),
            m.getOriginalFilename(),
            m.getDurationMs(),
            "/api/files/media/" + m.getId(),
            m.getUploader().getId(),
            m.getUploader().getFullName(),
            m.getUploadedAt()
        );
    }
}
