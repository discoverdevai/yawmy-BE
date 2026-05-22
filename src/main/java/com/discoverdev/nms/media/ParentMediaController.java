package com.discoverdev.nms.media;

import com.discoverdev.nms.auth.AppUserDetails;
import com.discoverdev.nms.media.dto.MediaItemResponse;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/parent/children/{childId}/media")
@PreAuthorize("hasRole('PARENT')")
public class ParentMediaController {

    private final ParentMediaService service;

    public ParentMediaController(ParentMediaService service) {
        this.service = service;
    }

    @GetMapping
    public List<MediaItemResponse> list(
        @PathVariable UUID childId,
        @RequestParam(value = "date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
        @RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @RequestParam(value = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
        @AuthenticationPrincipal AppUserDetails principal
    ) {
        return service.listForChild(childId, date, from, to, principal.getUser());
    }
}
