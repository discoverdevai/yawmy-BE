package com.discoverdev.nms.child;

import com.discoverdev.nms.auth.AppUserDetails;
import com.discoverdev.nms.child.dto.GuardianLinkResponse;
import com.discoverdev.nms.child.dto.LinkOrInviteGuardianRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/children/{childId}/guardians")
@PreAuthorize("hasRole('ADMIN')")
public class AdminGuardianController {

    private final AdminGuardianService service;

    public AdminGuardianController(AdminGuardianService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public GuardianLinkResponse linkOrInvite(
        @PathVariable UUID childId,
        @Valid @RequestBody LinkOrInviteGuardianRequest req,
        @AuthenticationPrincipal AppUserDetails principal
    ) {
        return service.linkOrInvite(childId, req, principal.getUser());
    }

    @GetMapping
    public List<GuardianLinkResponse> list(@PathVariable UUID childId) {
        return service.listForChild(childId);
    }

    @DeleteMapping("/{guardianId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unlink(@PathVariable UUID childId, @PathVariable UUID guardianId) {
        service.unlink(childId, guardianId);
    }
}
