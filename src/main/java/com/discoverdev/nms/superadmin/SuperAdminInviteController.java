package com.discoverdev.nms.superadmin;

import com.discoverdev.nms.admin.dto.CreateInviteResponse;
import com.discoverdev.nms.auth.AppUserDetails;
import com.discoverdev.nms.superadmin.dto.InviteAdminRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import java.util.UUID;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * One-call admin invitation: creates a bare nursery (id only) + disabled admin
 * + invite token. The admin sets up the nursery after accepting.
 */
@RestController
@RequestMapping("/api/super-admin/admin-invites")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class SuperAdminInviteController {

    private final SuperAdminNurseryService service;

    public SuperAdminInviteController(SuperAdminNurseryService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CreateInviteResponse inviteAdmin(
        @Valid @RequestBody InviteAdminRequest req,
        @AuthenticationPrincipal AppUserDetails principal
    ) {
        return service.inviteAdmin(req, principal.getUser());
    }

    @PostMapping("/{userId}/resend")
    @ResponseStatus(HttpStatus.CREATED)
    public CreateInviteResponse resend(
        @PathVariable UUID userId,
        @AuthenticationPrincipal AppUserDetails principal
    ) {
        return service.resendInvite(userId, principal.getUser());
    }
}