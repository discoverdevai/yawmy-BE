package com.discoverdev.nms.admin;

import com.discoverdev.nms.admin.dto.EnrollFamilyRequest;
import com.discoverdev.nms.admin.dto.EnrollFamilyResponse;
import com.discoverdev.nms.admin.dto.EnrollFamilyResponse.EnrolledChild;
import com.discoverdev.nms.child.AdminChildService;
import com.discoverdev.nms.child.AdminEmergencyContactService;
import com.discoverdev.nms.child.AdminGuardianService;
import com.discoverdev.nms.child.AdminPickupService;
import com.discoverdev.nms.child.dto.ChildResponse;
import com.discoverdev.nms.child.dto.ContactRequest;
import com.discoverdev.nms.child.dto.CreateChildRequest;
import com.discoverdev.nms.child.dto.GuardianLinkResponse;
import com.discoverdev.nms.child.dto.LinkOrInviteGuardianRequest;
import com.discoverdev.nms.user.User;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Atomic "invite parent" flow: creates all children, links the parent (invited +
 * emailed once) plus any additional guardians and pickup contacts, in a single
 * transaction. If anything fails, the whole thing rolls back — no partial data.
 */
@Service
public class AdminParentInviteService {

    private final AdminChildService childService;
    private final AdminGuardianService guardianService;
    private final AdminPickupService pickupService;
    private final AdminEmergencyContactService emergencyContactService;

    public AdminParentInviteService(
        AdminChildService childService,
        AdminGuardianService guardianService,
        AdminPickupService pickupService,
        AdminEmergencyContactService emergencyContactService
    ) {
        this.childService = childService;
        this.guardianService = guardianService;
        this.pickupService = pickupService;
        this.emergencyContactService = emergencyContactService;
    }

    @Transactional
    public EnrollFamilyResponse enroll(EnrollFamilyRequest req, User actor) {
        EnrollFamilyRequest.Parent parent = req.parent();

        UUID parentId = null;
        String inviteToken = null;
        Instant inviteExpiresAt = null;
        List<EnrolledChild> childRefs = new ArrayList<>();

        for (EnrollFamilyRequest.Child c : req.children()) {
            // 1. Create the child (capacity is enforced inside AdminChildService).
            ChildResponse child = childService.create(
                new CreateChildRequest(c.fullName(), c.dob(), c.roomId(), null, null, null));
            childRefs.add(new EnrolledChild(child.id(), child.fullName()));

            // 2. Link the main parent as the primary guardian. On the first child
            //    this creates the parent account + invite and emails it; on later
            //    children it just links the same parent (no duplicate email).
            GuardianLinkResponse primary = guardianService.linkOrInvite(
                child.id(),
                new LinkOrInviteGuardianRequest(
                    parent.email(), parent.fullName(), parent.relationship(), true, null, null),
                actor);
            parentId = primary.guardianId();
            if (primary.newInvite()) {
                inviteToken = primary.inviteToken();
                inviteExpiresAt = primary.inviteExpiresAt();
            }

            // 3. Additional guardians for this child — contact info only (saved
            //    as emergency contacts; the main parent above is the invited one).
            if (c.guardians() != null) {
                for (EnrollFamilyRequest.Guardian g : c.guardians()) {
                    emergencyContactService.create(
                        child.id(), new ContactRequest(g.fullName(), g.relationship(), g.phone()));
                }
            }

            // 4. Pickup contacts for this child (saved info only).
            if (c.pickups() != null) {
                for (EnrollFamilyRequest.Pickup p : c.pickups()) {
                    pickupService.create(
                        child.id(), new ContactRequest(p.fullName(), p.relationship(), p.phone()));
                }
            }
        }

        return new EnrollFamilyResponse(parentId, parent.email(), inviteToken, inviteExpiresAt, childRefs);
    }
}
