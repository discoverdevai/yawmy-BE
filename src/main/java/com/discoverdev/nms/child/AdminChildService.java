package com.discoverdev.nms.child;

import com.discoverdev.nms.child.dto.ChildResponse;
import com.discoverdev.nms.child.dto.CreateChildRequest;
import com.discoverdev.nms.child.dto.ReassignRoomRequest;
import com.discoverdev.nms.child.dto.UpdateChildRequest;
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
public class AdminChildService {

    private final ChildRepository childRepository;
    private final RoomRepository roomRepository;
    private final NurseryRepository nurseryRepository;

    public AdminChildService(
        ChildRepository childRepository,
        RoomRepository roomRepository,
        NurseryRepository nurseryRepository
    ) {
        this.childRepository = childRepository;
        this.roomRepository = roomRepository;
        this.nurseryRepository = nurseryRepository;
    }

    @Transactional
    public ChildResponse create(CreateChildRequest req) {
        UUID nurseryId = TenantContext.currentNurseryId();
        Nursery nursery = nurseryRepository.getReferenceById(nurseryId);
        Room room = null;
        if (req.roomId() != null) {
            room = loadActiveRoom(req.roomId(), nurseryId);
        }
        Child child = Child.builder()
            .nursery(nursery)
            .room(room)
            .fullName(req.fullName())
            .dob(req.dob())
            .allergies(req.allergies())
            .medicalNotes(req.medicalNotes())
            .immunizations(req.immunizations())
            .enrollmentStatus(EnrollmentStatus.ENROLLED)
            .build();
        return ChildResponse.of(childRepository.save(child));
    }

    @Transactional(readOnly = true)
    public List<ChildResponse> list(UUID roomId, boolean includeUnenrolled) {
        UUID nurseryId = TenantContext.currentNurseryId();
        List<Child> children;
        if (roomId != null) {
            children = includeUnenrolled
                ? childRepository.findByNurseryIdAndRoomIdOrderByFullNameAsc(nurseryId, roomId)
                : childRepository.findByNurseryIdAndRoomIdAndEnrollmentStatusOrderByFullNameAsc(
                    nurseryId, roomId, EnrollmentStatus.ENROLLED);
        } else if (includeUnenrolled) {
            children = childRepository.findByNurseryIdOrderByFullNameAsc(nurseryId);
        } else {
            children = childRepository.findByNurseryIdAndEnrollmentStatusOrderByFullNameAsc(
                nurseryId, EnrollmentStatus.ENROLLED);
        }
        return children.stream().map(ChildResponse::of).toList();
    }

    @Transactional(readOnly = true)
    public ChildResponse get(UUID id) {
        return ChildResponse.of(loadOwned(id));
    }

    @Transactional
    public ChildResponse update(UUID id, UpdateChildRequest req) {
        Child child = loadOwned(id);
        if (req.fullName() != null) child.setFullName(req.fullName());
        if (req.dob() != null) child.setDob(req.dob());
        if (req.allergies() != null) child.setAllergies(req.allergies());
        if (req.medicalNotes() != null) child.setMedicalNotes(req.medicalNotes());
        if (req.immunizations() != null) child.setImmunizations(req.immunizations());
        return ChildResponse.of(childRepository.save(child));
    }

    @Transactional
    public ChildResponse reassignRoom(UUID id, ReassignRoomRequest req) {
        UUID nurseryId = TenantContext.currentNurseryId();
        Child child = loadOwned(id);
        if (req.roomId() == null) {
            child.setRoom(null);
        } else {
            child.setRoom(loadActiveRoom(req.roomId(), nurseryId));
        }
        return ChildResponse.of(childRepository.save(child));
    }

    @Transactional
    public ChildResponse unenroll(UUID id) {
        Child child = loadOwned(id);
        if (child.getEnrollmentStatus() == EnrollmentStatus.UNENROLLED) {
            return ChildResponse.of(child);
        }
        child.setEnrollmentStatus(EnrollmentStatus.UNENROLLED);
        child.setUnenrolledAt(Instant.now());
        return ChildResponse.of(childRepository.save(child));
    }

    @Transactional
    public ChildResponse restore(UUID id) {
        Child child = loadOwned(id);
        if (child.getEnrollmentStatus() == EnrollmentStatus.ENROLLED) {
            return ChildResponse.of(child);
        }
        child.setEnrollmentStatus(EnrollmentStatus.ENROLLED);
        child.setUnenrolledAt(null);
        return ChildResponse.of(childRepository.save(child));
    }

    Child loadOwned(UUID id) {
        return childRepository.findByIdAndNurseryId(id, TenantContext.currentNurseryId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Child not found"));
    }

    private Room loadActiveRoom(UUID roomId, UUID nurseryId) {
        Room room = roomRepository.findByIdAndNurseryId(roomId, nurseryId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found"));
        if (room.getArchivedAt() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Room is archived");
        }
        return room;
    }
}
