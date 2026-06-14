package com.discoverdev.nms.chat;

import com.discoverdev.nms.child.Child;
import com.discoverdev.nms.child.ChildRepository;
import com.discoverdev.nms.child.EnrollmentStatus;
import com.discoverdev.nms.child.GuardianLink;
import com.discoverdev.nms.child.GuardianLinkRepository;
import com.discoverdev.nms.nursery.Nursery;
import com.discoverdev.nms.nursery.NurseryRepository;
import com.discoverdev.nms.nursery.Room;
import com.discoverdev.nms.nursery.RoomRepository;
import com.discoverdev.nms.nursery.TeacherRoomAssignmentRepository;
import com.discoverdev.nms.user.Role;
import com.discoverdev.nms.user.User;
import com.discoverdev.nms.user.UserRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ChatAccessService {

    private final ChatThreadRepository threadRepository;
    private final ChatParticipantRepository participantRepository;
    private final UserRepository userRepository;
    private final NurseryRepository nurseryRepository;
    private final RoomRepository roomRepository;
    private final ChildRepository childRepository;
    private final GuardianLinkRepository guardianLinkRepository;
    private final TeacherRoomAssignmentRepository teacherRoomRepository;

    public ChatAccessService(
        ChatThreadRepository threadRepository,
        ChatParticipantRepository participantRepository,
        UserRepository userRepository,
        NurseryRepository nurseryRepository,
        RoomRepository roomRepository,
        ChildRepository childRepository,
        GuardianLinkRepository guardianLinkRepository,
        TeacherRoomAssignmentRepository teacherRoomRepository
    ) {
        this.threadRepository = threadRepository;
        this.participantRepository = participantRepository;
        this.userRepository = userRepository;
        this.nurseryRepository = nurseryRepository;
        this.roomRepository = roomRepository;
        this.childRepository = childRepository;
        this.guardianLinkRepository = guardianLinkRepository;
        this.teacherRoomRepository = teacherRoomRepository;
    }

    @Transactional
    public ChatThread openDirectAsParent(User parent, UUID otherUserId) {
        if (parent.getRole() != Role.PARENT) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only parents may call this");
        }
        User other = loadSameNursery(parent, otherUserId);
        if (other.getRole() == Role.PARENT || other.getRole() == Role.SUPER_ADMIN) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Parents may only DM teachers or admins in their nursery");
        }
        if (other.getRole() == Role.TEACHER) {
            Set<UUID> teacherRooms = roomsTaughtBy(other);
            Set<UUID> parentRooms = enrolledRoomIdsOf(parent);
            if (java.util.Collections.disjoint(teacherRooms, parentRooms)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Teacher does not teach any of your children's rooms");
            }
        }
        return openOrCreateDirect(parent, other);
    }

    @Transactional
    public ChatThread openDirectAsTeacher(User teacher, UUID otherUserId) {
        if (teacher.getRole() != Role.TEACHER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only teachers may call this");
        }
        User other = loadSameNursery(teacher, otherUserId);
        if (other.getRole() == Role.PARENT) {
            Set<UUID> teacherRooms = roomsTaughtBy(teacher);
            Set<UUID> parentRooms = enrolledRoomIdsOf(other);
            if (java.util.Collections.disjoint(teacherRooms, parentRooms)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "This parent has no child in any of your assigned rooms");
            }
        } else if (other.getRole() == Role.SUPER_ADMIN) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Cannot DM super admin");
        }
        return openOrCreateDirect(teacher, other);
    }

    @Transactional
    public ChatThread openDirectAsAdmin(User admin, UUID otherUserId) {
        if (admin.getRole() != Role.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only admins may call this");
        }
        User other = loadSameNursery(admin, otherUserId);
        if (other.getRole() == Role.SUPER_ADMIN) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot DM super admin");
        }
        return openOrCreateDirect(admin, other);
    }

    @Transactional
    public ChatThread openRoomGroupAsTeacher(User teacher, UUID roomId) {
        if (teacher.getRole() != Role.TEACHER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only teachers may open a room group");
        }
        UUID nurseryId = teacher.getNursery().getId();
        Room room = roomRepository.findByIdAndNurseryId(roomId, nurseryId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found"));
        if (room.getArchivedAt() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot open chat in archived room");
        }
        if (!teacherRoomRepository.existsByTeacherIdAndRoomId(teacher.getId(), roomId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found");
        }
        ChatThread existing = threadRepository
            .findByNurseryIdAndRoomIdAndType(nurseryId, roomId, ChatThreadType.ROOM_GROUP)
            .orElse(null);
        Nursery nursery = nurseryRepository.findById(nurseryId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Nursery not found"));
        if (existing == null) {
            existing = threadRepository.save(ChatThread.builder()
                .nursery(nursery)
                .type(ChatThreadType.ROOM_GROUP)
                .room(room)
                .createdBy(teacher)
                .build());
        }
        syncRoomGroupParticipants(existing, room, nursery);
        return existing;
    }

    @Transactional
    public ChatThread openStaffGroup(User actor) {
        if (actor.getRole() != Role.TEACHER && actor.getRole() != Role.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only staff may open staff chat");
        }
        UUID nurseryId = actor.getNursery().getId();
        ChatThread existing = threadRepository
            .findByNurseryIdAndType(nurseryId, ChatThreadType.STAFF_GROUP).orElse(null);
        Nursery nursery = nurseryRepository.findById(nurseryId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Nursery not found"));
        if (existing == null) {
            existing = threadRepository.save(ChatThread.builder()
                .nursery(nursery)
                .type(ChatThreadType.STAFF_GROUP)
                .createdBy(actor)
                .build());
        }
        syncStaffGroupParticipants(existing, nursery);
        return existing;
    }

    private ChatThread openOrCreateDirect(User a, User b) {
        if (a.getId().equals(b.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot open DM with self");
        }
        UUID nurseryId = a.getNursery().getId();
        String key = ChatThread.buildDirectKey(a.getId(), b.getId());
        ChatThread existing = threadRepository.findByNurseryIdAndDirectKey(nurseryId, key).orElse(null);
        Nursery nursery = nurseryRepository.findById(nurseryId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Nursery not found"));
        if (existing == null) {
            existing = threadRepository.save(ChatThread.builder()
                .nursery(nursery)
                .type(ChatThreadType.DIRECT)
                .directKey(key)
                .createdBy(a)
                .build());
            ensureActiveParticipant(existing, a);
            ensureActiveParticipant(existing, b);
        } else {
            // Reactivate if either party had left
            reactivateParticipant(existing, a);
            reactivateParticipant(existing, b);
        }
        return existing;
    }

    private void syncRoomGroupParticipants(ChatThread thread, Room room, Nursery nursery) {
        // Teachers assigned to this room
        teacherRoomRepository.findByRoomIdOrderByCreatedAtAsc(room.getId()).forEach(a ->
            ensureActiveParticipant(thread, a.getTeacher()));
        // Parents of enrolled children in this room
        childRepository.findByNurseryIdAndRoomIdAndEnrollmentStatusOrderByFullNameAsc(
                nursery.getId(), room.getId(), EnrollmentStatus.ENROLLED).forEach(child ->
            guardianLinkRepository.findByChildIdOrderByCreatedAtAsc(child.getId()).forEach(link ->
                ensureActiveParticipant(thread, link.getGuardian())));
    }

    private void syncStaffGroupParticipants(ChatThread thread, Nursery nursery) {
        userRepository.findByNurseryIdAndRoleAndEnabledTrue(nursery.getId(), Role.TEACHER)
            .forEach(u -> ensureActiveParticipant(thread, u));
        userRepository.findByNurseryIdAndRoleAndEnabledTrue(nursery.getId(), Role.ADMIN)
            .forEach(u -> ensureActiveParticipant(thread, u));
    }

    private void ensureActiveParticipant(ChatThread thread, User user) {
        ChatParticipant p = participantRepository
            .findByThreadIdAndUserId(thread.getId(), user.getId()).orElse(null);
        if (p == null) {
            participantRepository.save(ChatParticipant.builder()
                .thread(thread).user(user).build());
        } else if (p.getLeftAt() != null) {
            p.setLeftAt(null);
            participantRepository.save(p);
        }
    }

    private void reactivateParticipant(ChatThread thread, User user) {
        ChatParticipant p = participantRepository
            .findByThreadIdAndUserId(thread.getId(), user.getId()).orElse(null);
        if (p == null) {
            participantRepository.save(ChatParticipant.builder()
                .thread(thread).user(user).build());
        } else if (p.getLeftAt() != null) {
            p.setLeftAt(null);
            participantRepository.save(p);
        }
    }

    private User loadSameNursery(User actor, UUID otherUserId) {
        if (actor.getNursery() == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }
        User other = userRepository.findById(otherUserId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        if (other.getNursery() == null
            || !other.getNursery().getId().equals(actor.getNursery().getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }
        if (!other.isEnabled()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }
        return other;
    }

    private Set<UUID> roomsTaughtBy(User teacher) {
        Set<UUID> out = new HashSet<>();
        teacherRoomRepository.findByTeacherIdOrderByCreatedAtAsc(teacher.getId())
            .forEach(a -> out.add(a.getRoom().getId()));
        return out;
    }

    private Set<UUID> enrolledRoomIdsOf(User parent) {
        Set<UUID> out = new HashSet<>();
        for (GuardianLink link : guardianLinkRepository.findByGuardianIdOrderByCreatedAtAsc(parent.getId())) {
            Child child = link.getChild();
            if (child.getEnrollmentStatus() == EnrollmentStatus.ENROLLED && child.getRoom() != null) {
                out.add(child.getRoom().getId());
            }
        }
        return out;
    }

    /**
     * Revoke chat access for a parent who no longer has any enrolled children in the nursery.
     * Sets leftAt on every active participant row this parent has in this nursery.
     */
    @Transactional
    public void revokeParentIfNoEnrolledChildren(User parent) {
        if (parent.getRole() != Role.PARENT) return;
        boolean hasEnrolled = guardianLinkRepository.findByGuardianIdOrderByCreatedAtAsc(parent.getId())
            .stream()
            .anyMatch(l -> l.getChild().getEnrollmentStatus() == EnrollmentStatus.ENROLLED);
        if (hasEnrolled) return;
        UUID nurseryId = parent.getNursery() != null ? parent.getNursery().getId() : null;
        if (nurseryId == null) return;
        List<ChatParticipant> active = participantRepository
            .findActiveByUserAndNursery(parent.getId(), nurseryId);
        java.time.Instant now = java.time.Instant.now();
        for (ChatParticipant p : active) {
            p.setLeftAt(now);
        }
        participantRepository.saveAll(active);
    }
}
