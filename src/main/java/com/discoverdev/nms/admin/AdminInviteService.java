package com.discoverdev.nms.admin;

import com.discoverdev.nms.admin.dto.CreateInviteRequest;
import com.discoverdev.nms.admin.dto.CreateInviteResponse;
import com.discoverdev.nms.auth.UserInvitationFactory;
import com.discoverdev.nms.user.Role;
import com.discoverdev.nms.user.User;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AdminInviteService {

    private final UserInvitationFactory invitationFactory;

    public AdminInviteService(UserInvitationFactory invitationFactory) {
        this.invitationFactory = invitationFactory;
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
