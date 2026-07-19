package com.discoverdev.nms.admin;

import com.discoverdev.nms.admin.dto.DashboardResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Home-screen summary for the logged-in admin's nursery: headline counts +
 * the current week's daily attendance for the chart.
 */
@RestController
@RequestMapping("/api/admin/dashboard")
@PreAuthorize("hasRole('ADMIN')")
public class AdminDashboardController {

    private final AdminDashboardService service;

    public AdminDashboardController(AdminDashboardService service) {
        this.service = service;
    }

    @GetMapping
    public DashboardResponse get() {
        return service.getDashboard();
    }
}
