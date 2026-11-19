package com.discoverdev.nms.push;

import com.discoverdev.nms.chat.ChatMessage;
import com.discoverdev.nms.chat.ChatMessageRepository;
import com.discoverdev.nms.chat.ChatMessageSentEvent;
import com.discoverdev.nms.chat.ChatParticipant;
import com.discoverdev.nms.chat.ChatParticipantRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * A new chat message becomes a push notification with sound for everyone in the conversation
 * except the sender, skipping people who muted the conversation or the sender (Jira YC-35, YC-60).
 */
@Component
public class ChatPushNotifier {

    private final ChatMessageRepository messages;
    private final ChatParticipantRepository participants;
    private final PushTokenRepository tokens;
    private final ExpoPushSender sender;

    public ChatPushNotifier(
        ChatMessageRepository messages,
        ChatParticipantRepository participants,
        PushTokenRepository tokens,
        ExpoPushSender sender
    ) {
        this.messages = messages;
        this.participants = participants;
        this.tokens = tokens;
        this.sender = sender;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public void onMessage(ChatMessageSentEvent event) {
        ChatMessage m = messages.findById(event.messageId()).orElse(null);
        if (m == null || m.getDeletedAt() != null) return;
        // The event's recipients already leave out the sender and people who muted the sender.
        Set<UUID> to = new HashSet<>(event.recipientUserIds() == null ? List.of() : event.recipientUserIds());
        to.remove(event.senderUserId());
        for (ChatParticipant p : participants.findByThreadIdAndLeftAtIsNull(event.threadId())) {
            if (p.isMuted()) to.remove(p.getUser().getId());   // conversation muted
        }
        if (to.isEmpty()) return;
        List<String> devices = tokens.findByUserIdIn(to).stream().map(PushToken::getToken).toList();
        if (devices.isEmpty()) return;
        String title = m.getSender().getFullName();
        String body = m.getBody() == null || m.getBody().isBlank() ? "Sent an attachment"
            : (m.getBody().length() > 140 ? m.getBody().substring(0, 140) + "…" : m.getBody());
        Map<String, Object> data = Map.of("type", "CHAT_MESSAGE", "threadId", event.threadId().toString());
        CompletableFuture.runAsync(() -> sender.send(devices, title, body, data));   // never slows the sender down
    }
}
