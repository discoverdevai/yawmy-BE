package com.discoverdev.nms.incident;

import com.discoverdev.nms.auth.AppUserDetails;
import com.discoverdev.nms.incident.dto.ParentIncidentResponse;
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
@RequestMapping("/api/parent")
@PreAuthorize("hasRole('PARENT')")
public class ParentIncidentController {

    private final ParentIncidentService service;

    public ParentIncidentController(ParentIncidentService service) {
        this.service = service;
    }

    @GetMapping("/children/{childId}/incidents")
    public List<ParentIncidentResponse> listForChild(
        @PathVariable UUID childId, @AuthenticationPrincipal AppUserDetails principal
    ) {
        return service.listForChild(childId, principal.getUser());
    }

    @GetMapping("/incidents/{incidentId}")
    public ParentIncidentResponse get(
        @PathVariable UUID incidentId, @AuthenticationPrincipal AppUserDetails principal
    ) {
        return service.get(incidentId, principal.getUser());
    }

    @PostMapping("/incidents/{incidentId}/acknowledge")
    public ParentIncidentResponse acknowledge(
        @PathVariable UUID incidentId, @AuthenticationPrincipal AppUserDetails principal
    ) {
        return service.acknowledge(incidentId, principal.getUser());
    }
}
