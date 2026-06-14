package com.discoverdev.nms.chat;

import com.discoverdev.nms.auth.AppUserDetails;
import com.discoverdev.nms.chat.dto.ThreadDetailResponse;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/parent/chat")
@PreAuthorize("hasRole('PARENT')")
public class ParentChatController {

    private final ChatAccessService access;
    private final ChatService chatService;

    public ParentChatController(ChatAccessService access, ChatService chatService) {
        this.access = access;
        this.chatService = chatService;
    }

    @PostMapping("/threads/teacher/{teacherId}")
    @ResponseStatus(HttpStatus.CREATED)
    public ThreadDetailResponse openWithTeacher(
        @PathVariable UUID teacherId, @AuthenticationPrincipal AppUserDetails principal
    ) {
        ChatThread t = access.openDirectAsParent(principal.getUser(), teacherId);
        return chatService.getThread(t.getId(), principal.getUser());
    }

    @PostMapping("/threads/admin/{adminId}")
    @ResponseStatus(HttpStatus.CREATED)
    public ThreadDetailResponse openWithAdmin(
        @PathVariable UUID adminId, @AuthenticationPrincipal AppUserDetails principal
    ) {
        ChatThread t = access.openDirectAsParent(principal.getUser(), adminId);
        return chatService.getThread(t.getId(), principal.getUser());
    }
}
