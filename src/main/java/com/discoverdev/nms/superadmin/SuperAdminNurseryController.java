package com.discoverdev.nms.superadmin;

import com.discoverdev.nms.admin.dto.CreateInviteResponse;
import com.discoverdev.nms.admin.dto.NurseryResponse;
import com.discoverdev.nms.auth.AppUserDetails;
import com.discoverdev.nms.superadmin.dto.CreateNurseryRequest;
import com.discoverdev.nms.superadmin.dto.InviteNurseryAdminRequest;
import com.discoverdev.nms.superadmin.dto.RenameNurseryRequest;
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
@RequestMapping("/api/super-admin/nurseries")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class SuperAdminNurseryController {

    private final SuperAdminNurseryService service;

    public SuperAdminNurseryController(SuperAdminNurseryService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public NurseryResponse create(@Valid @RequestBody CreateNurseryRequest req) {
        return service.create(req);
    }

    @GetMapping
    public List<NurseryResponse> list() {
        return service.list();
    }

    @GetMapping("/{id}")
    public NurseryResponse get(@PathVariable UUID id) {
        return service.get(id);
    }

    @PatchMapping("/{id}")
    public NurseryResponse rename(@PathVariable UUID id, @Valid @RequestBody RenameNurseryRequest req) {
        return service.rename(id, req);
    }

    @PostMapping("/{id}/admin-invites")
    @ResponseStatus(HttpStatus.CREATED)
    public CreateInviteResponse inviteAdmin(
        @PathVariable UUID id,
        @Valid @RequestBody InviteNurseryAdminRequest req,
        @AuthenticationPrincipal AppUserDetails principal
    ) {
        return service.inviteAdmin(id, req, principal.getUser());
    }
}
