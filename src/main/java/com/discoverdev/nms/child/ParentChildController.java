package com.discoverdev.nms.child;

import com.discoverdev.nms.auth.AppUserDetails;
import com.discoverdev.nms.child.dto.ChildResponse;
import com.discoverdev.nms.child.dto.ParentChildDetailResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/parent/children")
@PreAuthorize("hasRole('PARENT')")
public class ParentChildController {

    private final ParentChildService service;

    public ParentChildController(ParentChildService service) {
        this.service = service;
    }

    @GetMapping
    public List<ChildResponse> list(@AuthenticationPrincipal AppUserDetails principal) {
        return service.listForParent(principal.getUser());
    }

    @GetMapping("/{id}")
    public ParentChildDetailResponse get(
        @PathVariable UUID id,
        @AuthenticationPrincipal AppUserDetails principal
    ) {
        return service.getForParent(id, principal.getUser());
    }
}
