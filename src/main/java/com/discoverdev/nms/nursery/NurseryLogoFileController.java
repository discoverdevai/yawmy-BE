package com.discoverdev.nms.nursery;

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
@RequestMapping("/api/files/nursery")
public class NurseryLogoFileController {

    private final NurseryLogoService service;

    public NurseryLogoFileController(NurseryLogoService service) {
        this.service = service;
    }

    @GetMapping("/{nurseryId}/logo")
    public ResponseEntity<Resource> get(
        @PathVariable UUID nurseryId,
        @AuthenticationPrincipal AppUserDetails principal
    ) {
        NurseryLogoService.LogoStream stream = service.loadFor(nurseryId, principal.getUser());
        return ResponseEntity.ok()
            .header(HttpHeaders.CACHE_CONTROL, "private, max-age=300")
            .contentType(MediaType.parseMediaType(stream.contentType()))
            .body(stream.resource());
    }
}