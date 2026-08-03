package com.discoverdev.nms.chat;

import com.discoverdev.nms.media.ChatAttachmentAccess;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ChatAttachmentAccessImpl implements ChatAttachmentAccess {

    private final ChatMessageRepository messageRepository;

    public ChatAttachmentAccessImpl(ChatMessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isAttachedToThreadVisibleTo(UUID mediaId, UUID userId) {
        return messageRepository.existsVisibleAttachment(mediaId, userId);
    }
}
