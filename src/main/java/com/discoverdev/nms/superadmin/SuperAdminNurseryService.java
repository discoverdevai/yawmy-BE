package com.discoverdev.nms.superadmin;

import com.discoverdev.nms.admin.dto.CreateInviteResponse;
import com.discoverdev.nms.admin.dto.NurseryResponse;
import com.discoverdev.nms.auth.MailService;
import com.discoverdev.nms.auth.UserInvitationFactory;
import com.discoverdev.nms.config.InviteProperties;
import com.discoverdev.nms.nursery.Nursery;
import com.discoverdev.nms.nursery.NurseryRepository;
import com.discoverdev.nms.superadmin.dto.CreateNurseryRequest;
import com.discoverdev.nms.superadmin.dto.InviteAdminRequest;
import com.discoverdev.nms.superadmin.dto.InviteNurseryAdminRequest;
import com.discoverdev.nms.superadmin.dto.RenameNurseryRequest;
import com.discoverdev.nms.user.Role;
import com.discoverdev.nms.user.User;
import com.discoverdev.nms.user.UserRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class SuperAdminNurseryService {

    private final NurseryRepository nurseryRepository;
    private final UserInvitationFactory invitationFactory;
    private final MailService mailService;
    private final InviteProperties inviteProperties;
    private final UserRepository userRepository;

    public SuperAdminNurseryService(
        NurseryRepository nurseryRepository,
        UserInvitationFactory invitationFactory,
        MailService mailService,
        InviteProperties inviteProperties,
        UserRepository userRepository
    ) {
        this.nurseryRepository = nurseryRepository;
        this.invitationFactory = invitationFactory;
        this.mailService = mailService;
        this.inviteProperties = inviteProperties;
        this.userRepository = userRepository;
    }

    /** Reactivate a pending admin by user id: issue a fresh token and re-email it. */
    @Transactional
    public CreateInviteResponse resendInvite(UUID userId, User superAdmin) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        return resendFor(user, superAdmin);
    }

    /**
     * Reactivate a pending admin by their email — the value from the reactivation
     * notification, so the root admin doesn't need to look up a user id.
     */
    @Transactional
    public CreateInviteResponse resendInvite(String email, User superAdmin) {
        User user = userRepository.findByEmailIgnoreCase(email)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                "No pending admin found with this email"));
        return resendFor(user, superAdmin);
    }

    private CreateInviteResponse resendFor(User user, User superAdmin) {
        if (user.getRole() != Role.ADMIN) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User is not an admin");
        }
        if (user.isEnabled()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Admin already activated");
        }
        UserInvitationFactory.IssuedInvite issued = invitationFactory.reissueInvite(user, superAdmin);
        mailService.sendAdminInvite(issued.email(), user.getFullName(), buildInviteLink(issued.token()));
        return new CreateInviteResponse(
            issued.inviteId(),
            issued.userId(),
            issued.email(),
            issued.role(),
            issued.token(),
            issued.expiresAt()
        );
    }

    private String buildInviteLink(String token) {
        String base = inviteProperties.linkBaseUrl();
        String sep = base != null && base.contains("?") ? "&" : "?";
        return base + sep + "token=" + token;
    }

    @Transactional
    public NurseryResponse create(CreateNurseryRequest req) {
        Nursery nursery = Nursery.builder().name(req.name()).build();
        return NurseryResponse.of(nurseryRepository.save(nursery));
    }

    @Transactional(readOnly = true)
    public List<NurseryResponse> list() {
        return nurseryRepository.findAllByOrderByNameAsc().stream()
            .map(NurseryResponse::of)
            .toList();
    }

    @Transactional(readOnly = true)
    public NurseryResponse get(UUID id) {
        return NurseryResponse.of(load(id));
    }

    @Transactional
    public NurseryResponse rename(UUID id, RenameNurseryRequest req) {
        Nursery nursery = load(id);
        nursery.setName(req.name());
        return NurseryResponse.of(nurseryRepository.save(nursery));
    }

    /**
     * One-call flow: create a bare nursery (id only) and invite its admin.
     * The admin completes the nursery profile (name/logo/location/hours/geofence)
     * after accepting the invite.
     */
    @Transactional
    public CreateInviteResponse inviteAdmin(InviteAdminRequest req, User superAdmin) {
        Nursery nursery = nurseryRepository.save(Nursery.builder().build());
        UserInvitationFactory.IssuedInvite issued = invitationFactory.createInvite(
            req.email(),
            req.fullName(),
            Role.ADMIN,
            null,
            null,
            nursery,
            superAdmin
        );
        mailService.sendAdminInvite(issued.email(), req.fullName(), buildInviteLink(issued.token()));
        return new CreateInviteResponse(
            issued.inviteId(),
            issued.userId(),
            issued.email(),
            issued.role(),
            issued.token(),
            issued.expiresAt()
        );
    }

    @Transactional
    public CreateInviteResponse inviteAdmin(UUID nurseryId, InviteNurseryAdminRequest req, User superAdmin) {
        Nursery nursery = load(nurseryId);
        UserInvitationFactory.IssuedInvite issued = invitationFactory.createInvite(
            req.email(),
            req.fullName(),
            Role.ADMIN,
            req.locale(),
            req.phone(),
            nursery,
            superAdmin
        );
        return new CreateInviteResponse(
            issued.inviteId(),
            issued.userId(),
            issued.email(),
            issued.role(),
            issued.token(),
            issued.expiresAt()
        );
    }

    private Nursery load(UUID id) {
        return nurseryRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Nursery not found"));
    }
}
