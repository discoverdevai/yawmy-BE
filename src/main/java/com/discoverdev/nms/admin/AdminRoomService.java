package com.discoverdev.nms.admin;

import com.discoverdev.nms.admin.dto.CreateRoomRequest;
import com.discoverdev.nms.admin.dto.RoomResponse;
import com.discoverdev.nms.admin.dto.UpdateRoomRequest;
import com.discoverdev.nms.nursery.Nursery;
import com.discoverdev.nms.nursery.NurseryRepository;
import com.discoverdev.nms.nursery.Room;
import com.discoverdev.nms.nursery.RoomRepository;
import com.discoverdev.nms.tenant.TenantContext;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AdminRoomService {

    private final RoomRepository roomRepository;
    private final NurseryRepository nurseryRepository;

    public AdminRoomService(RoomRepository roomRepository, NurseryRepository nurseryRepository) {
        this.roomRepository = roomRepository;
        this.nurseryRepository = nurseryRepository;
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
            .capacity(req.capacity())
            .build();
        return RoomResponse.of(roomRepository.save(room));
    }

    @Transactional(readOnly = true)
    public List<RoomResponse> list(boolean includeArchived) {
        UUID nurseryId = TenantContext.currentNurseryId();
        List<Room> rooms = includeArchived
            ? roomRepository.findByNurseryIdOrderByNameAsc(nurseryId)
            : roomRepository.findByNurseryIdAndArchivedAtIsNullOrderByNameAsc(nurseryId);
        return rooms.stream().map(RoomResponse::of).toList();
    }

    @Transactional(readOnly = true)
    public RoomResponse get(UUID id) {
        return RoomResponse.of(loadOwned(id));
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
        return RoomResponse.of(roomRepository.save(room));
    }

    @Transactional
    public RoomResponse archive(UUID id) {
        Room room = loadOwned(id);
        if (room.getArchivedAt() == null) {
            room.setArchivedAt(Instant.now());
            roomRepository.save(room);
        }
        return RoomResponse.of(room);
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
        return RoomResponse.of(room);
    }

    private Room loadOwned(UUID id) {
        return roomRepository.findByIdAndNurseryId(id, TenantContext.currentNurseryId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found"));
    }
}
