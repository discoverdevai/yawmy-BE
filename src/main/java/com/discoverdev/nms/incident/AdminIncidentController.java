package com.discoverdev.nms.incident;

import com.discoverdev.nms.auth.AppUserDetails;
import com.discoverdev.nms.incident.dto.AcknowledgmentRow;
import com.discoverdev.nms.incident.dto.CreateIncidentRequest;
import com.discoverdev.nms.incident.dto.IncidentResponse;
import com.discoverdev.nms.incident.dto.UpdateIncidentRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminIncidentController {

    private final AdminIncidentService service;

    public AdminIncidentController(AdminIncidentService service) {
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
        @Valid @RequestBody UpdateIncidentRequest req
    ) {
        return service.update(incidentId, req);
    }

    @GetMapping("/incidents")
    public List<IncidentResponse> listAll() {
        return service.listNurseryWide();
    }

    @GetMapping("/incidents/{incidentId}")
    public IncidentResponse get(@PathVariable UUID incidentId) {
        return service.get(incidentId);
    }

    @GetMapping("/children/{childId}/incidents")
    public List<IncidentResponse> listForChild(@PathVariable UUID childId) {
        return service.listForChild(childId);
    }

    @GetMapping("/incidents/{incidentId}/acknowledgments")
    public List<AcknowledgmentRow> acknowledgments(@PathVariable UUID incidentId) {
        return service.listAcknowledgments(incidentId);
    }
}
