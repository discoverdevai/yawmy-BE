package com.discoverdev.nms.admin;

import com.discoverdev.nms.admin.dto.StaffMemberResponse;
import com.discoverdev.nms.admin.dto.StaffRoleOption;
import com.discoverdev.nms.admin.dto.UpdateStaffRequest;
import com.discoverdev.nms.auth.Invite;
import com.discoverdev.nms.auth.InviteRepository;
import com.discoverdev.nms.nursery.TeacherRoomAssignmentRepository;
import com.discoverdev.nms.tenant.TenantContext;
import com.discoverdev.nms.user.Role;
import com.discoverdev.nms.user.StaffRole;
import com.discoverdev.nms.user.User;
import com.discoverdev.nms.user.UserRepository;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AdminStaffService {

    private final UserRepository userRepository;
    private final InviteRepository inviteRepository;
    private final TeacherRoomAssignmentRepository assignmentRepository;

    public AdminStaffService(
        UserRepository userRepository,
        InviteRepository inviteRepository,
        TeacherRoomAssignmentRepository assignmentRepository
    ) {
        this.userRepository = userRepository;
        this.inviteRepository = inviteRepository;
        this.assignmentRepository = assignmentRepository;
    }

    /** All (non-deleted) teachers in the nursery with their invitation status. */
    @Transactional(readOnly = true)
    public List<StaffMemberResponse> listStaff() {
        UUID nurseryId = TenantContext.currentNurseryId();
        return userRepository
            .findByNurseryIdAndRoleAndDeletedAtIsNullOrderByFullNameAsc(nurseryId, Role.TEACHER)
            .stream()
            .map(this::toResponse)
            .toList();
    }

    /** A single teacher's details. */
    @Transactional(readOnly = true)
    public StaffMemberResponse getStaff(UUID id) {
        return toResponse(loadTeacher(id));
    }

    /** Update a teacher's own fields (name / email / role). */
    @Transactional
    public StaffMemberResponse update(UUID id, UpdateStaffRequest req) {
        User t = loadTeacher(id);
        if (req.fullName() != null) {
            t.setFullName(req.fullName());
        }
        if (req.email() != null && !req.email().equalsIgnoreCase(t.getEmail())) {
            if (userRepository.existsByEmailIgnoreCase(req.email())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already in use");
            }
            t.setEmail(req.email());
        }
        if (req.staffRole() != null) {
            try {
                t.setStaffRole(StaffRole.valueOf(req.staffRole()));
            } catch (IllegalArgumentException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid role");
            }
        }
        return toResponse(userRepository.save(t));
    }

    /**
     * Remove a teacher from the nursery: unassign from every room and soft-delete
     * the account (revokes login, hidden from all lists). Records they authored
     * (logs, incidents, sessions, chat) are preserved for integrity.
     */
    @Transactional
    public void delete(UUID id) {
        User t = loadTeacher(id);
        assignmentRepository.deleteByTeacherId(t.getId()); // out of every room
        t.setEnabled(false);
        t.setDeletedAt(Instant.now());
        userRepository.save(t);
    }

    /** The staff-role options for the "add member" dropdown. */
    @Transactional(readOnly = true)
    public List<StaffRoleOption> listRoles() {
        return Arrays.stream(StaffRole.values()).map(StaffRoleOption::of).toList();
    }

    private StaffMemberResponse toResponse(User t) {
        return new StaffMemberResponse(
            t.getId(),
            t.getFullName(),
            t.getEmail(),
            t.getRole(),
            t.getStaffRole() != null ? t.getStaffRole().name() : null,
            t.getStaffRole() != null ? t.getStaffRole().getLabel() : null,
            t.getImageUrl(),
            statusOf(t),
            t.getCreatedAt());
    }

    private User loadTeacher(UUID id) {
        UUID nurseryId = TenantContext.currentNurseryId();
        User u = userRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Teacher not found"));
        if (u.getNursery() == null
            || !nurseryId.equals(u.getNursery().getId())
            || u.getRole() != Role.TEACHER
            || u.getDeletedAt() != null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Teacher not found");
        }
        return u;
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
