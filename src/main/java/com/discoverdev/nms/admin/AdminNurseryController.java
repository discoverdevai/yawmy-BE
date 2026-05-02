package com.discoverdev.nms.admin;

import com.discoverdev.nms.admin.dto.NurseryResponse;
import com.discoverdev.nms.admin.dto.UpdateGeofenceRequest;
import com.discoverdev.nms.admin.dto.UpdateNurseryRequest;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/nursery")
@PreAuthorize("hasRole('ADMIN')")
public class AdminNurseryController {

    private final AdminNurseryService adminNurseryService;

    public AdminNurseryController(AdminNurseryService adminNurseryService) {
        this.adminNurseryService = adminNurseryService;
    }

    @GetMapping
    public NurseryResponse getCurrent() {
        return adminNurseryService.getCurrent();
    }

    @PatchMapping
    public NurseryResponse update(@Valid @RequestBody UpdateNurseryRequest req) {
        return adminNurseryService.update(req);
    }

    @PatchMapping("/geofence")
    public NurseryResponse updateGeofence(@Valid @RequestBody UpdateGeofenceRequest req) {
        return adminNurseryService.updateGeofence(req);
    }
}
