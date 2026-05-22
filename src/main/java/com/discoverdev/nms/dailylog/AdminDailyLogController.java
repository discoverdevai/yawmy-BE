package com.discoverdev.nms.dailylog;

import com.discoverdev.nms.dailylog.dto.DailyLogResponse;
import com.discoverdev.nms.dailylog.dto.DailyLogSummaryResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminDailyLogController {

    private final AdminDailyLogService service;

    public AdminDailyLogController(AdminDailyLogService service) {
        this.service = service;
    }

    @GetMapping("/children/{childId}/daily-logs")
    public List<DailyLogSummaryResponse> listForChild(@PathVariable UUID childId) {
        return service.listForChild(childId);
    }

    @GetMapping("/daily-logs/{logId}")
    public DailyLogResponse get(@PathVariable UUID logId) {
        return service.get(logId);
    }
}
