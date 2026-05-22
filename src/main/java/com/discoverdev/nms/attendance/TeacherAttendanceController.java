package com.discoverdev.nms.attendance;

import com.discoverdev.nms.attendance.dto.ChildAttendanceResponse;
import com.discoverdev.nms.attendance.dto.ManualCheckRequest;
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
@RequestMapping("/api/teacher")
@PreAuthorize("hasRole('TEACHER')")
public class TeacherAttendanceController {

    private final TeacherAttendanceService service;

    public TeacherAttendanceController(TeacherAttendanceService service) {
        this.service = service;
    }

    @PostMapping("/children/{childId}/attendance/check-in")
    public ChildAttendanceResponse checkIn(
        @PathVariable UUID childId,
        @Valid @RequestBody(required = false) ManualCheckRequest req,
        @AuthenticationPrincipal AppUserDetails principal
    ) {
        return service.checkIn(childId, req, principal.getUser());
    }

    @PostMapping("/children/{childId}/attendance/check-out")
    public ChildAttendanceResponse checkOut(
        @PathVariable UUID childId,
        @Valid @RequestBody(required = false) ManualCheckRequest req,
        @AuthenticationPrincipal AppUserDetails principal
    ) {
        return service.checkOut(childId, req, principal.getUser());
    }

    @GetMapping("/rooms/{roomId}/attendance")
    public List<ChildAttendanceResponse> roomRoster(
        @PathVariable UUID roomId,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
        @AuthenticationPrincipal AppUserDetails principal
    ) {
        return service.roomRoster(roomId, date, principal.getUser());
    }
}
