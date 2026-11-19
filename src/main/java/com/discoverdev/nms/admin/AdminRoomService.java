package com.discoverdev.nms.admin;

import com.discoverdev.nms.admin.dto.ArchiveRoomRequest;
import com.discoverdev.nms.admin.dto.CreateRoomRequest;
import com.discoverdev.nms.admin.dto.RoomResponse;
import com.discoverdev.nms.admin.dto.UpdateRoomRequest;
import com.discoverdev.nms.chat.ChatAccessService;
import com.discoverdev.nms.chat.RoomMembershipChangedEvent;
import com.discoverdev.nms.child.Child;
import com.discoverdev.nms.child.ChildRepository;
import com.discoverdev.nms.child.EnrollmentStatus;
import com.discoverdev.nms.nursery.Nursery;
import com.discoverdev.nms.nursery.NurseryRepository;
import com.discoverdev.nms.nursery.Room;
import com.discoverdev.nms.nursery.RoomRepository;
import com.discoverdev.nms.nursery.TeacherRoomAssignmentRepository;
import com.discoverdev.nms.tenant.TenantContext;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AdminRoomService {

    // Default room capacity when the request doesn't specify one.
    private static final int DEFAULT_CAPACITY = 20;

    private final RoomRepository roomRepository;
    private final NurseryRepository nurseryRepository;
    private final ChildRepository childRepository;
    private final AdminTeacherAssignmentService teacherAssignmentService;
    private final ChatAccessService chatAccess;
    private final TeacherRoomAssignmentRepository teacherRoomRepository;
    private final ApplicationEventPublisher events;

    public AdminRoomService(
        RoomRepository roomRepository,
        NurseryRepository nurseryRepository,
        ChildRepository childRepository,
        AdminTeacherAssignmentService teacherAssignmentService,
        ChatAccessService chatAccess,
        TeacherRoomAssignmentRepository teacherRoomRepository,
        ApplicationEventPublisher events
    ) {
        this.roomRepository = roomRepository;
        this.nurseryRepository = nurseryRepository;
        this.childRepository = childRepository;
        this.teacherAssignmentService = teacherAssignmentService;
        this.chatAccess = chatAccess;
        this.teacherRoomRepository = teacherRoomRepository;
        this.events = events;
    }

    /** Room response with its current enrolled-child count. */
    private RoomResponse toResponse(Room room) {
        long count = childRepository.countByRoomIdAndEnrollmentStatus(
            room.getId(), EnrollmentStatus.ENROLLED);
        return RoomResponse.of(room, count);
    }

    @Transactional
    public RoomResponse create(CreateRoomRequest req) {
        UUID nurseryId = TenantContext.currentNurseryId();
        if (roomRepository.existsByNurseryIdAndNameIgnoreCaseAndArchivedAtIsNull(nurseryId, req.name())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A room with this name already exists");
        }
        Nursery nursery = nurseryRepository.getReferenceById(nurseryId);
        Room room = Room.builder()
            .nursery(nursery)
            .name(req.name())
            .capacity(req.capacity() != null ? req.capacity() : DEFAULT_CAPACITY)
            .ageMinMonths(req.ageMinMonths())
            .ageMaxMonths(req.ageMaxMonths())
            .build();
        checkAgeRange(room.getAgeMinMonths(), room.getAgeMaxMonths());
        List<Child> picked = req.childIds() == null ? List.of() : loadChildrenForRoom(req.childIds(), room);
        room = roomRepository.save(room);

        // Every room owns a group chat from the moment it exists, so it can be
        // listed (and joined by whoever belongs in it) before anyone has sent a
        // message. Without this the group only appears once a teacher opens it.
        chatAccess.ensureRoomGroup(room, TenantContext.currentUser());

        // Optionally assign a teacher picked in the "add room" modal.
        Set<UUID> teachers = new LinkedHashSet<>();
        if (req.teacherId() != null) teachers.add(req.teacherId());
        if (req.teacherIds() != null) teachers.addAll(req.teacherIds());
        for (UUID teacherId : teachers) {
            teacherAssignmentService.assign(room.getId(), teacherId);
        }
        // Jira YC-85: children can be picked while creating the room (capacity-limited).
        moveChildrenInto(room, picked);
        return toResponse(room);
    }

    @Transactional(readOnly = true)
    public List<RoomResponse> list(boolean includeArchived) {
        return list(includeArchived ? null : "active", null);
    }

    /**
     * Rooms A to Z. {@code status} is "active", "archived", or null for both;
     * {@code search} matches part of the room name, ignoring case.
     */
    @Transactional(readOnly = true)
    public List<RoomResponse> list(String status, String search) {
        UUID nurseryId = TenantContext.currentNurseryId();
        String needle = search == null || search.isBlank() ? null : search.trim().toLowerCase();
        List<Room> rooms = roomRepository.findByNurseryIdOrderByNameAsc(nurseryId).stream()
            .filter(r -> status == null
                || ("active".equalsIgnoreCase(status) && r.getArchivedAt() == null)
                || ("archived".equalsIgnoreCase(status) && r.getArchivedAt() != null))
            .filter(r -> needle == null || r.getName().toLowerCase().contains(needle))
            .toList();

        // One grouped query for all room child-counts (no N+1).
        Map<UUID, Long> counts = childRepository
            .countEnrolledGroupedByRoom(nurseryId, EnrollmentStatus.ENROLLED)
            .stream()
            .collect(Collectors.toMap(
                ChildRepository.RoomChildCount::getRoomId,
                ChildRepository.RoomChildCount::getCount));

        return rooms.stream()
            .map(r -> RoomResponse.of(r, counts.getOrDefault(r.getId(), 0L)))
            .toList();
    }

    @Transactional(readOnly = true)
    public RoomResponse get(UUID id) {
        return toResponse(loadOwned(id));
    }

    @Transactional
    public RoomResponse update(UUID id, UpdateRoomRequest req) {
        Room room = loadOwned(id);
        if (room.getArchivedAt() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot edit an archived room");
        }
        if (req.name() != null && !req.name().equalsIgnoreCase(room.getName())) {
            boolean clash = roomRepository
                .existsByNurseryIdAndNameIgnoreCaseAndArchivedAtIsNull(
                    TenantContext.currentNurseryId(), req.name());
            if (clash) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "A room with this name already exists");
            }
            room.setName(req.name());
        }
        if (req.capacity() != null) {
            long enrolled = childRepository.countByRoomIdAndEnrollmentStatus(room.getId(), EnrollmentStatus.ENROLLED);
            if (req.childIds() == null && req.capacity() < enrolled) {   // with childIds, the final list is checked below
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "This room has " + enrolled + " children, so its capacity can't be lower than " + enrolled);
            }
            room.setCapacity(req.capacity());
        }
        // Jira YC-86: age range, and the full lists of teachers and children.
        Integer min = req.ageMinMonths() != null ? req.ageMinMonths() : room.getAgeMinMonths();
        Integer max = req.ageMaxMonths() != null ? req.ageMaxMonths() : room.getAgeMaxMonths();
        checkAgeRange(min, max);
        room.setAgeMinMonths(min);
        room.setAgeMaxMonths(max);
        List<Child> current = childRepository.findByNurseryIdAndRoomIdAndEnrollmentStatusOrderByFullNameAsc(
            TenantContext.currentNurseryId(), room.getId(), EnrollmentStatus.ENROLLED);
        List<Child> wanted = req.childIds() == null ? current : loadChildrenForRoom(req.childIds(), room);
        if (wanted.size() > room.getCapacity()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Only " + room.getCapacity() + " children fit in this room");
        }
        for (Child c : wanted) {
            if (!fitsAgeRange(c, min, max)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                    c.getFullName() + " is outside this room's age range");
            }
        }
        room = roomRepository.save(room);
        if (req.childIds() != null) {
            Set<UUID> keep = new HashSet<>(req.childIds());
            List<Child> leaving = current.stream().filter(c -> !keep.contains(c.getId())).toList();
            leaving.forEach(c -> c.setRoom(null));   // still enrolled, just without a room
            childRepository.saveAll(leaving);
            moveChildrenInto(room, wanted);
        }
        if (req.teacherIds() != null) {
            Set<UUID> keepTeachers = new HashSet<>(req.teacherIds());
            Set<UUID> now = new HashSet<>();
            for (var a : teacherRoomRepository.findByRoomIdOrderByCreatedAtAsc(room.getId())) {
                UUID t = a.getTeacher().getId();
                now.add(t);
                if (!keepTeachers.contains(t)) teacherAssignmentService.unassign(room.getId(), t);
            }
            for (UUID t : keepTeachers) {
                if (!now.contains(t)) teacherAssignmentService.assign(room.getId(), t);
            }
        }
        return toResponse(room);
    }

    /**
     * Archives a room in one transaction: the given children lose their room (they stay
     * enrolled), the room's teachers are unassigned, and its group chat is emptied.
     */
    private void finishArchive(Room room, Collection<Child> unassign) {
        unassign.forEach(c -> c.setRoom(null));
        childRepository.saveAll(unassign);
        teacherRoomRepository.deleteByRoomId(room.getId());
        room.setArchivedAt(Instant.now());
        roomRepository.save(room);
        events.publishEvent(RoomMembershipChangedEvent.of(room.getId()));
    }

    @Transactional
    public RoomResponse archive(UUID id) {
        Room room = loadOwned(id);
        if (room.getArchivedAt() == null) {
            // "Archive Anyway": the room's children stay enrolled but no longer have a room.
            finishArchive(room, childRepository.findByNurseryIdAndRoomIdAndEnrollmentStatusOrderByFullNameAsc(
                TenantContext.currentNurseryId(), room.getId(), EnrollmentStatus.ENROLLED));
        }
        return toResponse(room);
    }

    /**
     * Archive a room that still has children: every enrolled child must be
     * reassigned to another active room, then the source room is archived — all
     * in one transaction. Capacity is enforced per destination room.
     */
    @Transactional
    public RoomResponse archiveWithReassignment(UUID roomId, ArchiveRoomRequest req) {
        UUID nurseryId = TenantContext.currentNurseryId();
        Room source = loadOwned(roomId);
        if (source.getArchivedAt() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Room is already archived");
        }

        // The children that actually live in this room right now.
        List<Child> sourceChildren = childRepository
            .findByNurseryIdAndRoomIdAndEnrollmentStatusOrderByFullNameAsc(
                nurseryId, roomId, EnrollmentStatus.ENROLLED);
        Map<UUID, Child> childrenById = sourceChildren.stream()
            .collect(Collectors.toMap(Child::getId, c -> c));

        // Desired child -> destination room, from the request.
        List<ArchiveRoomRequest.Reassignment> moves =
            req != null && req.reassignments() != null ? req.reassignments() : List.of();
        Map<UUID, UUID> desired = new HashMap<>();
        for (ArchiveRoomRequest.Reassignment m : moves) {
            if (m.childId() == null || m.roomId() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Each reassignment needs a childId and a roomId");
            }
            if (!childrenById.containsKey(m.childId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Child " + m.childId() + " is not in this room");
            }
            if (m.roomId().equals(roomId)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Cannot reassign a child to the room being archived");
            }
            desired.put(m.childId(), m.roomId());
        }

        // Validate each destination room's capacity against the incoming batch.
        Map<UUID, Long> incoming = desired.values().stream()
            .collect(Collectors.groupingBy(id -> id, Collectors.counting()));
        Map<UUID, Room> targets = new HashMap<>();
        for (Map.Entry<UUID, Long> e : incoming.entrySet()) {
            Room target = roomRepository.findByIdAndNurseryId(e.getKey(), nurseryId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found"));
            if (target.getArchivedAt() != null) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Room " + target.getName() + " is archived");
            }
            long current = childRepository.countByRoomIdAndEnrollmentStatus(
                target.getId(), EnrollmentStatus.ENROLLED);
            if (current + e.getValue() > target.getCapacity()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Room " + target.getName() + " is full (" + target.getCapacity() + " of " + target.getCapacity() + " places taken)");
            }
            targets.put(target.getId(), target);
        }

        // Apply the moves; children that weren't moved stay enrolled without a room.
        desired.forEach((childId, targetRoomId) ->
            childrenById.get(childId).setRoom(targets.get(targetRoomId)));
        childRepository.saveAll(childrenById.values());
        List<Child> leftOver = sourceChildren.stream().filter(c -> !desired.containsKey(c.getId())).toList();

        finishArchive(source, leftOver);
        events.publishEvent(RoomMembershipChangedEvent.of(desired.values().toArray(UUID[]::new)));
        return toResponse(source);
    }

    @Transactional
    public RoomResponse restore(UUID id) {
        Room room = loadOwned(id);
        if (room.getArchivedAt() != null) {
            boolean clash = roomRepository
                .existsByNurseryIdAndNameIgnoreCaseAndArchivedAtIsNull(
                    TenantContext.currentNurseryId(), room.getName());
            if (clash) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "An active room with this name already exists; rename it before restoring");
            }
            room.setArchivedAt(null);
            roomRepository.save(room);
        }
        return toResponse(room);
    }

    private static void checkAgeRange(Integer min, Integer max) {
        if (min != null && max != null && min > max) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "The minimum age can't be higher than the maximum age");
        }
    }

    private static boolean fitsAgeRange(Child c, Integer min, Integer max) {
        if (c.getDob() == null || (min == null && max == null)) return true;
        long months = Period.between(c.getDob(), LocalDate.now()).toTotalMonths();
        return (min == null || months >= min) && (max == null || months <= max);
    }

    /** The requested children, checked: in this nursery, enrolled, within capacity and age range. */
    private List<Child> loadChildrenForRoom(List<UUID> ids, Room room) {
        UUID nurseryId = TenantContext.currentNurseryId();
        List<Child> list = new java.util.ArrayList<>();
        for (UUID id : new LinkedHashSet<>(ids)) {
            Child c = childRepository.findById(id)
                .filter(x -> x.getNursery() != null && nurseryId.equals(x.getNursery().getId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Child not found"));
            if (c.getEnrollmentStatus() != EnrollmentStatus.ENROLLED) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, c.getFullName() + " is not enrolled");
            }
            if (!fitsAgeRange(c, room.getAgeMinMonths(), room.getAgeMaxMonths())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                    c.getFullName() + " is outside this room's age range");
            }
            list.add(c);
        }
        if (list.size() > room.getCapacity()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Only " + room.getCapacity() + " children fit in this room");
        }
        return list;
    }

    /** Puts the children in the room (moving them from any other room) and updates the chats. */
    private void moveChildrenInto(Room room, List<Child> kids) {
        if (kids.isEmpty()) return;
        Set<UUID> touched = new HashSet<>();
        touched.add(room.getId());
        for (Child c : kids) {
            if (c.getRoom() != null) touched.add(c.getRoom().getId());
            c.setRoom(room);
        }
        childRepository.saveAll(kids);
        events.publishEvent(RoomMembershipChangedEvent.of(touched.toArray(UUID[]::new)));
    }

    private Room loadOwned(UUID id) {
        return roomRepository.findByIdAndNurseryId(id, TenantContext.currentNurseryId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found"));
    }
}
