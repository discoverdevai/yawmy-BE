package com.discoverdev.nms.admin;

import com.discoverdev.nms.admin.dto.CreateInviteRequest;
import com.discoverdev.nms.admin.dto.CreateInviteResponse;
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

@RestController
@RequestMapping("/api/admin/invites")
@PreAuthorize("hasRole('ADMIN')")
public class AdminInviteController {

    private final AdminInviteService adminInviteService;

    public AdminInviteController(AdminInviteService adminInviteService) {
        this.adminInviteService = adminInviteService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CreateInviteResponse create(
        @Valid @RequestBody CreateInviteRequest req,
        @AuthenticationPrincipal AppUserDetails principal
    ) {
        return adminInviteService.createInvite(req, principal.getUser());
    }
}
