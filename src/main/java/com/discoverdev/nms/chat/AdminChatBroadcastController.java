package com.discoverdev.nms.chat;

import com.discoverdev.nms.auth.AppUserDetails;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** POST /api/admin/chat/broadcasts — Send Broadcast (Jira YC-39). */
@RestController
@RequestMapping("/api/admin/chat/broadcasts")
@PreAuthorize("hasRole('ADMIN')")
public class AdminChatBroadcastController {

    private final ChatBroadcastService service;

    public AdminChatBroadcastController(ChatBroadcastService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ChatBroadcastService.BroadcastResponse send(
        @RequestBody ChatBroadcastService.BroadcastRequest req,
        @AuthenticationPrincipal AppUserDetails principal
    ) {
        return service.send(req, principal.getUser());
    }
}
