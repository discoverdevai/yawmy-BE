package com.discoverdev.nms.chat;

import com.discoverdev.nms.auth.AppUserDetails;
import com.discoverdev.nms.chat.dto.MuteRequest;
import com.discoverdev.nms.chat.dto.ThreadDetailResponse;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/chat")
@PreAuthorize("hasRole('ADMIN')")
public class AdminChatController {

    private final ChatAccessService access;
    private final ChatService chatService;

    public AdminChatController(ChatAccessService access, ChatService chatService) {
        this.access = access;
        this.chatService = chatService;
    }

    @PostMapping("/threads/user/{userId}")
    @ResponseStatus(HttpStatus.CREATED)
    public ThreadDetailResponse openWithUser(
        @PathVariable UUID userId, @AuthenticationPrincipal AppUserDetails principal
    ) {
        ChatThread t = access.openDirectAsAdmin(principal.getUser(), userId);
        return chatService.getThread(t.getId(), principal.getUser());
    }

    @PostMapping("/threads/staff")
    @ResponseStatus(HttpStatus.CREATED)
    public ThreadDetailResponse openStaffGroup(@AuthenticationPrincipal AppUserDetails principal) {
        ChatThread t = access.openStaffGroup(principal.getUser());
        return chatService.getThread(t.getId(), principal.getUser());
    }

    @PatchMapping("/participants/{participantId}/mute")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void muteParticipant(
        @PathVariable UUID participantId,
        @Valid @RequestBody MuteRequest req,
        @AuthenticationPrincipal AppUserDetails principal
    ) {
        chatService.setParticipantMuteAsAdmin(participantId, req, principal.getUser());
    }
}
