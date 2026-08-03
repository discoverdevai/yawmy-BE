package com.discoverdev.nms.announcement;

import com.discoverdev.nms.child.Child;
import com.discoverdev.nms.child.EnrollmentStatus;
import com.discoverdev.nms.child.GuardianLink;
import com.discoverdev.nms.child.GuardianLinkRepository;
import com.discoverdev.nms.media.AnnouncementAttachmentAccess;
import com.discoverdev.nms.tenant.TeacherAccessService;
import com.discoverdev.nms.user.Role;
import com.discoverdev.nms.user.User;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Whether an attachment is reachable through an announcement the reader can see.
 *
 * The rules mirror the read paths in {@link ParentAnnouncementService} and
 * {@link TeacherAnnouncementService} exactly — a nursery-wide post is for
 * everyone, a room post is for that room's teachers and its children's families.
 * If those rules ever change, this has to change with them, which is why it sits
 * next to them rather than inside the media package.
 */
@Component
public class AnnouncementAttachmentAccessImpl implements AnnouncementAttachmentAccess {

    private final AnnouncementRepository announcementRepository;
    private final GuardianLinkRepository guardianLinkRepository;
    private final TeacherAccessService teacherAccess;

    public AnnouncementAttachmentAccessImpl(
        AnnouncementRepository announcementRepository,
        GuardianLinkRepository guardianLinkRepository,
        TeacherAccessService teacherAccess
    ) {
        this.announcementRepository = announcementRepository;
        this.guardianLinkRepository = guardianLinkRepository;
        this.teacherAccess = teacherAccess;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isAttachedToAnnouncementVisibleTo(UUID mediaId, User requester) {
        if (requester.getNursery() == null) return false;
        UUID nurseryId = requester.getNursery().getId();

        List<Announcement> carriers = announcementRepository.findByAttachmentId(mediaId, nurseryId);
        if (carriers.isEmpty()) return false;

        // Resolved once, not per announcement — a parent with several children is
        // otherwise queried again for every post the file appears in.
        Set<UUID> parentRooms = requester.getRole() == Role.PARENT
            ? enrolledRoomIds(requester)
            : Set.of();

        return carriers.stream().anyMatch(a -> canRead(a, requester, parentRooms));
    }

    private boolean canRead(Announcement a, User requester, Set<UUID> parentRooms) {
        if (a.getScope() == AnnouncementScope.NURSERY) return true;
        if (a.getRoom() == null) return false;
        UUID roomId = a.getRoom().getId();
        return switch (requester.getRole()) {
            case TEACHER -> teacherAccess.canAccessRoom(requester, roomId);
            case PARENT -> parentRooms.contains(roomId);
            // Admins never reach here — MediaFileService lets them through first.
            case ADMIN, SUPER_ADMIN -> false;
        };
    }

    private Set<UUID> enrolledRoomIds(User parent) {
        Set<UUID> out = new HashSet<>();
        for (GuardianLink link : guardianLinkRepository
            .findByGuardianIdOrderByCreatedAtAsc(parent.getId())) {
            Child child = link.getChild();
            if (child.getEnrollmentStatus() == EnrollmentStatus.ENROLLED && child.getRoom() != null) {
                out.add(child.getRoom().getId());
            }
        }
        return out;
    }
}
