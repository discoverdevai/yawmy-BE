package com.discoverdev.nms.announcement.dto;

import com.discoverdev.nms.announcement.Announcement;
import com.discoverdev.nms.announcement.AnnouncementScope;
import com.discoverdev.nms.media.MediaItem;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AnnouncementResponse(
    UUID id,
    AnnouncementScope scope,
    UUID roomId,
    String roomName,
    UUID authorId,
    String authorName,
    String title,
    String body,
    List<String> links,
    List<AttachmentRef> attachments,
    long seenCount,
    boolean seenByMe,
    Instant seenByMeAt,
    Instant createdAt,
    Instant updatedAt
) {
    public record AttachmentRef(UUID id, String contentType, String fileUrl, long sizeBytes, String type) {}

    public static AnnouncementResponse of(
        Announcement a, long seenCount, boolean seenByMe, Instant seenByMeAt
    ) {
        List<AttachmentRef> refs = a.getAttachments() == null ? List.of() :
            a.getAttachments().stream()
                .map(AnnouncementResponse::refFor)
                .toList();
        return new AnnouncementResponse(
            a.getId(),
            a.getScope(),
            a.getRoom() != null ? a.getRoom().getId() : null,
            a.getRoom() != null ? a.getRoom().getName() : null,
            a.getAuthor().getId(),
            a.getAuthor().getFullName(),
            a.getTitle(),
            a.getBody(),
            a.getLinks() != null ? a.getLinks() : List.of(),
            refs,
            seenCount,
            seenByMe,
            seenByMeAt,
            a.getCreatedAt(),
            a.getUpdatedAt()
        );
    }

    private static AttachmentRef refFor(MediaItem m) {
        return new AttachmentRef(
            m.getId(),
            m.getContentType(),
            "/api/files/media/" + m.getId(),
            m.getSizeBytes(),
            m.getType().name()
        );
    }
}
