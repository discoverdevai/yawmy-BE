package com.discoverdev.nms.admin;

import com.discoverdev.nms.admin.dto.CreateRoomRequest;
import com.discoverdev.nms.admin.dto.RoomResponse;
import com.discoverdev.nms.admin.dto.UpdateRoomRequest;
import com.discoverdev.nms.child.ChildRepository;
import com.discoverdev.nms.child.EnrollmentStatus;
import com.discoverdev.nms.nursery.Nursery;
import com.discoverdev.nms.nursery.NurseryRepository;
import com.discoverdev.nms.nursery.Room;
import com.discoverdev.nms.nursery.RoomRepository;
import com.discoverdev.nms.tenant.TenantContext;
import java.time.Instant;
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

    public AdminRoomService(
        RoomRepository roomRepository,
        NurseryRepository nurseryRepository,
        ChildRepository childRepository,
        AdminTeacherAssignmentService teacherAssignmentService
    ) {
        this.roomRepository = roomRepository;
        this.nurseryRepository = nurseryRepository;
        this.childRepository = childRepository;
        this.teacherAssignmentService = teacherAssignmentService;
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
