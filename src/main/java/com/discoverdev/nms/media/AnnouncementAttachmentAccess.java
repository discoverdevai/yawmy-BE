package com.discoverdev.nms.media;

import com.discoverdev.nms.user.User;
import java.util.UUID;

/**
 * Answers whether a media item is readable because it is attached to an
 * announcement the requester can already read.
 *
 * Announcement attachments are nursery-scoped — no child, no room — so the
 * child/room rules in {@code MediaFileService} have nothing to match and would
 * deny every teacher and parent. The announcement's own scope is the authority
 * instead, and it lives in the announcement package.
 *
 * Declared here, implemented there, so media does not have to depend on
 * announcement. Same shape as {@link ChatAttachmentAccess}.
 */
public interface AnnouncementAttachmentAccess {

    /** True when this media hangs off an announcement within the user's reach. */
    boolean isAttachedToAnnouncementVisibleTo(UUID mediaId, User requester);
}
