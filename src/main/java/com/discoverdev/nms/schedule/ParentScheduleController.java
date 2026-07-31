package com.discoverdev.nms.schedule;

import com.discoverdev.nms.auth.AppUserDetails;
import com.discoverdev.nms.schedule.dto.SessionResponse;
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

/**
 * Parent view of a child's daily room schedule.
 * {@code GET /api/parent/children/{childId}/schedule?date=YYYY-MM-DD}.
 */
@RestController
@RequestMapping("/api/parent/children/{childId}/schedule")
@PreAuthorize("hasRole('PARENT')")
public class ParentScheduleController {

    private final ParentScheduleService service;

    public ParentScheduleController(ParentScheduleService service) {
        this.service = service;
    }

    @GetMapping
    public List<SessionResponse> schedule(
        @PathVariable UUID childId,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
        @AuthenticationPrincipal AppUserDetails principal
    ) {
        return service.schedule(childId, date, principal.getUser());
    }
}
