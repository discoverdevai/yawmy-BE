package com.discoverdev.nms.chat;

import com.discoverdev.nms.auth.AppUserDetails;
import com.discoverdev.nms.chat.dto.ChatDirectoryEntry;
import com.discoverdev.nms.chat.dto.MarkReadRequest;
import com.discoverdev.nms.chat.dto.MessageResponse;
import com.discoverdev.nms.chat.dto.MuteRequest;
import com.discoverdev.nms.chat.dto.ReactionRequest;
import com.discoverdev.nms.chat.dto.SendMessageRequest;
import com.discoverdev.nms.chat.dto.ThreadDetailResponse;
import com.discoverdev.nms.chat.dto.ThreadSummaryResponse;
import com.discoverdev.nms.media.dto.BulkUploadResponse;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/me/chat")
public class ChatController {

    private final ChatService service;
    private final ChatMediaService mediaService;
    private final ChatDirectoryService directoryService;

    public ChatController(
        ChatService service,
        ChatMediaService mediaService,
        ChatDirectoryService directoryService
    ) {
        this.service = service;
        this.mediaService = mediaService;
        this.directoryService = directoryService;
    }

    /**
     * Upload chat attachments (photo / video / document) or a voice note and get
     * back media ids to pass as `attachmentMediaIds` when sending the message.
     * `durationMs` is what the recorder measured; it applies to audio and video only.
     */
    @PostMapping(value = "/media", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public BulkUploadResponse uploadMedia(
        @RequestParam("files") List<MultipartFile> files,
        @RequestParam(value = "durationMs", required = false) Long durationMs,
        @AuthenticationPrincipal AppUserDetails principal
    ) {
        return mediaService.upload(principal.getUser(), files, durationMs);
    }

    /**
     * The caller's chat list, derived from who they are currently related to.
     *
     * Context is role-specific and must be re-fetched when it changes: a teacher
     * passes the room they have open, a parent passes the child they have
     * selected. Admins pass neither — they see the whole nursery.
     *
     * Rows with a null `threadId` are contacts nobody has messaged yet; opening
     * one goes through the role's `POST /threads/…` endpoint as usual.
     */
    @GetMapping("/directory")
    public List<ChatDirectoryEntry> directory(
        @RequestParam(value = "roomId", required = false) UUID roomId,
        @RequestParam(value = "childId", required = false) UUID childId,
        @AuthenticationPrincipal AppUserDetails principal
    ) {
        return directoryService.list(principal.getUser(), roomId, childId);
    }

    @GetMapping("/threads")
    public List<ThreadSummaryResponse> threads(@AuthenticationPrincipal AppUserDetails principal) {
        return service.listThreadsForUser(principal.getUser());
    }

    @GetMapping("/threads/{id}")
    public ThreadDetailResponse get(
        @PathVariable UUID id, @AuthenticationPrincipal AppUserDetails principal
    ) {
        return service.getThread(id, principal.getUser());
    }

    /**
     * `before` scrolls back through history (newest first). `after` fetches what
     * has arrived since a timestamp, oldest first — used when the socket is down
     * and the app is polling. Neither: the newest page.
     */
    @GetMapping("/threads/{id}/messages")
    public List<MessageResponse> history(
        @PathVariable UUID id,
        @RequestParam(value = "before", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant before,
        @RequestParam(value = "after", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant after,
        @RequestParam(value = "limit", required = false) Integer limit,
        @AuthenticationPrincipal AppUserDetails principal
    ) {
        return service.history(id, before, after, limit, principal.getUser());
    }

    /** Hide a message for me only. Everyone else's copy is untouched. */
    @PostMapping("/messages/{id}/hide")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void hideMessage(
        @PathVariable UUID id, @AuthenticationPrincipal AppUserDetails principal
    ) {
        service.hideForMe(id, principal.getUser());
    }

    @DeleteMapping("/messages/{id}/hide")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unhideMessage(
        @PathVariable UUID id, @AuthenticationPrincipal AppUserDetails principal
    ) {
        service.unhideForMe(id, principal.getUser());
    }

    /**
     * Stop being notified about this person. Their messages still appear in the
     * thread — this silences the notification, not the speaker.
     */
    @PostMapping("/mutes/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void muteUser(
        @PathVariable UUID userId, @AuthenticationPrincipal AppUserDetails principal
    ) {
        service.muteUser(userId, principal.getUser());
    }

    @DeleteMapping("/mutes/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unmuteUser(
        @PathVariable UUID userId, @AuthenticationPrincipal AppUserDetails principal
    ) {
        service.unmuteUser(userId, principal.getUser());
    }

    /** Everyone the caller has muted, so the app can render the toggle correctly. */
    @GetMapping("/mutes")
    public List<UUID> mutes(@AuthenticationPrincipal AppUserDetails principal) {
        return service.mutedUserIds(principal.getUser());
    }

    @PostMapping("/threads/{id}/messages")
    @ResponseStatus(HttpStatus.CREATED)
    public MessageResponse send(
        @PathVariable UUID id,
        @Valid @RequestBody SendMessageRequest req,
        @AuthenticationPrincipal AppUserDetails principal
    ) {
        return service.send(id, req, principal.getUser());
    }

    @PostMapping("/threads/{id}/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markRead(
        @PathVariable UUID id,
        @Valid @RequestBody MarkReadRequest req,
        @AuthenticationPrincipal AppUserDetails principal
    ) {
        service.markRead(id, req, principal.getUser());
    }

    @PatchMapping("/threads/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void setMute(
        @PathVariable UUID id,
        @Valid @RequestBody MuteRequest req,
        @AuthenticationPrincipal AppUserDetails principal
    ) {
        service.setMute(id, req, principal.getUser());
    }

    @PostMapping("/messages/{id}/reactions")
    public MessageResponse addReaction(
        @PathVariable UUID id,
        @Valid @RequestBody ReactionRequest req,
        @AuthenticationPrincipal AppUserDetails principal
    ) {
        return service.addReaction(id, req, principal.getUser());
    }

    @DeleteMapping("/messages/{id}/reactions")
    public MessageResponse removeReaction(
        @PathVariable UUID id,
        @RequestParam("emoji") String emoji,
        @AuthenticationPrincipal AppUserDetails principal
    ) {
        return service.removeReaction(id, emoji, principal.getUser());
    }

    @DeleteMapping("/messages/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMessage(
        @PathVariable UUID id, @AuthenticationPrincipal AppUserDetails principal
    ) {
        service.delete(id, principal.getUser());
    }
}
