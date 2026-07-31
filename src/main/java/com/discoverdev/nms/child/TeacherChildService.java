package com.discoverdev.nms.child;

import com.discoverdev.nms.child.dto.ChildResponse;
import com.discoverdev.nms.child.dto.EmergencyContactResponse;
import com.discoverdev.nms.child.dto.PickupResponse;
import com.discoverdev.nms.tenant.TeacherAccessService;
import com.discoverdev.nms.user.User;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Backs the teacher app's child profile. Reads only — teachers never edit these. */
@Service
public class TeacherChildService {

    private final ChildRepository childRepository;
    private final EmergencyContactRepository emergencyContactRepository;
    private final AuthorizedPickupRepository pickupRepository;
    private final TeacherAccessService teacherAccess;

    public TeacherChildService(
        ChildRepository childRepository,
        EmergencyContactRepository emergencyContactRepository,
        AuthorizedPickupRepository pickupRepository,
        TeacherAccessService teacherAccess
    ) {
        this.childRepository = childRepository;
        this.emergencyContactRepository = emergencyContactRepository;
        this.pickupRepository = pickupRepository;
        this.teacherAccess = teacherAccess;
    }

    @Transactional(readOnly = true)
    public ChildResponse getChild(UUID childId, User teacher) {
        return ChildResponse.of(requireAccessible(childId, teacher));
    }

    @Transactional(readOnly = true)
    public List<EmergencyContactResponse> emergencyContacts(UUID childId, User teacher) {
        requireAccessible(childId, teacher);
        return emergencyContactRepository.findByChildIdOrderByCreatedAtAsc(childId).stream()
            .map(EmergencyContactResponse::of)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<PickupResponse> pickups(UUID childId, User teacher) {
        requireAccessible(childId, teacher);
        return pickupRepository.findByChildIdOrderByCreatedAtAsc(childId).stream()
            .map(PickupResponse::of)
            .toList();
    }

    /**
     * 404 rather than 403 for a child the teacher is not assigned to, so the
     * endpoint cannot be used to discover which child ids exist elsewhere.
     */
    private Child requireAccessible(UUID childId, User teacher) {
        Child child = childRepository.findById(childId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Child not found"));
        if (!teacherAccess.canAccessChild(teacher, child)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Child not found");
        }
        return child;
    }
}
