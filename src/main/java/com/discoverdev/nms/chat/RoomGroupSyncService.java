package com.discoverdev.nms.chat;

import com.discoverdev.nms.child.Child;
import com.discoverdev.nms.child.ChildRepository;
import com.discoverdev.nms.child.EnrollmentStatus;
import com.discoverdev.nms.child.GuardianLink;
import com.discoverdev.nms.child.GuardianLinkRepository;
import com.discoverdev.nms.nursery.Room;
import com.discoverdev.nms.nursery.RoomRepository;
import com.discoverdev.nms.nursery.TeacherRoomAssignment;
import com.discoverdev.nms.nursery.TeacherRoomAssignmentRepository;
import com.discoverdev.nms.user.Role;
import com.discoverdev.nms.user.User;
import com.discoverdev.nms.user.UserRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Keeps a room's group chat membership equal to who currently belongs in the room.
 *
 * Deliberately a reconcile rather than a set of patches. "Child moved rooms" is
 * not one change — it can remove a parent from the old group unless their other
 * child is still there, add them to the new one, and do nothing at all if both
 * children were already in both rooms. Written as add/remove rules per case,
 * that grows a branch per scenario and the missed combinations are exactly the
 * ones nobody tests. Computing the whole set and diffing it has no cases.
 *
 * It also makes the operation idempotent: a reconcile that runs twice, or one
 * that runs after a missed event, converges to the same answer.
 */
@Service
public class RoomGroupSyncService {

    private static final Logger log = LoggerFactory.getLogger(RoomGroupSyncService.class);

    private final RoomRepository roomRepository;
    private final ChildRepository childRepository;
    private final GuardianLinkRepository guardianLinkRepository;
    private final TeacherRoomAssignmentRepository teacherRoomRepository;
    private final UserRepository userRepository;
    private final ChatThreadRepository threadRepository;
    private final ChatParticipantRepository participantRepository;

    public RoomGroupSyncService(
        RoomRepository roomRepository,
        ChildRepository childRepository,
        GuardianLinkRepository guardianLinkRepository,
        TeacherRoomAssignmentRepository teacherRoomRepository,
        UserRepository userRepository,
        ChatThreadRepository threadRepository,
        ChatParticipantRepository participantRepository
    ) {
        this.roomRepository = roomRepository;
        this.childRepository = childRepository;
        this.guardianLinkRepository = guardianLinkRepository;
        this.teacherRoomRepository = teacherRoomRepository;
        this.userRepository = userRepository;
        this.threadRepository = threadRepository;
        this.participantRepository = participantRepository;
    }

    /**
     * Bring one room's group chat in line with the room. Safe to call for a room
     * that has no group yet (nothing to do until someone opens it), for an
     * archived room (everyone leaves), and repeatedly.
     */
    @Transactional
    public void reconcile(UUID roomId) {
        if (roomId == null) return;
        Room room = roomRepository.findById(roomId).orElse(null);
        if (room == null) return;

        ChatThread thread = threadRepository.findByNurseryIdAndRoomIdAndType(
            room.getNursery().getId(), roomId, ChatThreadType.ROOM_GROUP).orElse(null);
        if (thread == null) {
            // Nothing to keep in step yet. The group is created with the room, or
            // on first access, and is correct from the moment it exists.
            return;
        }

        Set<UUID> shouldBeMembers = room.getArchivedAt() == null
            ? currentMembersOf(room)
            : Set.of();

        applyMembership(thread, shouldBeMembers);
    }

    /** Reconcile several rooms — a child moving leaves one group and joins another. */
    @Transactional
    public void reconcileAll(UUID... roomIds) {
        Set<UUID> distinct = new HashSet<>();
        for (UUID id : roomIds) {
            if (id != null) distinct.add(id);
        }
        distinct.forEach(this::reconcile);
    }

    /** Every room in the nursery — for changes that touch all of them, like a new admin. */
    @Transactional
    public void reconcileNursery(UUID nurseryId) {
        roomRepository.findByNurseryIdAndArchivedAtIsNullOrderByNameAsc(nurseryId)
            .forEach(room -> reconcile(room.getId()));
    }

    /**
     * Who belongs in this room's chat right now: every admin, the room's teachers,
     * and the guardians of the children currently enrolled in it. A parent with
     * two children in the same room appears once — it is a set.
     */
    private Set<UUID> currentMembersOf(Room room) {
        UUID nurseryId = room.getNursery().getId();
        Set<UUID> members = new HashSet<>();

        for (User admin : userRepository.findByNurseryIdAndRoleAndEnabledTrue(nurseryId, Role.ADMIN)) {
            members.add(admin.getId());
        }
        for (TeacherRoomAssignment assignment
            : teacherRoomRepository.findByRoomIdOrderByCreatedAtAsc(room.getId())) {
            User teacher = assignment.getTeacher();
            if (teacher.isEnabled() && teacher.getDeletedAt() == null) {
                members.add(teacher.getId());
            }
        }
        for (Child child : childRepository.findByNurseryIdAndRoomIdAndEnrollmentStatusOrderByFullNameAsc(
            nurseryId, room.getId(), EnrollmentStatus.ENROLLED)) {
            for (GuardianLink link : guardianLinkRepository.findByChildIdOrderByCreatedAtAsc(child.getId())) {
                User guardian = link.getGuardian();
                if (guardian.isEnabled() && guardian.getDeletedAt() == null) {
                    members.add(guardian.getId());
                }
            }
        }
        return members;
    }

    /**
     * Diff the desired set against the participant rows. Departures are marked
     * with leftAt rather than deleted, so the messages someone wrote while they
     * were in the room keep a valid author, and returning is just clearing it.
     */
    private void applyMembership(ChatThread thread, Set<UUID> shouldBeMembers) {
        List<ChatParticipant> existing = participantRepository.findByThreadId(thread.getId());
        Set<UUID> known = new HashSet<>();
        List<ChatParticipant> changed = new ArrayList<>();
        Instant now = Instant.now();

        for (ChatParticipant participant : existing) {
            UUID userId = participant.getUser().getId();
            known.add(userId);
            boolean shouldBeIn = shouldBeMembers.contains(userId);
            if (shouldBeIn && participant.getLeftAt() != null) {
                participant.setLeftAt(null);
                changed.add(participant);
            } else if (!shouldBeIn && participant.getLeftAt() == null) {
                participant.setLeftAt(now);
                changed.add(participant);
            }
        }

        for (UUID userId : shouldBeMembers) {
            if (known.contains(userId)) continue;
            userRepository.findById(userId).ifPresent(user ->
                changed.add(ChatParticipant.builder().thread(thread).user(user).build()));
        }

        if (!changed.isEmpty()) {
            participantRepository.saveAll(changed);
            log.debug("Room group {} membership reconciled — {} participant rows changed",
                thread.getId(), changed.size());
        }
    }
}
