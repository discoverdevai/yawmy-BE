package com.discoverdev.nms.chat;

import com.discoverdev.nms.chat.dto.MessageResponse;
import java.util.Collection;
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

    /**
     * To everyone with the conversation open.
     *
     * A failed broadcast must never fail the send — the message is already
     * committed, and the apps fall back to polling when the socket is down.
     */
    public void broadcast(UUID threadId, MessageResponse payload) {
        try {
            template.convertAndSend("/topic/threads/" + threadId, payload);
        } catch (RuntimeException e) {
            log.warn("WebSocket broadcast failed for thread {}: {}", threadId, e.getMessage());
        }
    }

    /**
     * To each recipient's own queue, so the chat list can raise a thread to the
     * top and bump its badge without that conversation being open.
     *
     * Topics are per-thread, so a list screen would otherwise need one
     * subscription per row — and would still miss any thread that has no row yet
     * because nobody has messaged it. One queue per user avoids both.
     *
     * Addressed by the principal's name, which {@code WebSocketConfig} sets to
     * the user's email.
     */
    public void notifyInboxes(Collection<String> recipientEmails, MessageResponse payload) {
        for (String email : recipientEmails) {
            try {
                template.convertAndSendToUser(email, "/queue/chat", payload);
            } catch (RuntimeException e) {
                log.warn("WebSocket inbox notify failed for {}: {}", email, e.getMessage());
            }
        }
    }
}
