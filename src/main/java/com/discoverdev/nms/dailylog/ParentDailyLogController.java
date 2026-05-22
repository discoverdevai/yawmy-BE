package com.discoverdev.nms.dailylog;

import com.discoverdev.nms.auth.AppUserDetails;
import com.discoverdev.nms.dailylog.dto.DailyLogResponse;
import com.discoverdev.nms.dailylog.dto.DailyLogSummaryResponse;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/parent/children/{childId}/daily-logs")
@PreAuthorize("hasRole('PARENT')")
public class ParentDailyLogController {

    private final ParentDailyLogService service;

    public ParentDailyLogController(ParentDailyLogService service) {
        this.service = service;
    }

    @GetMapping(params = "date")
    public ResponseEntity<DailyLogResponse> getForDate(
        @PathVariable UUID childId,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
        @AuthenticationPrincipal AppUserDetails principal
    ) {
        return service.getByChildAndDate(childId, date, principal.getUser())
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping(params = {"from", "to"})
    public List<DailyLogSummaryResponse> history(
        @PathVariable UUID childId,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
        @AuthenticationPrincipal AppUserDetails principal
    ) {
        return service.history(childId, from, to, principal.getUser());
    }
}
