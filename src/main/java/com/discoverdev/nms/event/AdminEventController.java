package com.discoverdev.nms.event;

import com.discoverdev.nms.auth.AppUserDetails;
import com.discoverdev.nms.event.dto.CreateEventRequest;
import com.discoverdev.nms.event.dto.EventResponse;
import com.discoverdev.nms.event.dto.UpdateEventRequest;
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
@RequestMapping("/api/admin/events")
@PreAuthorize("hasRole('ADMIN')")
public class AdminEventController {

    private final AdminEventService service;

    public AdminEventController(AdminEventService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EventResponse create(
        @Valid @RequestBody CreateEventRequest req,
        @AuthenticationPrincipal AppUserDetails principal
    ) {
        return service.create(req, principal.getUser());
    }

    @PatchMapping("/{id}")
    public EventResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateEventRequest req) {
        return service.update(id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }

    @GetMapping
    public List<EventResponse> list(
        @RequestParam(value = "upcoming", defaultValue = "false") boolean upcoming
    ) {
        return service.list(upcoming);
    }

    @GetMapping("/{id}")
    public EventResponse get(@PathVariable UUID id) {
        return service.get(id);
    }
}
