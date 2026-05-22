package com.discoverdev.nms.admin;

import com.discoverdev.nms.admin.dto.TeacherAssignmentResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminTeacherAssignmentController {

    private final AdminTeacherAssignmentService service;

    public AdminTeacherAssignmentController(AdminTeacherAssignmentService service) {
        this.service = service;
    }

    @PostMapping("/rooms/{roomId}/teachers/{teacherId}")
    @ResponseStatus(HttpStatus.CREATED)
    public TeacherAssignmentResponse assign(@PathVariable UUID roomId, @PathVariable UUID teacherId) {
        return service.assign(roomId, teacherId);
    }

    @DeleteMapping("/rooms/{roomId}/teachers/{teacherId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unassign(@PathVariable UUID roomId, @PathVariable UUID teacherId) {
        service.unassign(roomId, teacherId);
    }

    @GetMapping("/rooms/{roomId}/teachers")
    public List<TeacherAssignmentResponse> listByRoom(@PathVariable UUID roomId) {
        return service.listByRoom(roomId);
    }

    @GetMapping("/teachers/{teacherId}/rooms")
    public List<TeacherAssignmentResponse> listByTeacher(@PathVariable UUID teacherId) {
        return service.listByTeacher(teacherId);
    }
}
