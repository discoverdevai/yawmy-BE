package com.discoverdev.nms.event;

import com.discoverdev.nms.auth.AppUserDetails;
import java.util.UUID;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/files/events")
public class EventImageFileController {

    private final EventImageService service;

    public EventImageFileController(EventImageService service) {
        this.service = service;
    }

    @GetMapping("/{eventId}/image")
    public ResponseEntity<Resource> get(
        @PathVariable UUID eventId,
        @AuthenticationPrincipal AppUserDetails principal
    ) {
        EventImageService.ImageStream stream = service.loadFor(eventId, principal.getUser());
        return ResponseEntity.ok()
            .header(HttpHeaders.CACHE_CONTROL, "private, max-age=300")
            .contentType(MediaType.parseMediaType(stream.contentType()))
            .body(stream.resource());
    }
}
