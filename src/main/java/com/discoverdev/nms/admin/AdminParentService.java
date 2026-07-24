package com.discoverdev.nms.admin;

import com.discoverdev.nms.admin.dto.LinkChildrenRequest;
import com.discoverdev.nms.admin.dto.LinkableChildResponse;
import com.discoverdev.nms.admin.dto.ParentDetailResponse;
import com.discoverdev.nms.admin.dto.ParentResponse;
import com.discoverdev.nms.admin.dto.UpdateParentRequest;
import com.discoverdev.nms.auth.Invite;
import com.discoverdev.nms.auth.InviteRepository;
import com.discoverdev.nms.child.Child;
import com.discoverdev.nms.child.ChildRepository;
import com.discoverdev.nms.child.EnrollmentStatus;
import com.discoverdev.nms.child.GuardianLink;
import com.discoverdev.nms.child.GuardianLinkRepository;
import com.discoverdev.nms.tenant.TenantContext;
import com.discoverdev.nms.user.Role;
import com.discoverdev.nms.user.User;
import com.discoverdev.nms.user.UserRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AdminParentService {

    private final UserRepository userRepository;
    private final GuardianLinkRepository guardianLinkRepository;
    private final InviteRepository inviteRepository;
    private final ChildRepository childRepository;

    public AdminParentService(
        UserRepository userRepository,
        GuardianLinkRepository guardianLinkRepository,
        InviteRepository inviteRepository,
        ChildRepository childRepository
    ) {
        this.userRepository = userRepository;
        this.guardianLinkRepository = guardianLinkRepository;
        this.inviteRepository = inviteRepository;
        this.childRepository = childRepository;
    }

    /** All (non-deleted) parents in the nursery with their relationship + status. */
    @Transactional(readOnly = true)
    public List<ParentResponse> listParents() {
        UUID nurseryId = TenantContext.currentNurseryId();
        return userRepository
            .findByNurseryIdAndRoleAndDeletedAtIsNullOrderByFullNameAsc(nurseryId, Role.PARENT)
            .stream()
            .map(p -> {
                String relationship = guardianLinkRepository
                    .findByGuardianIdOrderByCreatedAtAsc(p.getId())
                    .stream()
                    .findFirst()
                    .map(GuardianLink::getRelationship)
                    .orElse(null);
                return new ParentResponse(
                    p.getId(), p.getFullName(), p.getEmail(), relationship,
                    p.getImageUrl(), statusOf(p), p.getCreatedAt());
            })
            .toList();
    }

    /** One parent's full detail with their linked children + each child's room. */
    @Transactional(readOnly = true)
    public ParentDetailResponse getParent(UUID id) {
        return detailOf(loadParent(id));
    }

    /** Update a parent's name / email, and their relationship (across all links). */
    @Transactional
    public ParentDetailResponse update(UUID id, UpdateParentRequest req) {
        User p = loadParent(id);
        if (req.fullName() != null) {
            p.setFullName(req.fullName());
        }
        if (req.email() != null && !req.email().equalsIgnoreCase(p.getEmail())) {
            if (userRepository.existsByEmailIgnoreCase(req.email())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already in use");
            }
            p.setEmail(req.email());
        }
        userRepository.save(p);

        if (req.relationship() != null) {
            List<GuardianLink> links = guardianLinkRepository.findByGuardianIdOrderByCreatedAtAsc(id);
            links.forEach(l -> l.setRelationship(req.relationship()));
            guardianLinkRepository.saveAll(links);
        }
        return detailOf(p);
    }

    /**
     * Children that can be linked to this parent: those that don't already have a
     * guardian with the same role (Father/Mother) as the parent. So a child with
     * only a mother is linkable to a father, and vice-versa; a child that already
     * has a parent of this role (or is already linked to this parent) is excluded.
     */
    @Transactional(readOnly = true)
    public List<LinkableChildResponse> getLinkableChildren(UUID id) {
        UUID nurseryId = TenantContext.currentNurseryId();
        loadParent(id);
        String rel = relationshipOf(id);
        return childRepository
            .findByNurseryIdAndEnrollmentStatusOrderByFullNameAsc(nurseryId, EnrollmentStatus.ENROLLED)
            .stream()
            .filter(c -> guardianLinkRepository.findByChildIdOrderByCreatedAtAsc(c.getId()).stream()
                .noneMatch(l -> rel != null && rel.equalsIgnoreCase(l.getRelationship())))
            .map(c -> new LinkableChildResponse(
                c.getId(),
                c.getFullName(),
                c.getRoom() != null ? c.getRoom().getName() : null,
                c.getPhotoKey() != null ? "/api/files/children/" + c.getId() + "/photo" : null))
            .toList();
    }

    /** Link the given children to this parent (with the parent's relationship). */
    @Transactional
    public ParentDetailResponse linkChildren(UUID id, LinkChildrenRequest req) {
        UUID nurseryId = TenantContext.currentNurseryId();
        User parent = loadParent(id);
        String rel = relationshipOf(id);
        if (rel == null) rel = "Guardian";
        List<UUID> childIds = req != null && req.childIds() != null ? req.childIds() : List.of();
        for (UUID childId : childIds) {
            Child child = childRepository.findByIdAndNurseryId(childId, nurseryId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Child not found"));
            if (!guardianLinkRepository.existsByChildIdAndGuardianId(childId, id)) {
                guardianLinkRepository.save(
                    GuardianLink.builder().child(child).guardian(parent).relationship(rel).build());
            }
        }
        return detailOf(parent);
    }

    /**
     * Remove a parent from the nursery (soft-delete). For each linked child:
     *  - if this parent was their ONLY guardian, the child is unenrolled too;
     *  - if the child has another guardian, only this parent's link is removed
     *    and the child stays enrolled.
     */
    @Transactional
    public void delete(UUID id) {
        User p = loadParent(id);
        List<GuardianLink> links = guardianLinkRepository.findByGuardianIdOrderByCreatedAtAsc(id);
        for (GuardianLink link : links) {
            Child child = link.getChild();
            boolean hasOtherGuardian = guardianLinkRepository
                .findByChildIdOrderByCreatedAtAsc(child.getId())
                .stream()
                .anyMatch(l -> !l.getGuardian().getId().equals(id));
            if (!hasOtherGuardian && child.getEnrollmentStatus() != EnrollmentStatus.UNENROLLED) {
                // This parent was the child's only guardian → unenroll the child.
                child.setEnrollmentStatus(EnrollmentStatus.UNENROLLED);
                child.setUnenrolledAt(Instant.now());
                childRepository.save(child);
            }
        }
        guardianLinkRepository.deleteAll(links); // remove this parent's links either way
        p.setEnabled(false);
        p.setDeletedAt(Instant.now());
        userRepository.save(p);
    }

    private String relationshipOf(UUID parentId) {
        return guardianLinkRepository.findByGuardianIdOrderByCreatedAtAsc(parentId).stream()
            .findFirst()
            .map(GuardianLink::getRelationship)
            .orElse(null);
    }

    private User loadParent(UUID id) {
        UUID nurseryId = TenantContext.currentNurseryId();
        User p = userRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Parent not found"));
        if (p.getNursery() == null
            || !nurseryId.equals(p.getNursery().getId())
            || p.getRole() != Role.PARENT
            || p.getDeletedAt() != null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Parent not found");
        }
        return p;
    }

    private ParentDetailResponse detailOf(User p) {
        List<GuardianLink> links = guardianLinkRepository.findByGuardianIdOrderByCreatedAtAsc(p.getId());
        String relationship = links.stream().findFirst().map(GuardianLink::getRelationship).orElse(null);
        List<ParentDetailResponse.LinkedChild> children = links.stream()
            .map(GuardianLink::getChild)
            .map(c -> new ParentDetailResponse.LinkedChild(
                c.getId(),
                c.getFullName(),
                c.getRoom() != null ? c.getRoom().getName() : null))
            .toList();
        return new ParentDetailResponse(
            p.getId(), p.getFullName(), p.getEmail(), relationship,
            p.getImageUrl(), statusOf(p), p.getCreatedAt(), children);
    }

    private String statusOf(User parent) {
        if (parent.isEnabled()) {
            return "ACTIVE";
        }
        Invite latest = inviteRepository.findFirstByUserIdOrderByCreatedAtDesc(parent.getId()).orElse(null);
        if (latest != null && !latest.isUsed() && !latest.isExpired()) {
            return "PENDING";
        }
        return "INVITATION_EXPIRED";
    }
}
