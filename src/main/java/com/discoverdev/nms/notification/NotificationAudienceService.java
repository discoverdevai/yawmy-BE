package com.discoverdev.nms.notification;

import com.discoverdev.nms.child.Child;
import com.discoverdev.nms.child.ChildRepository;
import com.discoverdev.nms.child.EnrollmentStatus;
import com.discoverdev.nms.child.GuardianLink;
import com.discoverdev.nms.child.GuardianLinkRepository;
import com.discoverdev.nms.nursery.TeacherRoomAssignment;
import com.discoverdev.nms.nursery.TeacherRoomAssignmentRepository;
import com.discoverdev.nms.user.Role;
import com.discoverdev.nms.user.User;
import com.discoverdev.nms.user.UserRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationAudienceService {

    private final GuardianLinkRepository guardianLinkRepository;
    private final ChildRepository childRepository;
    private final UserRepository userRepository;
    private final TeacherRoomAssignmentRepository assignmentRepository;

    public NotificationAudienceService(
        GuardianLinkRepository guardianLinkRepository,
        ChildRepository childRepository,
        UserRepository userRepository,
        TeacherRoomAssignmentRepository assignmentRepository
    ) {
        this.guardianLinkRepository = guardianLinkRepository;
        this.childRepository = childRepository;
        this.userRepository = userRepository;
        this.assignmentRepository = assignmentRepository;
    }

    @Transactional(readOnly = true)
    public List<User> guardiansOfChild(UUID childId, boolean enrolledOnly) {
        return guardianLinkRepository.findByChildIdOrderByCreatedAtAsc(childId).stream()
            .filter(link -> !enrolledOnly || link.getChild().getEnrollmentStatus() == EnrollmentStatus.ENROLLED)
            .map(GuardianLink::getGuardian)
            .filter(User::isEnabled)
            .collect(distinctById());
    }

    @Transactional(readOnly = true)
    public List<User> parentsInRoom(UUID nurseryId, UUID roomId) {
        List<Child> children = childRepository
            .findByNurseryIdAndRoomIdAndEnrollmentStatusOrderByFullNameAsc(
                nurseryId, roomId, EnrollmentStatus.ENROLLED);
        return children.stream()
            .flatMap(c -> guardianLinkRepository.findByChildIdOrderByCreatedAtAsc(c.getId()).stream())
            .map(GuardianLink::getGuardian)
            .filter(User::isEnabled)
            .collect(distinctById());
    }

    @Transactional(readOnly = true)
    public List<User> adminsOfNursery(UUID nurseryId) {
        return userRepository.findByNurseryIdAndRoleAndEnabledTrue(nurseryId, Role.ADMIN);
    }

    /**
     * The teachers assigned to a room — the room-scoped counterpart of
     * {@link #parentsInRoom}. Use it for anything that concerns the room as a
     * whole: an announcement, an event, a session change.
     *
     * Skips disabled and soft-deleted staff, so a teacher removed from the
     * nursery stops receiving its notifications immediately. Deduped, because a
     * teacher could in principle hold two assignment rows for one room.
     */
    @Transactional(readOnly = true)
    public List<User> teachersOfRoom(UUID roomId) {
        return assignmentRepository.findByRoomIdOrderByCreatedAtAsc(roomId).stream()
            .map(TeacherRoomAssignment::getTeacher)
            .filter(t -> t.isEnabled() && t.getDeletedAt() == null)
            .collect(distinctById());
    }

    /**
     * Every teacher in the nursery, **including those not yet assigned to a
     * room** — a newly activated teacher still needs to hear nursery-wide news.
     */
    @Transactional(readOnly = true)
    public List<User> allTeachersOfNursery(UUID nurseryId) {
        return userRepository
            .findByNurseryIdAndRoleAndEnabledTrueAndDeletedAtIsNullOrderByFullNameAsc(
                nurseryId, Role.TEACHER);
    }

    /** Every parent in the nursery, whatever room their children are in. */
    @Transactional(readOnly = true)
    public List<User> allParentsOfNursery(UUID nurseryId) {
        return userRepository.findByNurseryIdAndRoleAndEnabledTrue(nurseryId, Role.PARENT);
    }

    /**
     * The teachers responsible for one child — the teachers of the room that
     * child is in. The counterpart of {@link #guardiansOfChild}: use it for
     * anything about a single child, such as an incident or a check-in.
     *
     * Empty when the child has no room assigned yet, which is a normal state.
     */
    @Transactional(readOnly = true)
    public List<User> teachersOfChild(UUID childId) {
        Child child = childRepository.findById(childId).orElse(null);
        if (child == null || child.getRoom() == null) return List.of();
        return teachersOfRoom(child.getRoom().getId());
    }

    private static java.util.stream.Collector<User, ?, List<User>> distinctById() {
        return java.util.stream.Collectors.collectingAndThen(
            java.util.stream.Collectors.toMap(
                User::getId,
                u -> u,
                (a, b) -> a,
                LinkedHashMap::new),
            m -> List.copyOf(m.values()));
    }
}
