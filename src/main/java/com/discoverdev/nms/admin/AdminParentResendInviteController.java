package com.discoverdev.nms.admin;

import com.discoverdev.nms.auth.AppUserDetails;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/parents")
@PreAuthorize("hasRole('ADMIN')")
public class AdminParentResendInviteController {

    private final ParentInviteResendService service;

    public AdminParentResendInviteController(ParentInviteResendService service) {
        this.service = service;
    }

    /** Re-send the invitation email to a parent who hasn't activated their account yet. */
    @PostMapping("/{id}/resend-invite")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resendInvite(@PathVariable UUID id, @AuthenticationPrincipal AppUserDetails principal) {
        service.resendInvite(id, principal.getUser());
    }
}
