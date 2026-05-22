package com.discoverdev.nms.child;

import com.discoverdev.nms.child.dto.ContactRequest;
import com.discoverdev.nms.child.dto.EmergencyContactResponse;
import com.discoverdev.nms.tenant.TenantContext;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AdminEmergencyContactService {

    private final EmergencyContactRepository contactRepository;
    private final ChildRepository childRepository;

    public AdminEmergencyContactService(
        EmergencyContactRepository contactRepository,
        ChildRepository childRepository
    ) {
        this.contactRepository = contactRepository;
        this.childRepository = childRepository;
    }

    @Transactional
    public EmergencyContactResponse create(UUID childId, ContactRequest req) {
        Child child = loadOwnedChild(childId);
        EmergencyContact c = EmergencyContact.builder()
            .child(child)
            .fullName(req.fullName())
            .relationship(req.relationship())
            .phone(req.phone())
            .build();
        return EmergencyContactResponse.of(contactRepository.save(c));
    }

    @Transactional(readOnly = true)
    public List<EmergencyContactResponse> list(UUID childId) {
        loadOwnedChild(childId);
        return contactRepository.findByChildIdOrderByCreatedAtAsc(childId).stream()
            .map(EmergencyContactResponse::of)
            .toList();
    }

    @Transactional
    public EmergencyContactResponse update(UUID childId, UUID contactId, ContactRequest req) {
        loadOwnedChild(childId);
        EmergencyContact c = contactRepository.findByIdAndChildId(contactId, childId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Emergency contact not found"));
        c.setFullName(req.fullName());
        c.setRelationship(req.relationship());
        c.setPhone(req.phone());
        return EmergencyContactResponse.of(contactRepository.save(c));
    }

    @Transactional
    public void delete(UUID childId, UUID contactId) {
        loadOwnedChild(childId);
        EmergencyContact c = contactRepository.findByIdAndChildId(contactId, childId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Emergency contact not found"));
        contactRepository.delete(c);
    }

    private Child loadOwnedChild(UUID childId) {
        return childRepository.findByIdAndNurseryId(childId, TenantContext.currentNurseryId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Child not found"));
    }
}
