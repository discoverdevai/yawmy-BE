package com.discoverdev.nms.notification;

import com.discoverdev.nms.auth.AppUserDetails;
import com.discoverdev.nms.notification.dto.NotificationResponse;
import com.discoverdev.nms.notification.dto.UnreadCountResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me/notifications")
public class NotificationController {

    private final NotificationService service;

    public NotificationController(NotificationService service) {
        this.service = service;
    }

    @GetMapping
    public List<NotificationResponse> list(
        @RequestParam(value = "status", defaultValue = "all") String status,
        @AuthenticationPrincipal AppUserDetails principal
    ) {
        return service.list(principal.getUser(), status);
    }

    @GetMapping("/unread-count")
    public UnreadCountResponse unreadCount(@AuthenticationPrincipal AppUserDetails principal) {
        return new UnreadCountResponse(service.unreadCount(principal.getUser()));
    }

    @PostMapping("/{id}/read")
    public NotificationResponse markRead(
        @PathVariable UUID id, @AuthenticationPrincipal AppUserDetails principal
    ) {
        return service.markRead(id, principal.getUser());
    }

    @PostMapping("/read-all")
    public UnreadCountResponse markAllRead(@AuthenticationPrincipal AppUserDetails principal) {
        service.markAllRead(principal.getUser());
        return new UnreadCountResponse(service.unreadCount(principal.getUser()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id, @AuthenticationPrincipal AppUserDetails principal) {
        service.softDelete(id, principal.getUser());
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAll(@AuthenticationPrincipal AppUserDetails principal) {
        service.deleteAll(principal.getUser());
    }
}
