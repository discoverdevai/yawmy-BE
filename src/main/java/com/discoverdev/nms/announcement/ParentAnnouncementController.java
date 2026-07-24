package com.discoverdev.nms.announcement;

import com.discoverdev.nms.announcement.dto.AnnouncementResponse;
import com.discoverdev.nms.auth.AppUserDetails;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/parent/announcements")
@PreAuthorize("hasRole('PARENT')")
public class ParentAnnouncementController {

    private final ParentAnnouncementService service;

    public ParentAnnouncementController(ParentAnnouncementService service) {
        this.service = service;
    }

    @GetMapping
    public List<AnnouncementResponse> list(@AuthenticationPrincipal AppUserDetails principal) {
        return service.listForParent(principal.getUser());
    }

    /** Nursery-wide announcements only. */
    @GetMapping("/nursery")
    public List<AnnouncementResponse> listNurseryWide(@AuthenticationPrincipal AppUserDetails principal) {
        return service.listNurseryWide(principal.getUser());
    }

    /** Announcements for the parent's children's rooms only. */
    @GetMapping("/rooms")
    public List<AnnouncementResponse> listForRooms(@AuthenticationPrincipal AppUserDetails principal) {
        return service.listForRooms(principal.getUser());
    }

    @GetMapping("/{id}")
    public AnnouncementResponse get(
        @PathVariable UUID id, @AuthenticationPrincipal AppUserDetails principal
    ) {
        return service.get(id, principal.getUser());
    }

    /** Parent opened an announcement — records this parent as having seen it. */
    @PostMapping("/{id}/seen")
    public AnnouncementResponse markSeen(
        @PathVariable UUID id, @AuthenticationPrincipal AppUserDetails principal
    ) {
        return service.markSeen(id, principal.getUser());
    }
}
