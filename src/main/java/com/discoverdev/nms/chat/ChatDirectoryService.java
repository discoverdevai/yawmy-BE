package com.discoverdev.nms.chat;

import com.discoverdev.nms.chat.dto.ChatDirectoryEntry;
import com.discoverdev.nms.chat.dto.MessageResponse;
import com.discoverdev.nms.child.Child;
import com.discoverdev.nms.child.ChildRepository;
import com.discoverdev.nms.child.EnrollmentStatus;
import com.discoverdev.nms.child.GuardianLink;
import com.discoverdev.nms.child.GuardianLinkRepository;
import com.discoverdev.nms.nursery.Room;
import com.discoverdev.nms.nursery.RoomRepository;
import com.discoverdev.nms.nursery.TeacherRoomAssignmentRepository;
import com.discoverdev.nms.user.Role;
import com.discoverdev.nms.user.User;
import com.discoverdev.nms.user.UserRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Builds a user's chat list from the nursery's current shape rather than from
 * the threads that happen to exist.
 *
 * The alternative — a stored membership row per allowed pair — has to be
 * reconciled on every enrolment, room move, teacher assignment and account
 * change, and is wrong in exactly the ways nobody notices: a parent who still
 * sees last term's teacher. Deriving means a child changing room changes the
 * chat list on the next fetch, with nothing to keep in step.
 *
 * Threads are still real, and still lazily created on first use. Where one
 * already exists, its preview and unread count are folded into the row.
 */
@Service
public class ChatDirectoryService {

    private static final String NURSERY_THREAD_TITLE = "Nursery Admin";

    private final UserRepository userRepository;
    private final RoomRepository roomRepository;
    private final ChildRepository childRepository;
    private final GuardianLinkRepository guardianLinkRepository;
    private final TeacherRoomAssignmentRepository teacherRoomRepository;
    private final ChatThreadRepository threadRepository;
    private final ChatMessageRepository messageRepository;
    private final ChatParticipantRepository participantRepository;
    private final ChatThreadReadRepository readRepository;

    public ChatDirectoryService(
        UserRepository userRepository,
        RoomRepository roomRepository,
        ChildRepository childRepository,
        GuardianLinkRepository guardianLinkRepository,
        TeacherRoomAssignmentRepository teacherRoomRepository,
        ChatThreadRepository threadRepository,
        ChatMessageRepository messageRepository,
        ChatParticipantRepository participantRepository,
        ChatThreadReadRepository readRepository
    ) {
        this.userRepository = userRepository;
        this.roomRepository = roomRepository;
        this.childRepository = childRepository;
        this.guardianLinkRepository = guardianLinkRepository;
        this.teacherRoomRepository = teacherRoomRepository;
        this.threadRepository = threadRepository;
        this.messageRepository = messageRepository;
        this.participantRepository = participantRepository;
        this.readRepository = readRepository;
    }

    /**
     * @param roomId  teacher context — which of their rooms is open. Null before
     *                one is picked, or for a teacher with no rooms at all.
     * @param childId parent context — which of their children is selected.
     */
    @Transactional(readOnly = true)
    public List<ChatDirectoryEntry> list(User me, UUID roomId, UUID childId) {
        if (me.getNursery() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                "User is not attached to a nursery");
        }
        List<ChatDirectoryEntry> rows = switch (me.getRole()) {
            case ADMIN -> forAdmin(me);
            case TEACHER -> forTeacher(me, roomId);
            case PARENT -> forParent(me, childId);
            case SUPER_ADMIN -> List.of();
        };
        return sort(rows.stream().map(row -> attachThread(me, row)).toList());
    }

    // ---- per-role directories ------------------------------------------------

    /** Admins see the whole nursery: every teacher, every parent, every room. */
    private List<ChatDirectoryEntry> forAdmin(User me) {
        UUID nurseryId = me.getNursery().getId();
        List<ChatDirectoryEntry> rows = new ArrayList<>();

        rows.add(staffGroupRow());

        for (User teacher : userRepository
            .findByNurseryIdAndRoleAndEnabledTrueAndDeletedAtIsNullOrderByFullNameAsc(
                nurseryId, Role.TEACHER)) {
            rows.add(ChatDirectoryEntry.pending(
                ChatThreadType.ADMIN_SUPPORT, teacher.getId(), Role.TEACHER.name(), null,
                teacher.getFullName(), staffSubtitle(teacher), userAvatar(teacher)));
        }

        for (Map.Entry<User, List<Child>> entry : enrolledParentsOf(nurseryId).entrySet()) {
            User parent = entry.getKey();
            rows.add(ChatDirectoryEntry.pending(
                ChatThreadType.ADMIN_SUPPORT, parent.getId(), Role.PARENT.name(), null,
                parent.getFullName(), parentSubtitle(entry.getValue()), userAvatar(parent)));
        }

        for (Room room : roomRepository.findByNurseryIdAndArchivedAtIsNullOrderByNameAsc(nurseryId)) {
            rows.add(roomGroupRow(room));
        }
        return rows;
    }

    /**
     * A teacher sees the nursery, the staff room, and their colleagues — plus,
     * once a room is selected, that room's group and the parents of the children
     * currently in it. With no room selected there is simply nothing room-scoped
     * to show, which is also the state a newly hired teacher starts in.
     */
    private List<ChatDirectoryEntry> forTeacher(User me, UUID roomId) {
        UUID nurseryId = me.getNursery().getId();
        List<ChatDirectoryEntry> rows = new ArrayList<>();

        rows.add(nurseryRow());
        rows.add(staffGroupRow());

        for (User colleague : userRepository
            .findByNurseryIdAndRoleAndEnabledTrueAndDeletedAtIsNullOrderByFullNameAsc(
                nurseryId, Role.TEACHER)) {
            if (colleague.getId().equals(me.getId())) continue;
            rows.add(ChatDirectoryEntry.pending(
                ChatThreadType.DIRECT, colleague.getId(), Role.TEACHER.name(), null,
                colleague.getFullName(), staffSubtitle(colleague), userAvatar(colleague)));
        }

        if (roomId == null) return rows;

        Room room = roomRepository.findByIdAndNurseryId(roomId, nurseryId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found"));
        // Asking about a room they do not teach is indistinguishable from asking
        // about a room in another nursery, and gets the same answer.
        if (!teacherRoomRepository.existsByTeacherIdAndRoomId(me.getId(), roomId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found");
        }

        rows.add(roomGroupRow(room));
        for (Map.Entry<User, List<Child>> entry : guardiansOfRoom(nurseryId, roomId).entrySet()) {
            User parent = entry.getKey();
            rows.add(ChatDirectoryEntry.pending(
                ChatThreadType.DIRECT, parent.getId(), Role.PARENT.name(), null,
                parent.getFullName(), parentSubtitle(entry.getValue()), userAvatar(parent)));
        }
        return rows;
    }

    /**
     * A parent sees the nursery, and — for the child they have selected — that
     * child's room group and the teachers who actually teach it. A child not yet
     * placed in a room leaves only the nursery thread, which is the whole list a
     * newly enrolled family starts with.
     */
    private List<ChatDirectoryEntry> forParent(User me, UUID childId) {
        UUID nurseryId = me.getNursery().getId();
        List<ChatDirectoryEntry> rows = new ArrayList<>();

        rows.add(nurseryRow());

        if (childId == null) return rows;

        Child child = guardianLinkRepository.findByGuardianIdOrderByCreatedAtAsc(me.getId()).stream()
            .map(GuardianLink::getChild)
            .filter(c -> c.getId().equals(childId))
            .findFirst()
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Child not found"));

        if (child.getEnrollmentStatus() != EnrollmentStatus.ENROLLED) return rows;

        Room room = child.getRoom();
        if (room == null || room.getArchivedAt() != null) return rows;

        rows.add(roomGroupRow(room));
        teacherRoomRepository.findByRoomIdOrderByCreatedAtAsc(room.getId()).stream()
            .map(a -> a.getTeacher())
            .filter(t -> t.isEnabled() && t.getDeletedAt() == null)
            .sorted(Comparator.comparing(User::getFullName, String.CASE_INSENSITIVE_ORDER))
            .forEach(teacher -> rows.add(ChatDirectoryEntry.pending(
                ChatThreadType.DIRECT, teacher.getId(), Role.TEACHER.name(), null,
                teacher.getFullName(), staffSubtitle(teacher), userAvatar(teacher))));

        // Guard against the same nursery id appearing twice for a parent whose
        // second child sits in the same room.
        return dedupe(rows);
    }

    // ---- row builders --------------------------------------------------------

    /** The caller's own thread with the nursery's admins. */
    private ChatDirectoryEntry nurseryRow() {
        return ChatDirectoryEntry.pending(
            ChatThreadType.ADMIN_SUPPORT, null, Role.ADMIN.name(), null,
            NURSERY_THREAD_TITLE, "Nursery office", null);
    }

    private ChatDirectoryEntry staffGroupRow() {
        return ChatDirectoryEntry.pending(
            ChatThreadType.STAFF_GROUP, null, null, null,
            "Staff Chat", "All teachers and admins", null);
    }

    private ChatDirectoryEntry roomGroupRow(Room room) {
        return ChatDirectoryEntry.pending(
            ChatThreadType.ROOM_GROUP, null, null, room.getId(),
            room.getName() + " Group Chat", "Teachers, parents and admins",
            room.getImageKey() != null ? "/api/files/rooms/" + room.getId() + "/image" : null);
    }

    // ---- thread lookup -------------------------------------------------------

    /**
     * Folds an existing conversation's preview and counters into a derived row.
     * The thread is found by the same deterministic key that created it, so no
     * mapping table is needed to connect a contact to their conversation — and a
     * contact who has never been messaged simply finds nothing and stays pending.
     */
    private ChatDirectoryEntry attachThread(User me, ChatDirectoryEntry row) {
        UUID nurseryId = me.getNursery().getId();
        ChatThread thread = switch (row.kind()) {
            // peerUserId null: the caller's own thread with the nursery.
            // peerUserId set: an admin looking at that member's nursery thread.
            case ADMIN_SUPPORT -> threadRepository.findByNurseryIdAndDirectKey(
                nurseryId,
                ChatThread.buildSupportKey(row.peerUserId() != null ? row.peerUserId() : me.getId())
            ).orElse(null);
            case DIRECT -> threadRepository.findByNurseryIdAndDirectKey(
                nurseryId, ChatThread.buildDirectKey(me.getId(), row.peerUserId())
            ).orElse(null);
            case ROOM_GROUP -> threadRepository.findByNurseryIdAndRoomIdAndType(
                nurseryId, row.roomId(), ChatThreadType.ROOM_GROUP
            ).orElse(null);
            case STAFF_GROUP -> threadRepository.findByNurseryIdAndType(
                nurseryId, ChatThreadType.STAFF_GROUP
            ).orElse(null);
        };
        if (thread == null) return row;

        ChatMessage last = messageRepository
            .findTopByThreadIdAndDeletedAtIsNullOrderByCreatedAtDesc(thread.getId())
            .orElse(null);
        ChatThreadRead read = readRepository
            .findByThreadIdAndUserId(thread.getId(), me.getId()).orElse(null);
        Instant lastReadAt = read != null ? read.getLastReadAt() : Instant.EPOCH;
        long unread = messageRepository
            .countByThreadIdAndCreatedAtGreaterThanAndSenderIdNotAndDeletedAtIsNull(
                thread.getId(), lastReadAt, me.getId());
        boolean muted = participantRepository
            .findByThreadIdAndUserId(thread.getId(), me.getId())
            .map(ChatParticipant::isMuted)
            .orElse(false);

        return row.withThread(
            thread.getId(),
            previewWithSender(last, me),
            thread.getLastMessageAt(),
            unread,
            muted
        );
    }

    /**
     * "You: see you at pickup" / "Ms. Farida: Voice message".
     *
     * Naming the sender is what makes a group's last line readable — otherwise a
     * room chat shows a sentence with no clue who said it. A one-to-one thread
     * needs the name only for your own messages: the row is already titled with
     * the other person, so repeating them on every line is noise.
     */
    private String previewWithSender(ChatMessage last, User me) {
        String body = MessageResponse.previewOf(last, 100);
        if (last == null || body == null) return body;
        boolean mine = last.getSender().getId().equals(me.getId());
        boolean group = last.getThread().getType() != ChatThreadType.DIRECT;
        if (mine) return "You: " + body;
        if (!group) return body;
        return firstName(last.getSender().getFullName()) + ": " + body;
    }

    /** A list row has no space for "Abdelrahman Mohamed Hassan" plus a message. */
    private String firstName(String fullName) {
        if (fullName == null || fullName.isBlank()) return "Someone";
        int space = fullName.trim().indexOf(' ');
        return space > 0 ? fullName.trim().substring(0, space) : fullName.trim();
    }

    private List<ChatDirectoryEntry> dedupe(List<ChatDirectoryEntry> rows) {
        Map<String, ChatDirectoryEntry> byKey = new LinkedHashMap<>();
        for (ChatDirectoryEntry row : rows) {
            String key = row.kind() + "|" + row.peerUserId() + "|" + row.roomId();
            byKey.putIfAbsent(key, row);
        }
        return new ArrayList<>(byKey.values());
    }

    /** Most recent conversation first; contacts never messaged fall to the bottom, by name. */
    private List<ChatDirectoryEntry> sort(List<ChatDirectoryEntry> rows) {
        List<ChatDirectoryEntry> sorted = new ArrayList<>(rows);
        sorted.sort((a, b) -> {
            Instant left = a.lastMessageAt();
            Instant right = b.lastMessageAt();
            if (left != null && right != null) return right.compareTo(left);
            if (left != null) return -1;
            if (right != null) return 1;
            return String.CASE_INSENSITIVE_ORDER.compare(a.title(), b.title());
        });
        return sorted;
    }

    // ---- lookups -------------------------------------------------------------

    /** Every parent with at least one enrolled child, mapped to those children. */
    private Map<User, List<Child>> enrolledParentsOf(UUID nurseryId) {
        Map<User, List<Child>> byParent = new LinkedHashMap<>();
        for (Child child : childRepository.findByNurseryIdAndEnrollmentStatusOrderByFullNameAsc(
            nurseryId, EnrollmentStatus.ENROLLED)) {
            addGuardians(byParent, child);
        }
        return sortedByName(byParent);
    }

    /** Guardians of the children currently enrolled in one room. */
    private Map<User, List<Child>> guardiansOfRoom(UUID nurseryId, UUID roomId) {
        Map<User, List<Child>> byParent = new LinkedHashMap<>();
        for (Child child : childRepository.findByNurseryIdAndRoomIdAndEnrollmentStatusOrderByFullNameAsc(
            nurseryId, roomId, EnrollmentStatus.ENROLLED)) {
            addGuardians(byParent, child);
        }
        return sortedByName(byParent);
    }

    private void addGuardians(Map<User, List<Child>> byParent, Child child) {
        for (GuardianLink link : guardianLinkRepository.findByChildIdOrderByCreatedAtAsc(child.getId())) {
            User guardian = link.getGuardian();
            if (!guardian.isEnabled() || guardian.getDeletedAt() != null) continue;
            byParent.computeIfAbsent(guardian, k -> new ArrayList<>()).add(child);
        }
    }

    private Map<User, List<Child>> sortedByName(Map<User, List<Child>> byParent) {
        Map<User, List<Child>> sorted = new LinkedHashMap<>();
        byParent.entrySet().stream()
            .sorted(Comparator.comparing(e -> e.getKey().getFullName(), String.CASE_INSENSITIVE_ORDER))
            .forEach(e -> sorted.put(e.getKey(), e.getValue()));
        return sorted;
    }

    // ---- labels --------------------------------------------------------------

    private String staffSubtitle(User teacher) {
        return teacher.getStaffRole() != null ? teacher.getStaffRole().getLabel() : "Teacher";
    }

    /** "Parent of Amina" / "Parent of Amina, Yusuf" — which family this is. */
    private String parentSubtitle(List<Child> children) {
        if (children == null || children.isEmpty()) return "Parent";
        String names = children.stream()
            .map(Child::getFullName)
            .distinct()
            .reduce((a, b) -> a + ", " + b)
            .orElse("");
        return names.isBlank() ? "Parent" : "Parent of " + names;
    }

    private String userAvatar(User user) {
        return user.getImageKey() != null ? "/api/files/users/" + user.getId() + "/image" : null;
    }
}
