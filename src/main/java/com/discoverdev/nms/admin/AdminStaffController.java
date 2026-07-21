package com.discoverdev.nms.admin;

import com.discoverdev.nms.admin.dto.StaffMemberResponse;
import com.discoverdev.nms.admin.dto.StaffRoleOption;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
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
}
