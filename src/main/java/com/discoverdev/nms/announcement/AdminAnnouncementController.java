package com.discoverdev.nms.announcement;

import com.discoverdev.nms.announcement.dto.AnnouncementResponse;
import com.discoverdev.nms.announcement.dto.AnnouncementSeenRow;
import com.discoverdev.nms.announcement.dto.CreateAnnouncementRequest;
import com.discoverdev.nms.announcement.dto.UpdateAnnouncementRequest;
import com.discoverdev.nms.auth.AppUserDetails;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/announcements")
@PreAuthorize("hasRole('ADMIN')")
public class AdminAnnouncementController {

    private final AdminAnnouncementService service;

    public AdminAnnouncementController(AdminAnnouncementService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AnnouncementResponse create(
        @Valid @RequestBody CreateAnnouncementRequest req,
        @AuthenticationPrincipal AppUserDetails principal
    ) {
        return service.create(req, principal.getUser());
    }

    @PatchMapping("/{id}")
    public AnnouncementResponse update(
        @PathVariable UUID id, @Valid @RequestBody UpdateAnnouncementRequest req
    ) {
        return service.update(id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }

    @GetMapping
    public List<AnnouncementResponse> list(@RequestParam(value = "roomId", required = false) UUID roomId) {
        return service.list(roomId);
    }

    @GetMapping("/{id}")
    public AnnouncementResponse get(@PathVariable UUID id) {
        return service.get(id);
    }

    @GetMapping("/{id}/seen-by")
    public List<AnnouncementSeenRow> seenBy(@PathVariable UUID id) {
        return service.seenBy(id);
    }
}
