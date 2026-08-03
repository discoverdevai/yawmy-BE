package com.discoverdev.nms.chat;

import com.discoverdev.nms.attendance.AttendanceDates;
import com.discoverdev.nms.chat.dto.MarkReadRequest;
import com.discoverdev.nms.chat.dto.MessageResponse;
import com.discoverdev.nms.chat.dto.MuteRequest;
import com.discoverdev.nms.chat.dto.ReactionRequest;
import com.discoverdev.nms.chat.dto.SendMessageRequest;
import com.discoverdev.nms.chat.dto.ThreadDetailResponse;
import com.discoverdev.nms.chat.dto.ThreadSummaryResponse;
import com.discoverdev.nms.media.MediaItem;
import com.discoverdev.nms.media.MediaItemRepository;
import com.discoverdev.nms.nursery.Nursery;
import com.discoverdev.nms.user.Role;
import com.discoverdev.nms.user.User;
import com.discoverdev.nms.user.UserRepository;
import java.time.Instant;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Limit;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ChatService {

    private static final int DEFAULT_PAGE = 50;
    private static final int MAX_PAGE = 200;

    private final ChatThreadRepository threadRepository;
    private final ChatParticipantRepository participantRepository;
    private final ChatMessageRepository messageRepository;
    private final ChatMessageReactionRepository reactionRepository;
    private final ChatThreadReadRepository readRepository;
    private final MediaItemRepository mediaRepository;
    private final ChatMessageHiddenRepository hiddenRepository;
    private final UserMuteRepository muteRepository;
    private final UserRepository userRepository;
    private final ChatMessageBroadcaster broadcaster;
    private final ApplicationEventPublisher events;

    public ChatService(
        ChatThreadRepository threadRepository,
        ChatParticipantRepository participantRepository,
        ChatMessageRepository messageRepository,
        ChatMessageReactionRepository reactionRepository,
        ChatThreadReadRepository readRepository,
        MediaItemRepository mediaRepository,
        ChatMessageHiddenRepository hiddenRepository,
        UserMuteRepository muteRepository,
        UserRepository userRepository,
        ChatMessageBroadcaster broadcaster,
        ApplicationEventPublisher events
    ) {
        this.threadRepository = threadRepository;
        this.participantRepository = participantRepository;
        this.messageRepository = messageRepository;
        this.reactionRepository = reactionRepository;
        this.readRepository = readRepository;
        this.mediaRepository = mediaRepository;
        this.hiddenRepository = hiddenRepository;
        this.muteRepository = muteRepository;
        this.userRepository = userRepository;
        this.broadcaster = broadcaster;
        this.events = events;
    }

    // ---- delete for me -------------------------------------------------------

    /**
     * Hide a message from this reader only. Everyone else's copy is untouched,
     * and it is reversible — which is the difference between this and
     * {@link #delete}, where the sender or an admin removes it for everyone.
     */
    @Transactional
    public void hideForMe(UUID messageId, User user) {
        ChatMessage message = messageRepository.findById(messageId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Message not found"));
        requireActiveMembership(message.getThread().getId(), user);
        if (hiddenRepository.existsByMessageIdAndUserId(messageId, user.getId())) return;
        hiddenRepository.save(ChatMessageHidden.builder().message(message).user(user).build());
    }

    @Transactional
    public void unhideForMe(UUID messageId, User user) {
        hiddenRepository.findByMessageIdAndUserId(messageId, user.getId())
            .ifPresent(hiddenRepository::delete);
    }

    // ---- mute a person -------------------------------------------------------

    /**
     * Stop being notified about this person. Their messages still appear in the
     * thread — this silences the interruption, not the speaker.
     */
    @Transactional
    public void muteUser(UUID mutedUserId, User muter) {
        if (mutedUserId.equals(muter.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot mute yourself");
        }
        User muted = userRepository.findById(mutedUserId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        if (muter.getNursery() == null || muted.getNursery() == null
            || !muted.getNursery().getId().equals(muter.getNursery().getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }
        if (muteRepository.existsByMuterIdAndMutedId(muter.getId(), mutedUserId)) return;
        muteRepository.save(UserMute.builder().muter(muter).muted(muted).build());
    }

    @Transactional
    public void unmuteUser(UUID mutedUserId, User muter) {
        muteRepository.findByMuterIdAndMutedId(muter.getId(), mutedUserId)
            .ifPresent(muteRepository::delete);
    }

    /** Who this user has muted, so the app can show the toggle already on. */
    @Transactional(readOnly = true)
    public List<UUID> mutedUserIds(User user) {
        return muteRepository.findByMuterIdOrderByCreatedAtDesc(user.getId()).stream()
            .map(m -> m.getMuted().getId())
            .toList();
    }

    @Transactional(readOnly = true)
    public List<ThreadSummaryResponse> listThreadsForUser(User user) {
        List<ChatParticipant> active = participantRepository
            .findActiveByUserAndNursery(user.getId(), user.getNursery().getId());
        List<ThreadSummaryResponse> out = new ArrayList<>();
        for (ChatParticipant cp : active) {
            ChatThread t = cp.getThread();
            ChatMessage last = messageRepository
                .findTopByThreadIdAndDeletedAtIsNullOrderByCreatedAtDesc(t.getId())
                .orElse(null);
            ChatThreadRead r = readRepository.findByThreadIdAndUserId(t.getId(), user.getId()).orElse(null);
            Instant lastReadAt = r != null ? r.getLastReadAt() : Instant.EPOCH;
            long unread = messageRepository
                .countByThreadIdAndCreatedAtGreaterThanAndSenderIdNotAndDeletedAtIsNull(
                    t.getId(), lastReadAt, user.getId());
            String preview = MessageResponse.previewOf(last, 100);
            List<ThreadSummaryResponse.ParticipantRef> parts = participantRepository
                .findByThreadIdAndLeftAtIsNull(t.getId()).stream()
                .map(p -> new ThreadSummaryResponse.ParticipantRef(
                    p.getUser().getId(), p.getUser().getFullName(), p.getUser().getRole().name()))
                .toList();
            out.add(ThreadSummaryResponse.of(t, parts, preview, unread, cp.isMuted()));
        }
        out.sort((a, b) -> {
            if (a.lastMessageAt() == null && b.lastMessageAt() == null) return 0;
            if (a.lastMessageAt() == null) return 1;
            if (b.lastMessageAt() == null) return -1;
            return b.lastMessageAt().compareTo(a.lastMessageAt());
        });
        return out;
    }

    @Transactional(readOnly = true)
    public ThreadDetailResponse getThread(UUID threadId, User user) {
        ChatThread t = requireActiveMembership(threadId, user);
        ChatParticipant me = participantRepository.findByThreadIdAndUserId(threadId, user.getId()).orElseThrow();
        ChatThreadRead r = readRepository.findByThreadIdAndUserId(threadId, user.getId()).orElse(null);
        List<ThreadDetailResponse.ParticipantRow> rows = participantRepository.findByThreadId(threadId).stream()
            .map(p -> new ThreadDetailResponse.ParticipantRow(
                p.getUser().getId(), p.getUser().getFullName(), p.getUser().getEmail(),
                p.getUser().getRole().name(), p.getJoinedAt(), p.getLeftAt(), p.isMuted()))
            .toList();
        return ThreadDetailResponse.of(t, rows, me.isMuted(),
            r != null ? r.getLastReadMessageId() : null);
    }

    /**
     * A page of the conversation as this reader sees it.
     *
     * `before` pages backwards through history (newest first) as the user scrolls
     * up. `after` asks for what has arrived since a known point, oldest first —
     * that is the socket-is-down fallback, and it must not be answered with the
     * newest page or the client has to guess what it already has.
     *
     * Messages this reader hid are dropped here, so "delete for me" holds for
     * every later fetch without touching anyone else's view.
     */
    @Transactional(readOnly = true)
    public List<MessageResponse> history(
        UUID threadId, Instant before, Instant after, Integer limit, User user
    ) {
        requireActiveMembership(threadId, user);
        int cap = (limit == null || limit <= 0) ? DEFAULT_PAGE : Math.min(limit, MAX_PAGE);
        List<ChatMessage> messages;
        if (after != null) {
            messages = messageRepository
                .findByThreadIdAndCreatedAtGreaterThanAndDeletedAtIsNullOrderByCreatedAtAsc(
                    threadId, after, Limit.of(cap));
        } else if (before != null) {
            messages = messageRepository
                .findByThreadIdAndCreatedAtLessThanAndDeletedAtIsNullOrderByCreatedAtDesc(
                    threadId, before, Limit.of(cap));
        } else {
            messages = messageRepository
                .findByThreadIdAndDeletedAtIsNullOrderByCreatedAtDesc(threadId, Limit.of(cap));
        }
        if (messages.isEmpty()) return List.of();

        List<UUID> ids = messages.stream().map(ChatMessage::getId).toList();
        Set<UUID> hidden = new HashSet<>(hiddenRepository.findHiddenMessageIds(user.getId(), ids));
        Map<UUID, List<ChatMessageReaction>> reactionsByMessage = new HashMap<>();
        for (ChatMessageReaction r : reactionRepository.findByMessageIdIn(ids)) {
            reactionsByMessage.computeIfAbsent(r.getMessage().getId(), k -> new ArrayList<>()).add(r);
        }
        return messages.stream()
            .filter(m -> !hidden.contains(m.getId()))
            .map(m -> MessageResponse.of(m, reactionsByMessage.getOrDefault(m.getId(), List.of())))
            .toList();
    }

    @Transactional
    public MessageResponse send(UUID threadId, SendMessageRequest req, User sender) {
        if (!req.hasContent()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "A message needs text or at least one attachment");
        }
        ChatThread t = requireActiveMembership(threadId, sender);
        Nursery nursery = t.getNursery();
        ChatMessage replyTo = null;
        if (req.replyToMessageId() != null) {
            replyTo = messageRepository.findById(req.replyToMessageId())
                .filter(m -> m.getThread().getId().equals(threadId))
                .filter(m -> m.getDeletedAt() == null)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "replyToMessageId is not in this thread"));
        }
        List<MediaItem> attachments = new ArrayList<>();
        if (req.attachmentMediaIds() != null) {
            for (UUID mediaId : req.attachmentMediaIds()) {
                MediaItem m = mediaRepository
                    .findByIdAndNurseryIdAndDeletedAtIsNull(mediaId, nursery.getId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Attachment not found: " + mediaId));
                attachments.add(m);
            }
        }
        boolean ooh = isOutOfHours(sender, nursery, Instant.now());
        ChatMessage saved = messageRepository.save(ChatMessage.builder()
            .thread(t)
            .sender(sender)
            .body(req.body())
            .replyTo(replyTo)
            .attachments(attachments)
            .outOfHours(ooh)
            .build());
        t.setLastMessageAt(saved.getCreatedAt());
        threadRepository.save(t);

        List<ChatParticipant> others = participantRepository
            .findByThreadIdAndLeftAtIsNull(threadId).stream()
            .filter(p -> !p.getUser().getId().equals(sender.getId()))
            .toList();
        List<UUID> recipients = others.stream().map(p -> p.getUser().getId()).toList();
        events.publishEvent(new ChatMessageSentEvent(
            nursery.getId(), t.getId(), saved.getId(), sender.getId(), t.getType(), recipients
        ));

        MessageResponse resp = MessageResponse.of(saved, List.of());
        // Two deliveries, two purposes: the thread topic updates an open
        // conversation, the personal queues update everyone else's chat list.
        broadcaster.broadcast(threadId, resp);
        broadcaster.notifyInboxes(
            others.stream().map(p -> p.getUser().getEmail()).toList(), resp);
        return resp;
    }

    @Transactional
    public void delete(UUID messageId, User user) {
        ChatMessage m = messageRepository.findById(messageId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Message not found"));
        if (m.getDeletedAt() != null) return;
        requireActiveMembership(m.getThread().getId(), user);
        boolean isOwner = m.getSender().getId().equals(user.getId());
        boolean isAdminInNursery = user.getRole() == Role.ADMIN
            && user.getNursery() != null
            && user.getNursery().getId().equals(m.getThread().getNursery().getId());
        if (!isOwner && !isAdminInNursery) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                "Only the sender or an admin can delete this message");
        }
        m.setDeletedAt(Instant.now());
        m.setDeletedBy(user);
        messageRepository.save(m);
    }

    @Transactional
    public MessageResponse addReaction(UUID messageId, ReactionRequest req, User user) {
        ChatMessage m = messageRepository.findById(messageId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Message not found"));
        requireActiveMembership(m.getThread().getId(), user);
        if (m.getDeletedAt() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot react to a deleted message");
        }
        if (!reactionRepository
                .findByMessageIdAndUserIdAndEmoji(messageId, user.getId(), req.emoji()).isPresent()) {
            reactionRepository.save(ChatMessageReaction.builder()
                .message(m).user(user).emoji(req.emoji()).build());
        }
        return MessageResponse.of(m, reactionRepository.findByMessageId(messageId));
    }

    @Transactional
    public MessageResponse removeReaction(UUID messageId, String emoji, User user) {
        ChatMessage m = messageRepository.findById(messageId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Message not found"));
        requireActiveMembership(m.getThread().getId(), user);
        reactionRepository.findByMessageIdAndUserIdAndEmoji(messageId, user.getId(), emoji)
            .ifPresent(reactionRepository::delete);
        return MessageResponse.of(m, reactionRepository.findByMessageId(messageId));
    }

    @Transactional
    public void markRead(UUID threadId, MarkReadRequest req, User user) {
        requireActiveMembership(threadId, user);
        ChatMessage m = messageRepository.findById(req.lastReadMessageId())
            .filter(msg -> msg.getThread().getId().equals(threadId))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Message not in this thread"));
        ChatThreadRead r = readRepository.findByThreadIdAndUserId(threadId, user.getId())
            .orElseGet(() -> ChatThreadRead.builder()
                .thread(m.getThread()).user(user).build());
        r.setLastReadMessageId(m.getId());
        r.setLastReadAt(Instant.now());
        readRepository.save(r);
    }

    @Transactional
    public void setMute(UUID threadId, MuteRequest req, User user) {
        ChatParticipant p = participantRepository
            .findByThreadIdAndUserId(threadId, user.getId())
            .filter(ChatParticipant::isActive)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Thread not found"));
        p.setMuted(Boolean.TRUE.equals(req.muted()));
        participantRepository.save(p);
    }

    @Transactional
    public void setParticipantMuteAsAdmin(UUID participantId, MuteRequest req, User admin) {
        ChatParticipant p = participantRepository.findById(participantId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Participant not found"));
        if (admin.getRole() != Role.ADMIN
            || !p.getThread().getNursery().getId().equals(admin.getNursery().getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Participant not found");
        }
        p.setMuted(Boolean.TRUE.equals(req.muted()));
        participantRepository.save(p);
    }

    private ChatThread requireActiveMembership(UUID threadId, User user) {
        ChatThread t = threadRepository.findById(threadId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Thread not found"));
        boolean active = participantRepository
            .existsByThreadIdAndUserIdAndLeftAtIsNull(threadId, user.getId());
        if (!active) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Thread not found");
        }
        return t;
    }

    private boolean isOutOfHours(User sender, Nursery nursery, Instant when) {
        if (sender.getRole() != Role.PARENT) return false;
        if (nursery.getHoursStart() == null || nursery.getHoursEnd() == null) return false;
        LocalTime local = AttendanceDates.localTimeOf(nursery, when);
        LocalTime start = nursery.getHoursStart();
        LocalTime end = nursery.getHoursEnd();
        if (start.isBefore(end)) {
            return local.isBefore(start) || !local.isBefore(end);
        }
        return local.isBefore(start) && !local.isBefore(end);
    }


    @Transactional(readOnly = true)
    public ChatMessage requireMessageInThread(UUID messageId, UUID threadId) {
        return messageRepository.findById(messageId)
            .filter(m -> m.getThread().getId().equals(threadId))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Message not found"));
    }
}
