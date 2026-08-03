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

    /**
     * The parent's conversation with the nursery's admins — one thread, whichever
     * admin answers. There is no id to supply: a parent should not have to know
     * how many admins the nursery has, or which of them is on shift.
     */
    @PostMapping("/threads/admin")
    @ResponseStatus(HttpStatus.CREATED)
    public ThreadDetailResponse openWithNursery(@AuthenticationPrincipal AppUserDetails principal) {
        ChatThread t = access.openAdminSupport(principal.getUser());
        return chatService.getThread(t.getId(), principal.getUser());
    }

    /** Resolves to the same thread as {@link #openWithNursery}, for callers that still send an id. */
    @PostMapping("/threads/admin/{adminId}")
    @ResponseStatus(HttpStatus.CREATED)
    public ThreadDetailResponse openWithAdmin(
        @PathVariable UUID adminId, @AuthenticationPrincipal AppUserDetails principal
    ) {
        ChatThread t = access.openDirectAsParent(principal.getUser(), adminId);
        return chatService.getThread(t.getId(), principal.getUser());
    }
}
