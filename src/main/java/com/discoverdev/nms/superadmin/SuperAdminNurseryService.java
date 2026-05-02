package com.discoverdev.nms.superadmin;

import com.discoverdev.nms.admin.dto.CreateInviteResponse;
import com.discoverdev.nms.admin.dto.NurseryResponse;
import com.discoverdev.nms.auth.UserInvitationFactory;
import com.discoverdev.nms.nursery.Nursery;
import com.discoverdev.nms.nursery.NurseryRepository;
import com.discoverdev.nms.superadmin.dto.CreateNurseryRequest;
import com.discoverdev.nms.superadmin.dto.InviteNurseryAdminRequest;
import com.discoverdev.nms.superadmin.dto.RenameNurseryRequest;
import com.discoverdev.nms.user.Role;
import com.discoverdev.nms.user.User;
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

    public SuperAdminNurseryService(
        NurseryRepository nurseryRepository,
        UserInvitationFactory invitationFactory
    ) {
        this.nurseryRepository = nurseryRepository;
        this.invitationFactory = invitationFactory;
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
