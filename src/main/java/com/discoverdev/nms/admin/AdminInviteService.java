package com.discoverdev.nms.admin;

import com.discoverdev.nms.admin.dto.CreateInviteRequest;
import com.discoverdev.nms.admin.dto.CreateInviteResponse;
import com.discoverdev.nms.auth.MailService;
import com.discoverdev.nms.auth.UserInvitationFactory;
import com.discoverdev.nms.config.InviteProperties;
import com.discoverdev.nms.user.Role;
import com.discoverdev.nms.user.User;
import com.discoverdev.nms.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AdminInviteService {

    private final UserInvitationFactory invitationFactory;
    private final UserRepository userRepository;
    private final MailService mailService;
    private final InviteProperties inviteProperties;

    public AdminInviteService(
        UserInvitationFactory invitationFactory,
        UserRepository userRepository,
        MailService mailService,
        InviteProperties inviteProperties
    ) {
        this.invitationFactory = invitationFactory;
        this.userRepository = userRepository;
        this.mailService = mailService;
        this.inviteProperties = inviteProperties;
    }

    @Transactional
    public CreateInviteResponse createInvite(CreateInviteRequest req, User invitedBy) {
        if (req.role() == Role.SUPER_ADMIN) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot invite a super admin");
        }
        if (invitedBy.getNursery() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Inviter is not attached to a nursery");
        }

        UserInvitationFactory.IssuedInvite issued = invitationFactory.createInvite(
            req.email(),
            req.fullName(),
            req.role(),
            req.locale(),
            req.phone(),
            invitedBy.getNursery(),
            invitedBy
        );

        // Store the job title for teacher staff.
        if (req.role() == Role.TEACHER && req.staffRole() != null) {
            userRepository.findById(issued.userId()).ifPresent(u -> {
                u.setStaffRole(req.staffRole());
                userRepository.save(u);
            });
        }

        // Email the invite link — teacher-app link for teachers, admin-app link for admins.
        String link = inviteProperties.linkFor(req.role(), issued.token());
        if (req.role() == Role.TEACHER) {
            mailService.sendStaffInvite(issued.email(), req.fullName(), link);
        } else if (req.role() == Role.ADMIN) {
            mailService.sendAdminInvite(issued.email(), req.fullName(), link);
        }

        return new CreateInviteResponse(
            issued.inviteId(),
            issued.userId(),
            issued.email(),
            issued.role(),
            issued.token(),
            issued.expiresAt()
        );
    }

}
