package com.discoverdev.nms.announcement;

import com.discoverdev.nms.announcement.dto.AnnouncementResponse;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/teacher")
@PreAuthorize("hasRole('TEACHER')")
public class TeacherAnnouncementController {

    private final TeacherAnnouncementService service;

    public TeacherAnnouncementController(TeacherAnnouncementService service) {
        this.service = service;
    }

    @PostMapping("/announcements")
    @ResponseStatus(HttpStatus.CREATED)
    public AnnouncementResponse create(
        @Valid @RequestBody CreateAnnouncementRequest req,
        @AuthenticationPrincipal AppUserDetails principal
    ) {
        return service.create(req, principal.getUser());
    }

    @PatchMapping("/announcements/{id}")
    public AnnouncementResponse update(
        @PathVariable UUID id,
        @Valid @RequestBody UpdateAnnouncementRequest req,
        @AuthenticationPrincipal AppUserDetails principal
    ) {
        return service.update(id, req, principal.getUser());
    }

    @DeleteMapping("/announcements/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id, @AuthenticationPrincipal AppUserDetails principal) {
        service.delete(id, principal.getUser());
    }

    @GetMapping("/rooms/{roomId}/announcements")
    public List<AnnouncementResponse> listForRoom(
        @PathVariable UUID roomId, @AuthenticationPrincipal AppUserDetails principal
    ) {
        return service.listForRoom(roomId, principal.getUser());
    }

    @GetMapping("/announcements/{id}")
    public AnnouncementResponse get(
        @PathVariable UUID id, @AuthenticationPrincipal AppUserDetails principal
    ) {
        return service.get(id, principal.getUser());
    }

    /** Teacher opened an announcement — records this teacher as having seen it. */
    @PostMapping("/announcements/{id}/seen")
    public AnnouncementResponse markSeen(
        @PathVariable UUID id, @AuthenticationPrincipal AppUserDetails principal
    ) {
        return service.markSeen(id, principal.getUser());
    }
}
