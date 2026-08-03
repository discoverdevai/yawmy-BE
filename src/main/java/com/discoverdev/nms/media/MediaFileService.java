package com.discoverdev.nms.media;

import com.discoverdev.nms.child.Child;
import com.discoverdev.nms.child.GuardianLinkRepository;
import com.discoverdev.nms.nursery.TeacherRoomAssignmentRepository;
import com.discoverdev.nms.user.Role;
import com.discoverdev.nms.user.User;
import java.io.IOException;
import java.util.UUID;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class MediaFileService {

    private final MediaItemRepository repository;
    private final MediaStorage storage;
    private final GuardianLinkRepository guardianLinkRepository;
    private final TeacherRoomAssignmentRepository teacherRoomRepository;
    private final ChatAttachmentAccess chatAttachmentAccess;
    private final AnnouncementAttachmentAccess announcementAttachmentAccess;

    public MediaFileService(
        MediaItemRepository repository,
        MediaStorage storage,
        GuardianLinkRepository guardianLinkRepository,
        TeacherRoomAssignmentRepository teacherRoomRepository,
        ChatAttachmentAccess chatAttachmentAccess,
        AnnouncementAttachmentAccess announcementAttachmentAccess
    ) {
        this.repository = repository;
        this.storage = storage;
        this.guardianLinkRepository = guardianLinkRepository;
        this.teacherRoomRepository = teacherRoomRepository;
        this.chatAttachmentAccess = chatAttachmentAccess;
        this.announcementAttachmentAccess = announcementAttachmentAccess;
    }

    @Transactional(readOnly = true)
    public StreamedFile loadFor(UUID mediaId, User requester) {
        MediaItem item = repository.findByIdAndDeletedAtIsNull(mediaId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Media not found"));
        if (!canSee(item, requester)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Media not found");
        }
        try {
            Resource resource = storage.load(item.getStorageKey());
            return new StreamedFile(resource, item.getContentType());
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to read file", e);
        }
    }

    private boolean canSee(MediaItem item, User requester) {
        if (requester.getNursery() == null) return false;
        UUID requesterNursery = requester.getNursery().getId();
        UUID itemNursery = item.getNursery().getId();
        if (!requesterNursery.equals(itemNursery)) return false;

        Role role = requester.getRole();
        if (role == Role.ADMIN) return true;

        // Chat and announcement attachments carry no child and no room, so the
        // room-scoped rules below have nothing to match and would deny every
        // teacher and parent. What the file hangs off decides instead: the thread
        // it was sent in, or the announcement it was posted with.
        if (item.getChild() == null && item.getRoom() == null) {
            if (chatAttachmentAccess.isAttachedToThreadVisibleTo(item.getId(), requester.getId())) {
                return true;
            }
            if (announcementAttachmentAccess
                .isAttachedToAnnouncementVisibleTo(item.getId(), requester)) {
                return true;
            }
        }

        if (role == Role.TEACHER) {
            UUID scopeRoomId = item.getChild() != null
                ? (item.getChild().getRoom() != null ? item.getChild().getRoom().getId() : null)
                : (item.getRoom() != null ? item.getRoom().getId() : null);
            return scopeRoomId != null
                && teacherRoomRepository.existsByTeacherIdAndRoomId(requester.getId(), scopeRoomId);
        }

        if (role == Role.PARENT) {
            Child child = item.getChild();
            if (child != null) {
                return guardianLinkRepository.existsByChildIdAndGuardianId(child.getId(), requester.getId());
            }
            if (item.getRoom() != null) {
                return guardianLinkRepository.findByGuardianIdOrderByCreatedAtAsc(requester.getId()).stream()
                    .map(link -> link.getChild())
                    .anyMatch(c -> c.getRoom() != null
                        && c.getRoom().getId().equals(item.getRoom().getId()));
            }
        }
        return false;
    }

    public record StreamedFile(Resource resource, String contentType) {}
}
