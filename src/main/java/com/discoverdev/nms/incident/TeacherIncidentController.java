package com.discoverdev.nms.incident;

import com.discoverdev.nms.auth.AppUserDetails;
import com.discoverdev.nms.incident.dto.AcknowledgmentRow;
import com.discoverdev.nms.incident.dto.CreateIncidentRequest;
import com.discoverdev.nms.incident.dto.IncidentResponse;
import com.discoverdev.nms.incident.dto.UpdateIncidentRequest;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
@RequestMapping("/api/teacher")
@PreAuthorize("hasRole('TEACHER')")
public class TeacherIncidentController {

    private final TeacherIncidentService service;

    public TeacherIncidentController(TeacherIncidentService service) {
        this.service = service;
    }

    @PostMapping("/children/{childId}/incidents")
    @ResponseStatus(HttpStatus.CREATED)
    public IncidentResponse create(
        @PathVariable UUID childId,
        @Valid @RequestBody CreateIncidentRequest req,
        @AuthenticationPrincipal AppUserDetails principal
    ) {
        return service.create(childId, req, principal.getUser());
    }

    @PatchMapping("/incidents/{incidentId}")
    public IncidentResponse update(
        @PathVariable UUID incidentId,
        @Valid @RequestBody UpdateIncidentRequest req,
        @AuthenticationPrincipal AppUserDetails principal
    ) {
        return service.update(incidentId, req, principal.getUser());
    }

    @GetMapping("/incidents/{incidentId}")
    public IncidentResponse get(
        @PathVariable UUID incidentId, @AuthenticationPrincipal AppUserDetails principal
    ) {
        return service.get(incidentId, principal.getUser());
    }

    /** A child's incidents, newest first. Omit {@code date} for the whole history. */
    @GetMapping("/children/{childId}/incidents")
    public List<IncidentResponse> listForChild(
        @PathVariable UUID childId,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
        @AuthenticationPrincipal AppUserDetails principal
    ) {
        return service.listForChild(childId, date, principal.getUser());
    }

    @GetMapping("/rooms/{roomId}/incidents")
    public List<IncidentResponse> listForRoom(
        @PathVariable UUID roomId, @AuthenticationPrincipal AppUserDetails principal
    ) {
        return service.listForRoom(roomId, principal.getUser());
    }

    @GetMapping("/incidents/{incidentId}/acknowledgments")
    public List<AcknowledgmentRow> acknowledgments(
        @PathVariable UUID incidentId, @AuthenticationPrincipal AppUserDetails principal
    ) {
        return service.listAcknowledgments(incidentId, principal.getUser());
    }
}
