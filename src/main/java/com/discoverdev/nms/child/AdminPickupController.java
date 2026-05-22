package com.discoverdev.nms.child;

import com.discoverdev.nms.child.dto.ContactRequest;
import com.discoverdev.nms.child.dto.PickupResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/children/{childId}/pickups")
@PreAuthorize("hasRole('ADMIN')")
public class AdminPickupController {

    private final AdminPickupService service;

    public AdminPickupController(AdminPickupService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PickupResponse create(@PathVariable UUID childId, @Valid @RequestBody ContactRequest req) {
        return service.create(childId, req);
    }

    @GetMapping
    public List<PickupResponse> list(@PathVariable UUID childId) {
        return service.list(childId);
    }

    @PatchMapping("/{pickupId}")
    public PickupResponse update(
        @PathVariable UUID childId,
        @PathVariable UUID pickupId,
        @Valid @RequestBody ContactRequest req
    ) {
        return service.update(childId, pickupId, req);
    }

    @DeleteMapping("/{pickupId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID childId, @PathVariable UUID pickupId) {
        service.delete(childId, pickupId);
    }
}
