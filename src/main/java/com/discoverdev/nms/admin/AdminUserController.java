package com.discoverdev.nms.admin;

import com.discoverdev.nms.admin.dto.AdminUserResponse;
import com.discoverdev.nms.auth.AppUserDetails;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    @PatchMapping("/{id}/disable")
    public AdminUserResponse disable(
        @PathVariable UUID id,
        @AuthenticationPrincipal AppUserDetails principal
    ) {
        return adminUserService.disable(id, principal.getUser());
    }

    @PatchMapping("/{id}/enable")
    public AdminUserResponse enable(@PathVariable UUID id) {
        return adminUserService.enable(id);
    }
}