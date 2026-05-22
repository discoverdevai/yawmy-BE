package com.discoverdev.nms.child;

import com.discoverdev.nms.auth.AppUserDetails;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/files/children")
public class ChildPhotoFileController {

    private final ChildPhotoService service;

    public ChildPhotoFileController(ChildPhotoService service) {
        this.service = service;
    }

    @GetMapping("/{childId}/photo")
    public ResponseEntity<org.springframework.core.io.Resource> get(
        @PathVariable UUID childId,
        @AuthenticationPrincipal AppUserDetails principal
    ) {
        ChildPhotoService.PhotoStream stream = service.loadFor(childId, principal.getUser());
        return ResponseEntity.ok()
            .header(HttpHeaders.CACHE_CONTROL, "private, max-age=300")
            .contentType(MediaType.parseMediaType(stream.contentType()))
            .body(stream.resource());
    }
}
