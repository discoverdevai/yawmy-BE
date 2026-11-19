package com.discoverdev.nms.admin;

import com.discoverdev.nms.user.Role;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Archive Member / Restore Member (Jira YC-118, YC-119) for staff and parents. */
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminMemberArchiveController {

    private final MemberArchiveService service;

    public AdminMemberArchiveController(MemberArchiveService service) {
        this.service = service;
    }

    @PostMapping("/staff/{id}/archive")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void archiveStaff(@PathVariable UUID id) {
        service.archive(id, Role.TEACHER);
    }

    @PostMapping("/staff/{id}/restore")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void restoreStaff(@PathVariable UUID id) {
        service.restore(id, Role.TEACHER);
    }

    @PostMapping("/parents/{id}/archive")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void archiveParent(@PathVariable UUID id) {
        service.archive(id, Role.PARENT);
    }

    @PostMapping("/parents/{id}/restore")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void restoreParent(@PathVariable UUID id) {
        service.restore(id, Role.PARENT);
    }
}
