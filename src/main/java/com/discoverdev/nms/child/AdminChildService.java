package com.discoverdev.nms.child;

import com.discoverdev.nms.chat.ChatAccessService;
import com.discoverdev.nms.chat.RoomMembershipChangedEvent;
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
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AdminChildService {

    private final ChildRepository childRepository;
    private final RoomRepository roomRepository;
    private final NurseryRepository nurseryRepository;
    private final GuardianLinkRepository guardianLinkRepository;
    private final ChatAccessService chatAccess;
    private final ApplicationEventPublisher events;

    public AdminChildService(
        ChildRepository childRepository,
        RoomRepository roomRepository,
        NurseryRepository nurseryRepository,
        GuardianLinkRepository guardianLinkRepository,
        ChatAccessService chatAccess,
        ApplicationEventPublisher events
    ) {
        this.childRepository = childRepository;
        this.roomRepository = roomRepository;
        this.nurseryRepository = nurseryRepository;
        this.guardianLinkRepository = guardianLinkRepository;
        this.chatAccess = chatAccess;
        this.events = events;
    }

    /** Ask the chat layer to re-derive who belongs in these rooms' group chats. */
    private void announceRoomChange(UUID... roomIds) {
        RoomMembershipChangedEvent event = RoomMembershipChangedEvent.of(roomIds);
        if (!event.isEmpty()) events.publishEvent(event);
    }

    private static UUID roomIdOf(Child child) {
        return child.getRoom() != null ? child.getRoom().getId() : null;
    }

    @Transactional
    public ChildResponse create(CreateChildRequest req) {
        UUID nurseryId = TenantContext.currentNurseryId();
        Nursery nursery = nurseryRepository.getReferenceById(nurseryId);
        Room room = null;
        if (req.roomId() != null) {
            room = loadActiveRoom(req.roomId(), nurseryId);
            assertRoomHasSpace(room);
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
        Child saved = childRepository.save(child);
        // Guardians are linked afterwards, so this usually finds nothing to add —
        // but a child created straight into a room still belongs in its chat.
        announceRoomChange(roomIdOf(saved));
        return ChildResponse.of(saved);
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
        UUID previousRoomId = roomIdOf(child);
        if (req.roomId() == null) {
            child.setRoom(null);
        } else {
            Room target = loadActiveRoom(req.roomId(), nurseryId);
            UUID currentRoomId = child.getRoom() != null ? child.getRoom().getId() : null;
            // Only enforce capacity when actually moving into a different room.
            if (!target.getId().equals(currentRoomId)) {
                assertRoomHasSpace(target);
            }
            child.setRoom(target);
        }
        Child saved = childRepository.save(child);
        // Both ends: the guardians may need to leave the old group and join the
        // new one — or stay in the old one, if another of their children is
        // still there. Recomputing both rooms settles that without asking.
        announceRoomChange(previousRoomId, roomIdOf(saved));
        return ChildResponse.of(saved);
    }

    @Transactional
    public ChildResponse unenroll(UUID id) {
        Child child = loadOwned(id);
        if (child.getEnrollmentStatus() == EnrollmentStatus.UNENROLLED) {
            return ChildResponse.of(child);
        }
        child.setEnrollmentStatus(EnrollmentStatus.UNENROLLED);
        child.setUnenrolledAt(Instant.now());
        Child saved = childRepository.save(child);
        // PRD §6.1: "All chat access revoked immediately on unenrollment".
        // Revoke chat for each linked guardian who now has no other enrolled children.
        guardianLinkRepository.findByChildIdOrderByCreatedAtAsc(saved.getId())
            .forEach(link -> chatAccess.revokeParentIfNoEnrolledChildren(link.getGuardian()));
        announceRoomChange(roomIdOf(saved));
        return ChildResponse.of(saved);
    }

    @Transactional
    public ChildResponse restore(UUID id) {
        Child child = loadOwned(id);
        if (child.getEnrollmentStatus() == EnrollmentStatus.ENROLLED) {
            return ChildResponse.of(child);
        }
        child.setEnrollmentStatus(EnrollmentStatus.ENROLLED);
        child.setUnenrolledAt(null);
        Child saved = childRepository.save(child);
        announceRoomChange(roomIdOf(saved));
        return ChildResponse.of(saved);
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

    /** Reject assigning a child when the room is already at capacity. */
    private void assertRoomHasSpace(Room room) {
        long current = childRepository.countByRoomIdAndEnrollmentStatus(
            room.getId(), EnrollmentStatus.ENROLLED);
        if (current >= room.getCapacity()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Room is full (max " + room.getCapacity() + " children)");
        }
    }
}
