package com.discoverdev.nms.media;

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
@RequestMapping("/api/files/media")
public class MediaFileController {

    private final MediaFileService service;

    public MediaFileController(MediaFileService service) {
        this.service = service;
    }

    @GetMapping("/{mediaId}")
    public ResponseEntity<Resource> get(
        @PathVariable UUID mediaId, @AuthenticationPrincipal AppUserDetails principal
    ) {
        MediaFileService.StreamedFile stream = service.loadFor(mediaId, principal.getUser());
        return ResponseEntity.ok()
            .header(HttpHeaders.CACHE_CONTROL, "private, max-age=600")
            .contentType(MediaType.parseMediaType(stream.contentType()))
            .body(stream.resource());
    }
}
