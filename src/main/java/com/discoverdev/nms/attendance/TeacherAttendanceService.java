package com.discoverdev.nms.attendance;

import com.discoverdev.nms.attendance.dto.AttendanceEventResponse;
import com.discoverdev.nms.attendance.dto.ChildAttendanceResponse;
import com.discoverdev.nms.attendance.dto.ManualCheckRequest;
import com.discoverdev.nms.child.Child;
import com.discoverdev.nms.child.ChildRepository;
import com.discoverdev.nms.child.EnrollmentStatus;
import com.discoverdev.nms.nursery.Nursery;
import com.discoverdev.nms.nursery.NurseryRepository;
import com.discoverdev.nms.nursery.Room;
import com.discoverdev.nms.nursery.RoomRepository;
import com.discoverdev.nms.tenant.TeacherAccessService;
import com.discoverdev.nms.user.User;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class TeacherAttendanceService {

    private final AttendanceEventRepository eventRepository;
    private final ChildRepository childRepository;
    private final RoomRepository roomRepository;
    private final NurseryRepository nurseryRepository;
    private final TeacherAccessService teacherAccess;
    private final ApplicationEventPublisher events;

    public TeacherAttendanceService(
        AttendanceEventRepository eventRepository,
        ChildRepository childRepository,
        RoomRepository roomRepository,
        NurseryRepository nurseryRepository,
        TeacherAccessService teacherAccess,
        ApplicationEventPublisher events
    ) {
        this.eventRepository = eventRepository;
        this.childRepository = childRepository;
        this.roomRepository = roomRepository;
        this.nurseryRepository = nurseryRepository;
        this.teacherAccess = teacherAccess;
        this.events = events;
    }

    @Transactional
    public ChildAttendanceResponse checkIn(UUID childId, ManualCheckRequest req, User teacher) {
        return record(childId, req, teacher, AttendanceEventType.CHECK_IN);
    }

    @Transactional
    public ChildAttendanceResponse checkOut(UUID childId, ManualCheckRequest req, User teacher) {
        return record(childId, req, teacher, AttendanceEventType.CHECK_OUT);
    }

    private ChildAttendanceResponse record(
        UUID childId, ManualCheckRequest req, User teacher, AttendanceEventType type
    ) {
        Child child = requireAccessibleChild(childId, teacher);
        Nursery nursery = child.getNursery();
        Instant now = Instant.now();
        AttendanceEvent saved = eventRepository.save(AttendanceEvent.builder()
            .nursery(nursery)
            .child(child)
            .room(child.getRoom())
            .type(type)
            .source(AttendanceSource.TEACHER_MANUAL)
            .recordedAt(now)
            .recordedBy(teacher)
            .notes(req != null ? req.notes() : null)
            .build());
        events.publishEvent(new AttendanceEventRecordedEvent(
            nursery.getId(), child.getId(), saved.getId(),
            saved.getType(), saved.getSource(), saved.getRecordedAt(), teacher.getId()
        ));
        return getForDate(child, AttendanceDates.localDateOf(nursery, now));
    }

    @Transactional(readOnly = true)
    public List<ChildAttendanceResponse> roomRoster(UUID roomId, LocalDate date, User teacher) {
        if (!teacherAccess.canAccessRoom(teacher, roomId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found");
        }
        UUID nurseryId = teacher.getNursery().getId();
        Nursery nursery = nurseryRepository.findById(nurseryId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Nursery not found"));
        Room room = roomRepository.findByIdAndNurseryId(roomId, nurseryId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found"));
        List<Child> children = childRepository
            .findByNurseryIdAndRoomIdAndEnrollmentStatusOrderByFullNameAsc(
                nursery.getId(), room.getId(), EnrollmentStatus.ENROLLED);
        if (children.isEmpty()) return List.of();

        Instant from = AttendanceDates.startOfDay(nursery, date);
        Instant to = AttendanceDates.startOfNextDay(nursery, date);
        List<AttendanceEvent> dayEvents = eventRepository
            .findByRoomIdAndRecordedAtBetween(room.getId(), from, to);
        Map<UUID, List<AttendanceEvent>> byChild = groupByChild(dayEvents);

        List<ChildAttendanceResponse> out = new ArrayList<>();
        for (Child child : children) {
            List<AttendanceEvent> e = byChild.getOrDefault(child.getId(), List.of());
            out.add(ChildAttendanceResponse.of(child, date, e));
        }
        return out;
    }

    /**
     * One child's attendance for a date, with the day's events.
     *
     * The child profile needs this on its own rather than by filtering the room
     * roster — and it needs the events, since the card names who checked the
     * child in and when. Unlike the roster, these are ordered by the query, so
     * "the last event decides the status" is guaranteed rather than incidental.
     */
    @Transactional(readOnly = true)
    public ChildAttendanceResponse childAttendance(UUID childId, LocalDate date, User teacher) {
        return getForDate(requireAccessibleChild(childId, teacher), date);
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

    private Child requireAccessibleChild(UUID childId, User teacher) {
        Child child = childRepository.findById(childId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Child not found"));
        if (!teacherAccess.canAccessChild(teacher, child)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Child not found");
        }
        return child;
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
