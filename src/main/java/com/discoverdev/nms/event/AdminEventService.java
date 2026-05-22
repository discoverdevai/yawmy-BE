package com.discoverdev.nms.event;

import com.discoverdev.nms.announcement.AnnouncementScope;
import com.discoverdev.nms.event.dto.CreateEventRequest;
import com.discoverdev.nms.event.dto.EventResponse;
import com.discoverdev.nms.event.dto.UpdateEventRequest;
import com.discoverdev.nms.nursery.Nursery;
import com.discoverdev.nms.nursery.NurseryRepository;
import com.discoverdev.nms.nursery.Room;
import com.discoverdev.nms.nursery.RoomRepository;
import com.discoverdev.nms.tenant.TenantContext;
import com.discoverdev.nms.user.User;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AdminEventService {

    private final NurseryEventRepository repository;
    private final RoomRepository roomRepository;
    private final NurseryRepository nurseryRepository;
    private final ApplicationEventPublisher events;

    public AdminEventService(
        NurseryEventRepository repository,
        RoomRepository roomRepository,
        NurseryRepository nurseryRepository,
        ApplicationEventPublisher events
    ) {
        this.repository = repository;
        this.roomRepository = roomRepository;
        this.nurseryRepository = nurseryRepository;
        this.events = events;
    }

    @Transactional
    public EventResponse create(CreateEventRequest req, User author) {
        UUID nurseryId = TenantContext.currentNurseryId();
        Nursery nursery = nurseryRepository.findById(nurseryId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Nursery not found"));
        if (!req.endsAt().isAfter(req.startsAt())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "endsAt must be after startsAt");
        }
        Room room = null;
        if (req.scope() == AnnouncementScope.ROOM) {
            if (req.roomId() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "roomId is required for ROOM-scoped events");
            }
            room = roomRepository.findByIdAndNurseryId(req.roomId(), nurseryId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found"));
        }
        if (req.eventType() == EventType.RECURRING
            && (req.recurrenceRule() == null || req.recurrenceRule().isBlank())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "recurrenceRule required for RECURRING events");
        }
        NurseryEvent saved = repository.save(NurseryEvent.builder()
            .nursery(nursery)
            .author(author)
            .scope(req.scope())
            .room(room)
            .eventType(req.eventType())
            .title(req.title())
            .description(req.description())
            .location(req.location())
            .startsAt(req.startsAt())
            .endsAt(req.endsAt())
            .recurrenceRule(req.recurrenceRule())
            .build());
        events.publishEvent(new EventCreatedEvent(
            nurseryId, saved.getId(), saved.getScope(),
            room != null ? room.getId() : null, saved.getStartsAt(), author.getId()
        ));
        return EventResponse.of(saved, false, null);
    }

    @Transactional
    public EventResponse update(UUID id, UpdateEventRequest req) {
        NurseryEvent e = loadOwned(id);
        if (req.title() != null) e.setTitle(req.title());
        if (req.description() != null) e.setDescription(req.description());
        if (req.location() != null) e.setLocation(req.location());
        if (req.eventType() != null) e.setEventType(req.eventType());
        if (req.startsAt() != null) e.setStartsAt(req.startsAt());
        if (req.endsAt() != null) e.setEndsAt(req.endsAt());
        if (req.recurrenceRule() != null) e.setRecurrenceRule(req.recurrenceRule());
        if (!e.getEndsAt().isAfter(e.getStartsAt())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "endsAt must be after startsAt");
        }
        return EventResponse.of(repository.save(e), false, null);
    }

    @Transactional
    public void delete(UUID id) {
        repository.delete(loadOwned(id));
    }

    @Transactional(readOnly = true)
    public List<EventResponse> list(boolean upcomingOnly) {
        UUID nurseryId = TenantContext.currentNurseryId();
        List<NurseryEvent> items = upcomingOnly
            ? repository.findByNurseryIdAndStartsAtGreaterThanEqualOrderByStartsAtAsc(nurseryId, Instant.now())
            : repository.findByNurseryIdOrderByStartsAtAsc(nurseryId);
        return items.stream().map(e -> EventResponse.of(e, false, null)).toList();
    }

    @Transactional(readOnly = true)
    public EventResponse get(UUID id) {
        return EventResponse.of(loadOwned(id), false, null);
    }

    private NurseryEvent loadOwned(UUID id) {
        UUID nurseryId = TenantContext.currentNurseryId();
        return repository.findByIdAndNurseryId(id, nurseryId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found"));
    }
}
