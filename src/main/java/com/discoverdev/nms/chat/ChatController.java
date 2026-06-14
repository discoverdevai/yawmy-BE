package com.discoverdev.nms.chat;

import com.discoverdev.nms.auth.AppUserDetails;
import com.discoverdev.nms.chat.dto.MarkReadRequest;
import com.discoverdev.nms.chat.dto.MessageResponse;
import com.discoverdev.nms.chat.dto.MuteRequest;
import com.discoverdev.nms.chat.dto.ReactionRequest;
import com.discoverdev.nms.chat.dto.SendMessageRequest;
import com.discoverdev.nms.chat.dto.ThreadDetailResponse;
import com.discoverdev.nms.chat.dto.ThreadSummaryResponse;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
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

@RestController
@RequestMapping("/api/me/chat")
public class ChatController {

    private final ChatService service;

    public ChatController(ChatService service) {
        this.service = service;
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

    @GetMapping("/threads/{id}/messages")
    public List<MessageResponse> history(
        @PathVariable UUID id,
        @RequestParam(value = "before", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant before,
        @RequestParam(value = "limit", required = false) Integer limit,
        @AuthenticationPrincipal AppUserDetails principal
    ) {
        return service.history(id, before, limit, principal.getUser());
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
