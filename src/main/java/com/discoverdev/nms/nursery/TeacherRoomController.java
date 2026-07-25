package com.discoverdev.nms.nursery;

import com.discoverdev.nms.auth.AppUserDetails;
import com.discoverdev.nms.nursery.dto.TeacherRoomResponse;
import com.discoverdev.nms.schedule.dto.SessionResponse;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Room discovery for the teacher app. Everything else under /api/teacher needs
 * a roomId or childId; these endpoints are where the app gets them.
 */
@RestController
@RequestMapping("/api/teacher/rooms")
@PreAuthorize("hasRole('TEACHER')")
public class TeacherRoomController {

    private final TeacherRoomService service;

    public TeacherRoomController(TeacherRoomService service) {
        this.service = service;
    }

    /** The signed-in teacher's assigned rooms. Empty list when none are assigned yet. */
    @GetMapping
    public List<TeacherRoomResponse> myRooms(@AuthenticationPrincipal AppUserDetails principal) {
        return service.myRooms(principal.getUser());
    }

    /** Sessions scheduled in the room on {@code date} (yyyy-MM-dd), earliest first. */
    @GetMapping("/{roomId}/sessions")
    public List<SessionResponse> sessions(
        @PathVariable UUID roomId,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
        @AuthenticationPrincipal AppUserDetails principal
    ) {
        return service.roomSessions(roomId, date, principal.getUser());
    }
}
