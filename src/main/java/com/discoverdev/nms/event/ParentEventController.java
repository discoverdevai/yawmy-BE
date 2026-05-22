package com.discoverdev.nms.event;

import com.discoverdev.nms.auth.AppUserDetails;
import com.discoverdev.nms.event.dto.EventResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/parent")
@PreAuthorize("hasRole('PARENT')")
public class ParentEventController {

    private final ParentEventService service;

    public ParentEventController(ParentEventService service) {
        this.service = service;
    }

    @GetMapping("/events")
    public List<EventResponse> list(
        @RequestParam(value = "upcoming", defaultValue = "false") boolean upcoming,
        @AuthenticationPrincipal AppUserDetails principal
    ) {
        return service.list(upcoming, principal.getUser());
    }

    @GetMapping("/events/saved")
    public List<EventResponse> savedTab(@AuthenticationPrincipal AppUserDetails principal) {
        return service.savedTab(principal.getUser());
    }

    @PostMapping("/events/{id}/save")
    public EventResponse save(
        @PathVariable UUID id, @AuthenticationPrincipal AppUserDetails principal
    ) {
        return service.save(id, principal.getUser());
    }

    @DeleteMapping("/events/{id}/save")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unsave(@PathVariable UUID id, @AuthenticationPrincipal AppUserDetails principal) {
        service.unsave(id, principal.getUser());
    }
}
