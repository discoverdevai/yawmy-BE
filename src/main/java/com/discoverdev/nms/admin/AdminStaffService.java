package com.discoverdev.nms.admin;

import com.discoverdev.nms.admin.dto.StaffMemberResponse;
import com.discoverdev.nms.admin.dto.StaffRoleOption;
import com.discoverdev.nms.auth.Invite;
import com.discoverdev.nms.auth.InviteRepository;
import com.discoverdev.nms.tenant.TenantContext;
import com.discoverdev.nms.user.Role;
import com.discoverdev.nms.user.StaffRole;
import com.discoverdev.nms.user.User;
import com.discoverdev.nms.user.UserRepository;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminStaffService {

    private final UserRepository userRepository;
    private final InviteRepository inviteRepository;

    public AdminStaffService(UserRepository userRepository, InviteRepository inviteRepository) {
        this.userRepository = userRepository;
        this.inviteRepository = inviteRepository;
    }

    /** All teachers in the nursery with their invitation status. */
    @Transactional(readOnly = true)
    public List<StaffMemberResponse> listStaff() {
        UUID nurseryId = TenantContext.currentNurseryId();
        return userRepository.findByNurseryIdAndRoleOrderByFullNameAsc(nurseryId, Role.TEACHER)
            .stream()
            .map(t -> new StaffMemberResponse(
                t.getId(),
                t.getFullName(),
                t.getEmail(),
                t.getRole(),
                t.getStaffRole() != null ? t.getStaffRole().getLabel() : null,
                t.getImageUrl(),
                statusOf(t)))
            .toList();
    }

    /** The staff-role options for the "add member" dropdown. */
    @Transactional(readOnly = true)
    public List<StaffRoleOption> listRoles() {
        return Arrays.stream(StaffRole.values()).map(StaffRoleOption::of).toList();
    }

    private String statusOf(User teacher) {
        // Accepting an invite enables the account.
        if (teacher.isEnabled()) {
            return "ACTIVE";
        }
        // Not yet accepted → look at the latest invite to tell pending from expired.
        Invite latest = inviteRepository.findFirstByUserIdOrderByCreatedAtDesc(teacher.getId()).orElse(null);
        if (latest != null && !latest.isUsed() && !latest.isExpired()) {
            return "PENDING";
        }
        return "INVITATION_EXPIRED";
    }
}
