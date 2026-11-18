package com.discoverdev.nms.admin;

import com.discoverdev.nms.auth.MailService;
import com.discoverdev.nms.auth.UserInvitationFactory;
import com.discoverdev.nms.config.InviteProperties;
import com.discoverdev.nms.tenant.TenantContext;
import com.discoverdev.nms.user.Role;
import com.discoverdev.nms.user.User;
import com.discoverdev.nms.user.UserRepository;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Re-sends the invitation to a parent who hasn't activated their account yet
 * (Jira YC-30: "Resend Invite" for parents in Pending status).
 */
@Service
public class ParentInviteResendService {

    private final UserRepository userRepository;
    private final UserInvitationFactory invitationFactory;
    private final MailService mailService;
    private final InviteProperties inviteProperties;

    public ParentInviteResendService(
        UserRepository userRepository,
        UserInvitationFactory invitationFactory,
        MailService mailService,
        InviteProperties inviteProperties
    ) {
        this.userRepository = userRepository;
        this.invitationFactory = invitationFactory;
        this.mailService = mailService;
        this.inviteProperties = inviteProperties;
    }

    @Transactional
    public void resendInvite(UUID parentId, User invitedBy) {
        UUID nurseryId = TenantContext.currentNurseryId();
        User parent = userRepository.findById(parentId)
            .filter(p -> p.getRole() == Role.PARENT && p.getDeletedAt() == null
                && p.getNursery() != null && nurseryId.equals(p.getNursery().getId()))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Parent not found"));
        if (parent.isEnabled()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "This parent has already activated their account.");
        }
        UserInvitationFactory.IssuedInvite issued = invitationFactory.reissueInvite(parent, invitedBy);
        mailService.sendParentInvite(
            issued.email(), parent.getFullName(), inviteProperties.linkFor(Role.PARENT, issued.token()));
    }
}
