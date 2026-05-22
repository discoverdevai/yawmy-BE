package com.discoverdev.nms.attendance;

import com.discoverdev.nms.attendance.dto.AttendanceDaySummary;
import com.discoverdev.nms.attendance.dto.ChildAttendanceResponse;
import com.discoverdev.nms.attendance.dto.NurseryInfoResponse;
import com.discoverdev.nms.attendance.dto.ParentCheckRequest;
import com.discoverdev.nms.auth.AppUserDetails;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/parent")
@PreAuthorize("hasRole('PARENT')")
public class ParentAttendanceController {

    private final ParentAttendanceService service;

    public ParentAttendanceController(ParentAttendanceService service) {
        this.service = service;
    }

    @PostMapping("/children/{childId}/attendance/check-in")
    public ChildAttendanceResponse checkIn(
        @PathVariable UUID childId,
        @Valid @RequestBody ParentCheckRequest req,
        @AuthenticationPrincipal AppUserDetails principal
    ) {
        return service.checkIn(childId, req, principal.getUser());
    }

    @PostMapping("/children/{childId}/attendance/check-out")
    public ChildAttendanceResponse checkOut(
        @PathVariable UUID childId,
        @Valid @RequestBody ParentCheckRequest req,
        @AuthenticationPrincipal AppUserDetails principal
    ) {
        return service.checkOut(childId, req, principal.getUser());
    }

    @GetMapping("/children/{childId}/attendance")
    public ChildAttendanceResponse getForDate(
        @PathVariable UUID childId,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
        @AuthenticationPrincipal AppUserDetails principal
    ) {
        return service.getForDate(childId, date, principal.getUser());
    }

    @GetMapping("/children/{childId}/attendance/history")
    public List<AttendanceDaySummary> history(
        @PathVariable UUID childId,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
        @AuthenticationPrincipal AppUserDetails principal
    ) {
        return service.history(childId, from, to, principal.getUser());
    }

    @GetMapping("/nursery")
    public NurseryInfoResponse nurseryInfo(@AuthenticationPrincipal AppUserDetails principal) {
        return service.getNurseryInfo(principal.getUser());
    }
}
