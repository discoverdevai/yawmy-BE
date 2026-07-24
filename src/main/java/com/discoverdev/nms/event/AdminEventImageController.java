package com.discoverdev.nms.event;

import com.discoverdev.nms.event.dto.EventResponse;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/admin/events/{eventId}/image")
@PreAuthorize("hasRole('ADMIN')")
public class AdminEventImageController {

    private final EventImageService service;

    public AdminEventImageController(EventImageService service) {
        this.service = service;
    }

    @PostMapping
    public EventResponse upload(@PathVariable UUID eventId, @RequestParam("file") MultipartFile file) {
        return service.upload(eventId, file);
    }
}
