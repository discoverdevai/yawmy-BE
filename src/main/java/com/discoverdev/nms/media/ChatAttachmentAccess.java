package com.discoverdev.nms.media;

import java.util.UUID;

/**
 * Answers whether a media item is readable because it was attached to a chat
 * message the requester can see.
 *
 * Chat attachments are nursery-scoped — no child, no room — so the child/room
 * rules in {@code MediaFileService} have nothing to match on and would deny
 * every teacher and parent. The thread's participant list is the authority
 * instead, and it lives in the chat package.
 *
 * Declared here, implemented there, so media does not have to depend on chat.
 */
public interface ChatAttachmentAccess {

    /** True when the user is an active participant of a thread that carries this media. */
    boolean isAttachedToThreadVisibleTo(UUID mediaId, UUID userId);
}
