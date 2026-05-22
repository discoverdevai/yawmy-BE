package com.discoverdev.nms.event;

import com.discoverdev.nms.announcement.AnnouncementScope;
import com.discoverdev.nms.child.Child;
import com.discoverdev.nms.child.EnrollmentStatus;
import com.discoverdev.nms.child.GuardianLink;
import com.discoverdev.nms.child.GuardianLinkRepository;
import com.discoverdev.nms.event.dto.EventResponse;
import com.discoverdev.nms.user.User;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ParentEventService {

    private final NurseryEventRepository repository;
    private final EventSaveRepository saveRepository;
    private final GuardianLinkRepository guardianLinkRepository;

    public ParentEventService(
        NurseryEventRepository repository,
        EventSaveRepository saveRepository,
        GuardianLinkRepository guardianLinkRepository
    ) {
        this.repository = repository;
        this.saveRepository = saveRepository;
        this.guardianLinkRepository = guardianLinkRepository;
    }

    @Transactional(readOnly = true)
    public List<EventResponse> list(boolean upcomingOnly, User parent) {
        UUID nurseryId = parent.getNursery().getId();
        Set<UUID> roomIds = enrolledRoomIds(parent);
        List<NurseryEvent> items;
        if (roomIds.isEmpty()) {
            List<NurseryEvent> all = upcomingOnly
                ? repository.findByNurseryIdAndStartsAtGreaterThanEqualOrderByStartsAtAsc(nurseryId, Instant.now())
                : repository.findByNurseryIdOrderByStartsAtAsc(nurseryId);
            items = all.stream().filter(e -> e.getScope() == AnnouncementScope.NURSERY).toList();
        } else if (upcomingOnly) {
            items = repository.findUpcomingForRooms(nurseryId, roomIds, Instant.now());
        } else {
            items = repository.findVisibleToRooms(nurseryId, roomIds);
        }
        return items.stream().map(e -> toResponse(e, parent)).toList();
    }

    @Transactional
    public EventResponse save(UUID eventId, User parent) {
        NurseryEvent event = loadAccessible(eventId, parent);
        if (!saveRepository.existsByEventIdAndUserId(eventId, parent.getId())) {
            saveRepository.save(EventSave.builder().event(event).user(parent).build());
        }
        return toResponse(event, parent);
    }

    @Transactional
    public void unsave(UUID eventId, User parent) {
        loadAccessible(eventId, parent);
        saveRepository.deleteByEventIdAndUserId(eventId, parent.getId());
    }

    @Transactional(readOnly = true)
    public List<EventResponse> savedTab(User parent) {
        return saveRepository.findByUserIdOrderBySavedAtDesc(parent.getId()).stream()
            .map(EventSave::getEvent)
            .filter(e -> e.getNursery().getId().equals(parent.getNursery().getId()))
            .map(e -> toResponse(e, parent))
            .toList();
    }

    private NurseryEvent loadAccessible(UUID id, User parent) {
        UUID nurseryId = parent.getNursery().getId();
        NurseryEvent e = repository.findByIdAndNurseryId(id, nurseryId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found"));
        if (e.getScope() == AnnouncementScope.ROOM) {
            Set<UUID> roomIds = enrolledRoomIds(parent);
            if (e.getRoom() == null || !roomIds.contains(e.getRoom().getId())) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found");
            }
        }
        return e;
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

    private EventResponse toResponse(NurseryEvent e, User parent) {
        EventSave mine = saveRepository
            .findByEventIdAndUserId(e.getId(), parent.getId())
            .orElse(null);
        return EventResponse.of(e, mine != null, mine != null ? mine.getSavedAt() : null);
    }
}
