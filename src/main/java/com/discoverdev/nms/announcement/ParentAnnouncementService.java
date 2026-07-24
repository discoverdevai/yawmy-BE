package com.discoverdev.nms.announcement;

import com.discoverdev.nms.announcement.dto.AnnouncementResponse;
import com.discoverdev.nms.child.Child;
import com.discoverdev.nms.child.EnrollmentStatus;
import com.discoverdev.nms.child.GuardianLink;
import com.discoverdev.nms.child.GuardianLinkRepository;
import com.discoverdev.nms.user.User;
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

@Service
public class ParentAnnouncementService {

    private final AnnouncementRepository repository;
    private final AnnouncementSeenRepository seenRepository;
    private final GuardianLinkRepository guardianLinkRepository;
    private final ApplicationEventPublisher events;

    public ParentAnnouncementService(
        AnnouncementRepository repository,
        AnnouncementSeenRepository seenRepository,
        GuardianLinkRepository guardianLinkRepository,
        ApplicationEventPublisher events
    ) {
        this.repository = repository;
        this.seenRepository = seenRepository;
        this.guardianLinkRepository = guardianLinkRepository;
        this.events = events;
    }

    /** Combined feed: nursery-wide announcements plus those for the parent's children's rooms. */
    @Transactional(readOnly = true)
    public List<AnnouncementResponse> listForParent(User parent) {
        UUID nurseryId = parent.getNursery().getId();
        Set<UUID> roomIds = enrolledRoomIds(parent);
        List<Announcement> items = roomIds.isEmpty()
            ? repository.findByNurseryIdOrderByCreatedAtDesc(nurseryId).stream()
                .filter(a -> a.getScope() == AnnouncementScope.NURSERY)
                .toList()
            : repository.findVisibleToRooms(nurseryId, roomIds);
        return items.stream().map(a -> toResponse(a, parent)).toList();
    }

    /** Nursery-wide announcements only. */
    @Transactional(readOnly = true)
    public List<AnnouncementResponse> listNurseryWide(User parent) {
        UUID nurseryId = parent.getNursery().getId();
        return repository.findByNurseryIdOrderByCreatedAtDesc(nurseryId).stream()
            .filter(a -> a.getScope() == AnnouncementScope.NURSERY)
            .map(a -> toResponse(a, parent))
            .toList();
    }

    /** Room-scoped announcements for the parent's children's rooms only. */
    @Transactional(readOnly = true)
    public List<AnnouncementResponse> listForRooms(User parent) {
        UUID nurseryId = parent.getNursery().getId();
        Set<UUID> roomIds = enrolledRoomIds(parent);
        if (roomIds.isEmpty()) return List.of();
        return repository.findVisibleToRooms(nurseryId, roomIds).stream()
            .filter(a -> a.getScope() == AnnouncementScope.ROOM)
            .map(a -> toResponse(a, parent))
            .toList();
    }

    @Transactional(readOnly = true)
    public AnnouncementResponse get(UUID id, User parent) {
        return toResponse(loadAccessible(id, parent), parent);
    }

    /** Parent opened the announcement — record a one-per-user "seen" (idempotent). */
    @Transactional
    public AnnouncementResponse markSeen(UUID id, User parent) {
        Announcement ann = loadAccessible(id, parent);
        if (!seenRepository.existsByAnnouncementIdAndUserId(ann.getId(), parent.getId())) {
            seenRepository.save(AnnouncementSeen.builder()
                .announcement(ann)
                .user(parent)
                .seenAt(Instant.now())
                .build());
        }
        return toResponse(ann, parent);
    }

    private Announcement loadAccessible(UUID id, User parent) {
        UUID nurseryId = parent.getNursery().getId();
        Announcement ann = repository.findByIdAndNurseryId(id, nurseryId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Announcement not found"));
        if (ann.getScope() == AnnouncementScope.ROOM) {
            Set<UUID> roomIds = enrolledRoomIds(parent);
            if (ann.getRoom() == null || !roomIds.contains(ann.getRoom().getId())) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Announcement not found");
            }
        }
        return ann;
    }

    private Set<UUID> enrolledRoomIds(User parent) {
        Set<UUID> out = new HashSet<>();
        for (GuardianLink link : guardianLinkRepository.findByGuardianIdOrderByCreatedAtAsc(parent.getId())) {
            Child child = link.getChild();
            if (child.getEnrollmentStatus() == EnrollmentStatus.ENROLLED && child.getRoom() != null) {
                out.add(child.getRoom().getId());
            }
        }
        return out;
    }

    private AnnouncementResponse toResponse(Announcement a, User parent) {
        long count = seenRepository.countByAnnouncementId(a.getId());
        AnnouncementSeen mine = seenRepository
            .findByAnnouncementIdAndUserId(a.getId(), parent.getId())
            .orElse(null);
        return AnnouncementResponse.of(a, count, mine != null,
            mine != null ? mine.getSeenAt() : null);
    }
}
