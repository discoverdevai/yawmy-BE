package com.discoverdev.nms.admin;

import com.discoverdev.nms.auth.InviteRepository;
import com.discoverdev.nms.auth.RefreshTokenRepository;
import com.discoverdev.nms.chat.ChatParticipant;
import com.discoverdev.nms.chat.ChatParticipantRepository;
import com.discoverdev.nms.chat.RoomMembershipChangedEvent;
import com.discoverdev.nms.child.GuardianLink;
import com.discoverdev.nms.child.GuardianLinkRepository;
import com.discoverdev.nms.nursery.TeacherRoomAssignmentRepository;
import com.discoverdev.nms.tenant.TenantContext;
import com.discoverdev.nms.user.Role;
import com.discoverdev.nms.user.User;
import com.discoverdev.nms.user.UserRepository;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Archive / Restore Member (Jira YC-117, YC-118, YC-119) for teachers and parents.
 *
 * Archive is a soft, reversible step: the account and its history stay, sign-in is blocked
 * (and any open session stops at its next request), open invitation links stop working, a
 * teacher is taken out of their rooms, and the member leaves every chat. Restore lets them
 * sign in again (a member archived before activating gets a fresh invite via "Resend Invite");
 * a restored teacher is assigned to rooms again by the owner.
 */
@Service
public class MemberArchiveService {

    private final UserRepository users;
    private final MemberArchiveRepository archives;
    private final RefreshTokenRepository refreshTokens;
    private final InviteRepository invites;
    private final TeacherRoomAssignmentRepository teacherRooms;
    private final GuardianLinkRepository guardianLinks;
    private final ChatParticipantRepository participants;
    private final ApplicationEventPublisher events;

    public MemberArchiveService(
        UserRepository users,
        MemberArchiveRepository archives,
        RefreshTokenRepository refreshTokens,
        InviteRepository invites,
        TeacherRoomAssignmentRepository teacherRooms,
        GuardianLinkRepository guardianLinks,
        ChatParticipantRepository participants,
        ApplicationEventPublisher events
    ) {
        this.users = users;
        this.archives = archives;
        this.refreshTokens = refreshTokens;
        this.invites = invites;
        this.teacherRooms = teacherRooms;
        this.guardianLinks = guardianLinks;
        this.participants = participants;
        this.events = events;
    }

    public boolean isArchived(UUID userId) {
        return archives.existsById(userId);
    }

    @Transactional
    public void archive(UUID userId, Role role) {
        User u = load(userId, role);
        if (archives.existsById(u.getId())) return;   // already archived
        Instant now = Instant.now();
        archives.save(MemberArchive.builder().userId(u.getId()).archivedAt(now).wasActive(u.isEnabled()).build());

        u.setEnabled(false);                             // blocks sign-in and every open session
        users.save(u);
        refreshTokens.revokeAllForUser(u, now);
        invites.closePendingForUser(u.getId(), now);     // old invitation links stop working

        Set<UUID> rooms = new HashSet<>();
        if (role == Role.TEACHER) {
            teacherRooms.findByTeacherIdOrderByCreatedAtAsc(u.getId()).forEach(a -> rooms.add(a.getRoom().getId()));
            teacherRooms.deleteByTeacherId(u.getId());
        } else {
            for (GuardianLink l : guardianLinks.findByGuardianIdOrderByCreatedAtAsc(u.getId())) {
                if (l.getChild().getRoom() != null) rooms.add(l.getChild().getRoom().getId());
            }
        }
        List<ChatParticipant> active = participants.findActiveByUserAndNursery(u.getId(), u.getNursery().getId());
        active.forEach(p -> p.setLeftAt(now));
        participants.saveAll(active);
        if (!rooms.isEmpty()) events.publishEvent(RoomMembershipChangedEvent.of(rooms.toArray(UUID[]::new)));
    }

    @Transactional
    public void restore(UUID userId, Role role) {
        User u = load(userId, role);
        MemberArchive a = archives.findById(u.getId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "This member is not archived"));
        archives.delete(a);
        u.setEnabled(a.isWasActive());                   // a pending member stays pending (resend the invite)
        users.save(u);
        if (role == Role.PARENT) {                        // a parent rejoins their children's room chats
            Set<UUID> rooms = new HashSet<>();
            for (GuardianLink l : guardianLinks.findByGuardianIdOrderByCreatedAtAsc(u.getId())) {
                if (l.getChild().getRoom() != null) rooms.add(l.getChild().getRoom().getId());
            }
            if (!rooms.isEmpty()) events.publishEvent(RoomMembershipChangedEvent.of(rooms.toArray(UUID[]::new)));
        }
    }

    private User load(UUID id, Role role) {
        UUID nurseryId = TenantContext.currentNurseryId();
        String who = role == Role.TEACHER ? "Teacher" : "Parent";
        return users.findById(id)
            .filter(u -> u.getRole() == role && u.getDeletedAt() == null
                && u.getNursery() != null && nurseryId.equals(u.getNursery().getId()))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, who + " not found"));
    }
}
