package com.discoverdev.nms.child;

import com.discoverdev.nms.child.dto.ContactRequest;
import com.discoverdev.nms.child.dto.PickupResponse;
import com.discoverdev.nms.tenant.TenantContext;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AdminPickupService {

    private final AuthorizedPickupRepository pickupRepository;
    private final ChildRepository childRepository;

    public AdminPickupService(
        AuthorizedPickupRepository pickupRepository,
        ChildRepository childRepository
    ) {
        this.pickupRepository = pickupRepository;
        this.childRepository = childRepository;
    }

    @Transactional
    public PickupResponse create(UUID childId, ContactRequest req) {
        Child child = loadOwnedChild(childId);
        AuthorizedPickup p = AuthorizedPickup.builder()
            .child(child)
            .fullName(req.fullName())
            .relationship(req.relationship())
            .phone(req.phone())
            .build();
        return PickupResponse.of(pickupRepository.save(p));
    }

    @Transactional(readOnly = true)
    public List<PickupResponse> list(UUID childId) {
        loadOwnedChild(childId);
        return pickupRepository.findByChildIdOrderByCreatedAtAsc(childId).stream()
            .map(PickupResponse::of)
            .toList();
    }

    @Transactional
    public PickupResponse update(UUID childId, UUID pickupId, ContactRequest req) {
        loadOwnedChild(childId);
        AuthorizedPickup p = pickupRepository.findByIdAndChildId(pickupId, childId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pickup not found"));
        p.setFullName(req.fullName());
        p.setRelationship(req.relationship());
        p.setPhone(req.phone());
        return PickupResponse.of(pickupRepository.save(p));
    }

    @Transactional
    public void delete(UUID childId, UUID pickupId) {
        loadOwnedChild(childId);
        AuthorizedPickup p = pickupRepository.findByIdAndChildId(pickupId, childId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pickup not found"));
        pickupRepository.delete(p);
    }

    private Child loadOwnedChild(UUID childId) {
        return childRepository.findByIdAndNurseryId(childId, TenantContext.currentNurseryId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Child not found"));
    }
}
