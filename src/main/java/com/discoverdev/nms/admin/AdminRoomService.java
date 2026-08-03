package com.discoverdev.nms.admin;

import com.discoverdev.nms.admin.dto.ArchiveRoomRequest;
import com.discoverdev.nms.admin.dto.CreateRoomRequest;
import com.discoverdev.nms.admin.dto.RoomResponse;
import com.discoverdev.nms.admin.dto.UpdateRoomRequest;
import com.discoverdev.nms.chat.ChatAccessService;
import com.discoverdev.nms.child.Child;
import com.discoverdev.nms.child.ChildRepository;
import com.discoverdev.nms.child.EnrollmentStatus;
import com.discoverdev.nms.nursery.Nursery;
import com.discoverdev.nms.nursery.NurseryRepository;
import com.discoverdev.nms.nursery.Room;
import com.discoverdev.nms.nursery.RoomRepository;
import com.discoverdev.nms.tenant.TenantContext;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
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

    public AdminRoomService(
        RoomRepository roomRepository,
        NurseryRepository nurseryRepository,
        ChildRepository childRepository,
        AdminTeacherAssignmentService teacherAssignmentService,
        ChatAccessService chatAccess
    ) {
        this.roomRepository = roomRepository;
        this.nurseryRepository = nurseryRepository;
        this.childRepository = childRepository;
        this.teacherAssignmentService = teacherAssignmentService;
        this.chatAccess = chatAccess;
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
            .build();
        room = roomRepository.save(room);

        // Every room owns a group chat from the moment it exists, so it can be
        // listed (and joined by whoever belongs in it) before anyone has sent a
        // message. Without this the group only appears once a teacher opens it.
        chatAccess.ensureRoomGroup(room, TenantContext.currentUser());

        // Optionally assign a teacher picked in the "add room" modal.
        if (req.teacherId() != null) {
            teacherAssignmentService.assign(room.getId(), req.teacherId());
        }
        return toResponse(room);
    }

    @Transactional(readOnly = true)
    public List<RoomResponse> list(boolean includeArchived) {
        UUID nurseryId = TenantContext.currentNurseryId();
        List<Room> rooms = includeArchived
            ? roomRepository.findByNurseryIdOrderByNameAsc(nurseryId)
            : roomRepository.findByNurseryIdAndArchivedAtIsNullOrderByNameAsc(nurseryId);

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
        if (req.capacity() != null) room.setCapacity(req.capacity());
        return toResponse(roomRepository.save(room));
    }

    @Transactional
    public RoomResponse archive(UUID id) {
        Room room = loadOwned(id);
        if (room.getArchivedAt() == null) {
            room.setArchivedAt(Instant.now());
            roomRepository.save(room);
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

        // Every child in the room must be accounted for before we archive it.
        if (desired.size() < sourceChildren.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "All children must be reassigned before archiving the room");
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
                    "Room " + target.getName() + " is full (max " + target.getCapacity() + " children)");
            }
            targets.put(target.getId(), target);
        }

        // Apply the moves, then archive the emptied room.
        desired.forEach((childId, targetRoomId) ->
            childrenById.get(childId).setRoom(targets.get(targetRoomId)));
        childRepository.saveAll(childrenById.values());

        source.setArchivedAt(Instant.now());
        roomRepository.save(source);
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

    private Room loadOwned(UUID id) {
        return roomRepository.findByIdAndNurseryId(id, TenantContext.currentNurseryId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found"));
    }
}
