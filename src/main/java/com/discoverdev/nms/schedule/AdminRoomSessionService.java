package com.discoverdev.nms.schedule;

import com.discoverdev.nms.nursery.Nursery;
import com.discoverdev.nms.nursery.NurseryRepository;
import com.discoverdev.nms.nursery.Room;
import com.discoverdev.nms.nursery.RoomRepository;
import com.discoverdev.nms.schedule.dto.CreateSessionRequest;
import com.discoverdev.nms.schedule.dto.SessionResponse;
import com.discoverdev.nms.schedule.dto.UpdateSessionRequest;
import com.discoverdev.nms.tenant.TenantContext;
import com.discoverdev.nms.user.Role;
import com.discoverdev.nms.user.User;
import com.discoverdev.nms.user.UserRepository;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AdminRoomSessionService {

    private final RoomSessionRepository sessionRepository;
    private final RoomRepository roomRepository;
    private final NurseryRepository nurseryRepository;
    private final UserRepository userRepository;

    public AdminRoomSessionService(
        RoomSessionRepository sessionRepository,
        RoomRepository roomRepository,
        NurseryRepository nurseryRepository,
        UserRepository userRepository
    ) {
        this.sessionRepository = sessionRepository;
        this.roomRepository = roomRepository;
        this.nurseryRepository = nurseryRepository;
        this.userRepository = userRepository;
    }

    /** Sessions for a room on one date, ordered by start time. */
    @Transactional(readOnly = true)
    public List<SessionResponse> list(UUID roomId, LocalDate date) {
        UUID nurseryId = TenantContext.currentNurseryId();
        loadRoom(roomId, nurseryId);
        if (date == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "date is required");
        }
        return sessionRepository
            .findByNurseryIdAndRoomIdAndSessionDateOrderByStartTimeAsc(nurseryId, roomId, date)
            .stream()
            .map(SessionResponse::of)
            .toList();
    }

    @Transactional
    public SessionResponse create(UUID roomId, CreateSessionRequest req) {
        UUID nurseryId = TenantContext.currentNurseryId();
        Room room = loadRoom(roomId, nurseryId);
        if (room.getArchivedAt() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot add sessions to an archived room");
        }
        assertTimeOrder(req.startTime(), req.endTime());
        User teacher = loadTeacher(req.teacherId(), nurseryId);
        Nursery nursery = nurseryRepository.getReferenceById(nurseryId);

        RoomSession session = RoomSession.builder()
            .nursery(nursery)
            .room(room)
            .teacher(teacher)
            .name(req.name())
            .sessionDate(req.date())
            .startTime(req.startTime())
            .endTime(req.endTime())
            .build();
        return SessionResponse.of(sessionRepository.save(session));
    }

    @Transactional
    public SessionResponse update(UUID roomId, UUID sessionId, UpdateSessionRequest req) {
        UUID nurseryId = TenantContext.currentNurseryId();
        loadRoom(roomId, nurseryId);
        RoomSession session = loadSession(sessionId, roomId, nurseryId);

        if (req.name() != null) session.setName(req.name());
        if (req.date() != null) session.setSessionDate(req.date());
        if (req.startTime() != null) session.setStartTime(req.startTime());
        if (req.endTime() != null) session.setEndTime(req.endTime());
        assertTimeOrder(session.getStartTime(), session.getEndTime());
        if (req.teacherId() != null) session.setTeacher(loadTeacher(req.teacherId(), nurseryId));

        return SessionResponse.of(sessionRepository.save(session));
    }

    @Transactional
    public void delete(UUID roomId, UUID sessionId) {
        UUID nurseryId = TenantContext.currentNurseryId();
        loadRoom(roomId, nurseryId);
        RoomSession session = loadSession(sessionId, roomId, nurseryId);
        sessionRepository.delete(session);
    }

    private void assertTimeOrder(LocalTime start, LocalTime end) {
        if (start != null && end != null && !end.isAfter(start)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "End time must be after the start time");
        }
    }

    private Room loadRoom(UUID roomId, UUID nurseryId) {
        return roomRepository.findByIdAndNurseryId(roomId, nurseryId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found"));
    }

    private RoomSession loadSession(UUID sessionId, UUID roomId, UUID nurseryId) {
        RoomSession session = sessionRepository.findByIdAndNurseryId(sessionId, nurseryId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found"));
        if (!session.getRoom().getId().equals(roomId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found");
        }
        return session;
    }

    private User loadTeacher(UUID teacherId, UUID nurseryId) {
        User user = userRepository.findById(teacherId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Teacher not found"));
        if (user.getNursery() == null || !nurseryId.equals(user.getNursery().getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Teacher not found");
        }
        if (user.getRole() != Role.TEACHER) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User is not a teacher");
        }
        return user;
    }
}
