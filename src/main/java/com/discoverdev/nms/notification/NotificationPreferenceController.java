package com.discoverdev.nms.notification;

import com.discoverdev.nms.auth.AppUserDetails;
import com.discoverdev.nms.notification.dto.PreferencesResponse;
import com.discoverdev.nms.notification.dto.UpdatePreferencesRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me/notification-preferences")
public class NotificationPreferenceController {

    private final NotificationPreferenceService service;

    public NotificationPreferenceController(NotificationPreferenceService service) {
        this.service = service;
    }

    @GetMapping
    public PreferencesResponse get(@AuthenticationPrincipal AppUserDetails principal) {
        return service.get(principal.getUser());
    }

    @PatchMapping
    public PreferencesResponse update(
        @RequestBody UpdatePreferencesRequest req,
        @AuthenticationPrincipal AppUserDetails principal
    ) {
        return service.update(principal.getUser(), req);
    }
}
