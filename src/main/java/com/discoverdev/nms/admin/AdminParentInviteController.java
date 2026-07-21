package com.discoverdev.nms.admin;

import com.discoverdev.nms.admin.dto.EnrollFamilyRequest;
import com.discoverdev.nms.admin.dto.EnrollFamilyResponse;
import com.discoverdev.nms.auth.AppUserDetails;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * One-shot parent enrollment for the 3-step "Invite Parent" flow. Creates the
 * children + parent + guardians + pickups atomically and emails the parent.
 */
@RestController
@RequestMapping("/api/admin/parent-invites")
@PreAuthorize("hasRole('ADMIN')")
public class AdminParentInviteController {

    private final AdminParentInviteService service;

    public AdminParentInviteController(AdminParentInviteService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EnrollFamilyResponse enroll(
        @Valid @RequestBody EnrollFamilyRequest req,
        @AuthenticationPrincipal AppUserDetails principal
    ) {
        return service.enroll(req, principal.getUser());
    }
}
