package com.discoverdev.nms.chat;

import com.discoverdev.nms.chat.dto.MessageResponse;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
public class ChatMessageBroadcaster {

    private static final Logger log = LoggerFactory.getLogger(ChatMessageBroadcaster.class);

    private final SimpMessagingTemplate template;

    public ChatMessageBroadcaster(SimpMessagingTemplate template) {
        this.template = template;
    }

    public void broadcast(UUID threadId, MessageResponse payload) {
        try {
            template.convertAndSend("/topic/threads/" + threadId, payload);
        } catch (RuntimeException e) {
            log.warn("WebSocket broadcast failed for thread {}: {}", threadId, e.getMessage());
        }
    }
}
