package com.discoverdev.nms.child;

import com.discoverdev.nms.child.dto.ChildResponse;
import com.discoverdev.nms.child.dto.ParentChildDetailResponse;
import com.discoverdev.nms.user.User;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ParentChildService {

    private final ChildRepository childRepository;
    private final GuardianLinkRepository linkRepository;
    private final AuthorizedPickupRepository pickupRepository;
    private final EmergencyContactRepository emergencyContactRepository;

    public ParentChildService(
        ChildRepository childRepository,
        GuardianLinkRepository linkRepository,
        AuthorizedPickupRepository pickupRepository,
        EmergencyContactRepository emergencyContactRepository
    ) {
        this.childRepository = childRepository;
        this.linkRepository = linkRepository;
        this.pickupRepository = pickupRepository;
        this.emergencyContactRepository = emergencyContactRepository;
    }

    @Transactional(readOnly = true)
    public List<ChildResponse> listForParent(User parent) {
        return linkRepository.findByGuardianIdOrderByCreatedAtAsc(parent.getId()).stream()
            .map(GuardianLink::getChild)
            .filter(c -> c.getEnrollmentStatus() == EnrollmentStatus.ENROLLED)
            .map(ChildResponse::of)
            .toList();
    }

    @Transactional(readOnly = true)
    public ParentChildDetailResponse getForParent(UUID childId, User parent) {
        Child child = linkRepository.findByChildIdAndGuardianId(childId, parent.getId())
            .map(GuardianLink::getChild)
            .filter(c -> c.getEnrollmentStatus() == EnrollmentStatus.ENROLLED)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Child not found"));
        return ParentChildDetailResponse.of(
            child,
            pickupRepository.findByChildIdOrderByCreatedAtAsc(child.getId()),
            emergencyContactRepository.findByChildIdOrderByCreatedAtAsc(child.getId())
        );
    }
}
