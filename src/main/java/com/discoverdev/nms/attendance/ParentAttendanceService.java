package com.discoverdev.nms.attendance;

import com.discoverdev.nms.attendance.dto.AttendanceDaySummary;
import com.discoverdev.nms.attendance.dto.ChildAttendanceResponse;
import com.discoverdev.nms.attendance.dto.NurseryInfoResponse;
import com.discoverdev.nms.attendance.dto.ParentCheckRequest;
import com.discoverdev.nms.child.Child;
import com.discoverdev.nms.child.EnrollmentStatus;
import com.discoverdev.nms.child.GuardianLink;
import com.discoverdev.nms.child.GuardianLinkRepository;
import com.discoverdev.nms.nursery.Nursery;
import com.discoverdev.nms.nursery.NurseryRepository;
import com.discoverdev.nms.user.User;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ParentAttendanceService {

    private final AttendanceEventRepository eventRepository;
    private final GuardianLinkRepository guardianLinkRepository;
    private final NurseryRepository nurseryRepository;
    private final GeofenceService geofenceService;
    private final ApplicationEventPublisher events;

    public ParentAttendanceService(
        AttendanceEventRepository eventRepository,
        GuardianLinkRepository guardianLinkRepository,
        NurseryRepository nurseryRepository,
        GeofenceService geofenceService,
        ApplicationEventPublisher events
    ) {
        this.eventRepository = eventRepository;
        this.guardianLinkRepository = guardianLinkRepository;
        this.nurseryRepository = nurseryRepository;
        this.geofenceService = geofenceService;
        this.events = events;
    }

    @Transactional
    public ChildAttendanceResponse checkIn(UUID childId, ParentCheckRequest req, User parent) {
        return record(childId, req, parent, AttendanceEventType.CHECK_IN);
    }

    @Transactional
    public ChildAttendanceResponse checkOut(UUID childId, ParentCheckRequest req, User parent) {
        return record(childId, req, parent, AttendanceEventType.CHECK_OUT);
    }

    private ChildAttendanceResponse record(
        UUID childId, ParentCheckRequest req, User parent, AttendanceEventType type
    ) {
        Child child = requireLinkedEnrolledChild(childId, parent);
        Nursery nursery = child.getNursery();
        if (!geofenceService.isConfigured(nursery)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Nursery has not configured geofence");
        }
        if (!geofenceService.isWithin(nursery, req.lat(), req.lng())) {
            double dist = geofenceService.distanceMeters(nursery, req.lat(), req.lng());
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                String.format("Outside nursery geofence (%.0f m away)", dist)
            );
        }
        Instant now = Instant.now();
        AttendanceEvent saved = eventRepository.save(AttendanceEvent.builder()
            .nursery(nursery)
            .child(child)
            .room(child.getRoom())
            .type(type)
            .source(AttendanceSource.PARENT_APP)
            .recordedAt(now)
            .recordedBy(parent)
            .lat(req.lat())
            .lng(req.lng())
            .accuracyM(req.accuracyM())
            .build());
        events.publishEvent(new AttendanceEventRecordedEvent(
            nursery.getId(), child.getId(), saved.getId(),
            saved.getType(), saved.getSource(), saved.getRecordedAt(), parent.getId()
        ));
        return getForDate(child, AttendanceDates.localDateOf(nursery, now));
    }

    @Transactional(readOnly = true)
    public ChildAttendanceResponse getForDate(UUID childId, LocalDate date, User parent) {
        Child child = requireLinkedEnrolledChild(childId, parent);
        return getForDate(child, date);
    }

    @Transactional(readOnly = true)
    public List<AttendanceDaySummary> history(
        UUID childId, LocalDate from, LocalDate to, User parent
    ) {
        Child child = requireLinkedEnrolledChild(childId, parent);
        Nursery nursery = child.getNursery();
        List<AttendanceEvent> events = eventRepository
            .findByChildIdAndRecordedAtBetweenOrderByRecordedAtAsc(
                child.getId(),
                AttendanceDates.startOfDay(nursery, from),
                AttendanceDates.endOfRangeInclusive(nursery, to)
            );
        Map<LocalDate, List<AttendanceEvent>> byDate = new LinkedHashMap<>();
        for (AttendanceEvent e : events) {
            LocalDate d = AttendanceDates.localDateOf(nursery, e.getRecordedAt());
            byDate.computeIfAbsent(d, k -> new ArrayList<>()).add(e);
        }
        List<AttendanceDaySummary> out = new ArrayList<>();
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
            List<AttendanceEvent> dayEvents = byDate.getOrDefault(d, List.of());
            out.add(summarize(d, dayEvents));
        }
        return out;
    }

    @Transactional(readOnly = true)
    public NurseryInfoResponse getNurseryInfo(User parent) {
        if (parent.getNursery() == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Nursery not found");
        }
        UUID nurseryId = parent.getNursery().getId();
        Nursery nursery = nurseryRepository.findById(nurseryId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Nursery not found"));
        return NurseryInfoResponse.of(nursery);
    }

    private ChildAttendanceResponse getForDate(Child child, LocalDate date) {
        Nursery nursery = child.getNursery();
        List<AttendanceEvent> dayEvents = eventRepository
            .findByChildIdAndRecordedAtBetweenOrderByRecordedAtAsc(
                child.getId(),
                AttendanceDates.startOfDay(nursery, date),
                AttendanceDates.startOfNextDay(nursery, date)
            );
        return ChildAttendanceResponse.of(child, date, dayEvents);
    }

    private AttendanceDaySummary summarize(LocalDate date, List<AttendanceEvent> events) {
        Instant lastIn = null;
        Instant lastOut = null;
        AttendanceStatus status = AttendanceStatus.NOT_ARRIVED;
        for (AttendanceEvent e : events) {
            if (e.getType() == AttendanceEventType.CHECK_IN) {
                lastIn = e.getRecordedAt();
                status = AttendanceStatus.PRESENT;
            } else if (e.getType() == AttendanceEventType.CHECK_OUT) {
                lastOut = e.getRecordedAt();
                status = AttendanceStatus.CHECKED_OUT;
            }
        }
        return new AttendanceDaySummary(date, status, lastIn, lastOut);
    }

    private Child requireLinkedEnrolledChild(UUID childId, User parent) {
        GuardianLink link = guardianLinkRepository.findByChildIdAndGuardianId(childId, parent.getId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Child not found"));
        Child child = link.getChild();
        if (child.getEnrollmentStatus() != EnrollmentStatus.ENROLLED) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Child not found");
        }
        return child;
    }
}
