package com.discoverdev.nms.admin;

import com.discoverdev.nms.admin.dto.StaffMemberResponse;
import com.discoverdev.nms.admin.dto.StaffRoleOption;
import com.discoverdev.nms.admin.dto.UpdateStaffRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Staff (teachers) list for the enrollment module — each with an invitation
 * status of ACTIVE, PENDING, or INVITATION_EXPIRED.
 */
@RestController
@RequestMapping("/api/admin/staff")
@PreAuthorize("hasRole('ADMIN')")
public class AdminStaffController {

    private final AdminStaffService service;

    public AdminStaffController(AdminStaffService service) {
        this.service = service;
    }

    @GetMapping
    public List<StaffMemberResponse> list() {
        return service.listStaff();
    }

    /** Role options for the "add member" dropdown. */
    @GetMapping("/roles")
    public List<StaffRoleOption> roles() {
        return service.listRoles();
    }

    @GetMapping("/{id}")
    public StaffMemberResponse get(@PathVariable UUID id) {
        return service.getStaff(id);
    }

    @PatchMapping("/{id}")
    public StaffMemberResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateStaffRequest req) {
        return service.update(id, req);
    }

    /** Fully remove a teacher from the nursery (soft-delete + unassign all rooms). */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }
}
