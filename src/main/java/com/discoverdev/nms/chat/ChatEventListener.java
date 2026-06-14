package com.discoverdev.nms.chat;

import com.discoverdev.nms.notification.NotificationFanout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class ChatEventListener {

    private static final Logger log = LoggerFactory.getLogger(ChatEventListener.class);

    private final NotificationFanout fanout;

    public ChatEventListener(NotificationFanout fanout) {
        this.fanout = fanout;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onSent(ChatMessageSentEvent event) {
        log.debug("Chat message sent — thread={} message={} recipients={}",
            event.threadId(), event.messageId(), event.recipientUserIds().size());
        try {
            fanout.fanoutChatMessage(event);
        } catch (RuntimeException e) {
            log.error("Failed to fan out chat notifications: {}", e.getMessage(), e);
        }
    }
}
