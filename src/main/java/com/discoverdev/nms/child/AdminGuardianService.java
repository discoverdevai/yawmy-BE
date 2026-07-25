package com.discoverdev.nms.child;

import com.discoverdev.nms.auth.MailService;
import com.discoverdev.nms.auth.UserInvitationFactory;
import com.discoverdev.nms.child.dto.GuardianLinkResponse;
import com.discoverdev.nms.child.dto.LinkOrInviteGuardianRequest;
import com.discoverdev.nms.config.InviteProperties;
import com.discoverdev.nms.tenant.TenantContext;
import com.discoverdev.nms.user.Role;
import com.discoverdev.nms.user.User;
import com.discoverdev.nms.user.UserRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AdminGuardianService {

    private final GuardianLinkRepository linkRepository;
    private final ChildRepository childRepository;
    private final UserRepository userRepository;
    private final UserInvitationFactory invitationFactory;
    private final MailService mailService;
    private final InviteProperties inviteProperties;

    public AdminGuardianService(
        GuardianLinkRepository linkRepository,
        ChildRepository childRepository,
        UserRepository userRepository,
        UserInvitationFactory invitationFactory,
        MailService mailService,
        InviteProperties inviteProperties
    ) {
        this.linkRepository = linkRepository;
        this.childRepository = childRepository;
        this.userRepository = userRepository;
        this.invitationFactory = invitationFactory;
        this.mailService = mailService;
        this.inviteProperties = inviteProperties;
    }

    private String buildParentInviteLink(String token) {
        return inviteProperties.linkFor(Role.PARENT, token);
    }

    @Transactional
    public GuardianLinkResponse linkOrInvite(UUID childId, LinkOrInviteGuardianRequest req, User actor) {
        UUID nurseryId = TenantContext.currentNurseryId();
        Child child = childRepository.findByIdAndNurseryId(childId, nurseryId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Child not found"));

        Optional<User> existing = userRepository.findByEmailIgnoreCase(req.email());
        if (existing.isPresent()) {
            return linkExisting(existing.get(), child, req, nurseryId);
        }
        return inviteAndLink(child, req, actor);
    }

    @Transactional
    public void unlink(UUID childId, UUID guardianId) {
        UUID nurseryId = TenantContext.currentNurseryId();
        childRepository.findByIdAndNurseryId(childId, nurseryId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Child not found"));
        GuardianLink link = linkRepository.findByChildIdAndGuardianId(childId, guardianId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Guardian link not found"));
        linkRepository.delete(link);
    }

    @Transactional(readOnly = true)
    public List<GuardianLinkResponse> listForChild(UUID childId) {
        UUID nurseryId = TenantContext.currentNurseryId();
        childRepository.findByIdAndNurseryId(childId, nurseryId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Child not found"));
        return linkRepository.findByChildIdOrderByCreatedAtAsc(childId).stream()
            .map(GuardianLinkResponse::linked)
            .toList();
    }

    private GuardianLinkResponse linkExisting(
        User user, Child child, LinkOrInviteGuardianRequest req, UUID nurseryId
    ) {
        if (user.getNursery() == null || !nurseryId.equals(user.getNursery().getId())) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT, "Email is registered with another nursery");
        }
        if (user.getRole() != Role.PARENT) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT, "Email is registered with a non-parent role");
        }
        if (linkRepository.existsByChildIdAndGuardianId(child.getId(), user.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Guardian is already linked to this child");
        }
        GuardianLink link = GuardianLink.builder()
            .child(child)
            .guardian(user)
            .relationship(req.relationship())
            .primary(Boolean.TRUE.equals(req.isPrimary()))
            .build();
        return GuardianLinkResponse.linked(linkRepository.save(link));
    }

    private GuardianLinkResponse inviteAndLink(Child child, LinkOrInviteGuardianRequest req, User actor) {
        if (req.fullName() == null || req.fullName().isBlank()) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "fullName is required when inviting a new guardian");
        }
        UserInvitationFactory.IssuedInvite issued = invitationFactory.createInvite(
            req.email(),
            req.fullName(),
            Role.PARENT,
            req.locale(),
            req.phone(),
            child.getNursery(),
            actor
        );
        User newGuardian = userRepository.findById(issued.userId())
            .orElseThrow(() -> new IllegalStateException("Invite factory produced no user"));
        GuardianLink link = GuardianLink.builder()
            .child(child)
            .guardian(newGuardian)
            .relationship(req.relationship())
            .primary(Boolean.TRUE.equals(req.isPrimary()))
            .build();
        link = linkRepository.save(link);

        // Email the parent a clickable invite link (same as admin/teacher invites).
        mailService.sendParentInvite(issued.email(), req.fullName(), buildParentInviteLink(issued.token()));

        return GuardianLinkResponse.invited(link, issued.inviteId(), issued.token(), issued.expiresAt());
    }
}
