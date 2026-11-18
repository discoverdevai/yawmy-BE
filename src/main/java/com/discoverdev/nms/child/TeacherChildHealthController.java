package com.discoverdev.nms.child;

import com.discoverdev.nms.auth.AppUserDetails;
import com.discoverdev.nms.child.dto.HealthEntryResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Read-only health lists for the teacher's child profile (Staff App, Jira YC-233). */
@RestController
@RequestMapping("/api/teacher/children/{childId}/{list:allergies|immunizations|medical-notes}")
@PreAuthorize("hasRole('TEACHER')")
public class TeacherChildHealthController {

    private final ChildHealthService service;

    public TeacherChildHealthController(ChildHealthService service) {
        this.service = service;
    }

    @GetMapping
    public List<HealthEntryResponse> list(
        @PathVariable UUID childId, @PathVariable String list, @AuthenticationPrincipal AppUserDetails principal
    ) {
        return service.listForTeacher(childId, HealthEntryType.fromPath(list), principal.getUser());
    }
}
