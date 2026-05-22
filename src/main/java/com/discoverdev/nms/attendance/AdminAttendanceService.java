package com.discoverdev.nms.attendance;

import com.discoverdev.nms.attendance.dto.AttendanceDaySummary;
import com.discoverdev.nms.attendance.dto.ChildAttendanceResponse;
import com.discoverdev.nms.attendance.dto.ManualCheckRequest;
import com.discoverdev.nms.child.Child;
import com.discoverdev.nms.child.ChildRepository;
import com.discoverdev.nms.child.EnrollmentStatus;
import com.discoverdev.nms.nursery.Nursery;
import com.discoverdev.nms.nursery.NurseryRepository;
import com.discoverdev.nms.tenant.TenantContext;
import com.discoverdev.nms.user.User;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
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
public class AdminAttendanceService {

    private final AttendanceEventRepository eventRepository;
    private final ChildRepository childRepository;
    private final NurseryRepository nurseryRepository;
    private final ApplicationEventPublisher events;

    public AdminAttendanceService(
        AttendanceEventRepository eventRepository,
        ChildRepository childRepository,
        NurseryRepository nurseryRepository,
        ApplicationEventPublisher events
    ) {
        this.eventRepository = eventRepository;
        this.childRepository = childRepository;
        this.nurseryRepository = nurseryRepository;
        this.events = events;
    }

    @Transactional
    public ChildAttendanceResponse checkIn(UUID childId, ManualCheckRequest req, User admin) {
        return record(childId, req, admin, AttendanceEventType.CHECK_IN);
    }

    @Transactional
    public ChildAttendanceResponse checkOut(UUID childId, ManualCheckRequest req, User admin) {
        return record(childId, req, admin, AttendanceEventType.CHECK_OUT);
    }

    private ChildAttendanceResponse record(
        UUID childId, ManualCheckRequest req, User admin, AttendanceEventType type
    ) {
        UUID nurseryId = TenantContext.currentNurseryId();
        Child child = childRepository.findByIdAndNurseryId(childId, nurseryId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Child not found"));
        Nursery nursery = child.getNursery();
        Instant now = Instant.now();
        AttendanceEvent saved = eventRepository.save(AttendanceEvent.builder()
            .nursery(nursery)
            .child(child)
            .room(child.getRoom())
            .type(type)
            .source(AttendanceSource.ADMIN_MANUAL)
            .recordedAt(now)
            .recordedBy(admin)
            .notes(req != null ? req.notes() : null)
            .build());
        events.publishEvent(new AttendanceEventRecordedEvent(
            nursery.getId(), child.getId(), saved.getId(),
            saved.getType(), saved.getSource(), saved.getRecordedAt(), admin.getId()
        ));
        return loadForDate(child, AttendanceDates.localDateOf(nursery, now));
    }

    @Transactional(readOnly = true)
    public List<ChildAttendanceResponse> nurseryRoster(LocalDate date, UUID roomFilter) {
        UUID nurseryId = TenantContext.currentNurseryId();
        Nursery nursery = nurseryRepository.findById(nurseryId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Nursery not found"));
        List<Child> children;
        if (roomFilter != null) {
            children = childRepository.findByNurseryIdAndRoomIdAndEnrollmentStatusOrderByFullNameAsc(
                nurseryId, roomFilter, EnrollmentStatus.ENROLLED);
        } else {
            children = childRepository.findByNurseryIdAndEnrollmentStatusOrderByFullNameAsc(
                nurseryId, EnrollmentStatus.ENROLLED);
        }
        if (children.isEmpty()) return List.of();

        Instant from = AttendanceDates.startOfDay(nursery, date);
        Instant to = AttendanceDates.startOfNextDay(nursery, date);
        List<AttendanceEvent> dayEvents = eventRepository
            .findByNurseryIdAndRecordedAtBetween(nurseryId, from, to);
        Map<UUID, List<AttendanceEvent>> byChild = groupByChild(dayEvents);

        List<ChildAttendanceResponse> out = new ArrayList<>();
        for (Child child : children) {
            List<AttendanceEvent> e = byChild.getOrDefault(child.getId(), List.of());
            out.add(ChildAttendanceResponse.of(child, date, e));
        }
        return out;
    }

    @Transactional(readOnly = true)
    public List<AttendanceDaySummary> childHistory(UUID childId, LocalDate from, LocalDate to) {
        UUID nurseryId = TenantContext.currentNurseryId();
        Child child = childRepository.findByIdAndNurseryId(childId, nurseryId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Child not found"));
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

    private ChildAttendanceResponse loadForDate(Child child, LocalDate date) {
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

    private Map<UUID, List<AttendanceEvent>> groupByChild(List<AttendanceEvent> events) {
        Map<UUID, List<AttendanceEvent>> map = new HashMap<>();
        for (AttendanceEvent e : events) {
            map.computeIfAbsent(e.getChild().getId(), k -> new ArrayList<>()).add(e);
        }
        for (List<AttendanceEvent> list : map.values()) {
            list.sort((a, b) -> a.getRecordedAt().compareTo(b.getRecordedAt()));
        }
        return map;
    }
}
