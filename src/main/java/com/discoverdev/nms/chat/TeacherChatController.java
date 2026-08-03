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
@RequestMapping("/api/teacher/chat")
@PreAuthorize("hasRole('TEACHER')")
public class TeacherChatController {

    private final ChatAccessService access;
    private final ChatService chatService;

    public TeacherChatController(ChatAccessService access, ChatService chatService) {
        this.access = access;
        this.chatService = chatService;
    }

    @PostMapping("/threads/parent/{parentId}")
    @ResponseStatus(HttpStatus.CREATED)
    public ThreadDetailResponse openWithParent(
        @PathVariable UUID parentId, @AuthenticationPrincipal AppUserDetails principal
    ) {
        ChatThread t = access.openDirectAsTeacher(principal.getUser(), parentId);
        return chatService.getThread(t.getId(), principal.getUser());
    }

    /**
     * A DM with anyone the teacher is allowed to reach — a parent in one of their
     * rooms, or a colleague. `threads/parent/{id}` does the same thing, but its
     * name makes calling it for another teacher look like a mistake.
     */
    @PostMapping("/threads/user/{userId}")
    @ResponseStatus(HttpStatus.CREATED)
    public ThreadDetailResponse openWithUser(
        @PathVariable UUID userId, @AuthenticationPrincipal AppUserDetails principal
    ) {
        ChatThread t = access.openDirectAsTeacher(principal.getUser(), userId);
        return chatService.getThread(t.getId(), principal.getUser());
    }

    /**
     * The teacher's conversation with the nursery's admins — one thread, whichever
     * admin answers. This is the only chat a teacher has before any room is
     * assigned to them.
     */
    @PostMapping("/threads/admin")
    @ResponseStatus(HttpStatus.CREATED)
    public ThreadDetailResponse openWithNursery(@AuthenticationPrincipal AppUserDetails principal) {
        ChatThread t = access.openAdminSupport(principal.getUser());
        return chatService.getThread(t.getId(), principal.getUser());
    }

    @PostMapping("/threads/room/{roomId}")
    @ResponseStatus(HttpStatus.CREATED)
    public ThreadDetailResponse openRoomGroup(
        @PathVariable UUID roomId, @AuthenticationPrincipal AppUserDetails principal
    ) {
        ChatThread t = access.openRoomGroupAsTeacher(principal.getUser(), roomId);
        return chatService.getThread(t.getId(), principal.getUser());
    }

    @PostMapping("/threads/staff")
    @ResponseStatus(HttpStatus.CREATED)
    public ThreadDetailResponse openStaffGroup(@AuthenticationPrincipal AppUserDetails principal) {
        ChatThread t = access.openStaffGroup(principal.getUser());
        return chatService.getThread(t.getId(), principal.getUser());
    }
}
