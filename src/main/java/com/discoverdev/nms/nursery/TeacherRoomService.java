package com.discoverdev.nms.nursery;

import com.discoverdev.nms.child.ChildRepository;
import com.discoverdev.nms.child.EnrollmentStatus;
import com.discoverdev.nms.nursery.dto.TeacherRoomResponse;
import com.discoverdev.nms.schedule.RoomSessionRepository;
import com.discoverdev.nms.schedule.dto.SessionResponse;
import com.discoverdev.nms.tenant.TeacherAccessService;
import com.discoverdev.nms.user.User;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * The rooms a teacher is assigned to, and their schedule.
 *
 * Every other teacher endpoint is keyed by roomId or childId; this is how the
 * app discovers those ids in the first place.
 */
@Service
public class TeacherRoomService {

    private final TeacherRoomAssignmentRepository assignmentRepository;
    private final ChildRepository childRepository;
    private final RoomSessionRepository sessionRepository;
    private final TeacherAccessService teacherAccess;

    public TeacherRoomService(
        TeacherRoomAssignmentRepository assignmentRepository,
        ChildRepository childRepository,
        RoomSessionRepository sessionRepository,
        TeacherAccessService teacherAccess
    ) {
        this.assignmentRepository = assignmentRepository;
        this.childRepository = childRepository;
        this.sessionRepository = sessionRepository;
        this.teacherAccess = teacherAccess;
    }

    /**
     * The signed-in teacher's rooms, oldest assignment first so the app's
     * "default to the first room" is stable between launches.
     *
     * An empty list is a normal state, not an error: a teacher who has just
     * activated their account has no rooms until an admin assigns them.
     */
    @Transactional(readOnly = true)
    public List<TeacherRoomResponse> myRooms(User teacher) {
        if (teacher.getNursery() == null) return List.of();

        UUID nurseryId = teacher.getNursery().getId();
        return assignmentRepository.findByTeacherIdOrderByCreatedAtAsc(teacher.getId())
            .stream()
            .map(TeacherRoomAssignment::getRoom)
            // Defensive: never leak a room from another nursery, and skip
            // archived ones so Home does not show a closed room.
            .filter(room -> room.getNursery() != null
                && nurseryId.equals(room.getNursery().getId())
                && room.getArchivedAt() == null)
            .map(room -> TeacherRoomResponse.of(
                room,
                childRepository.countByRoomIdAndEnrollmentStatus(room.getId(), EnrollmentStatus.ENROLLED)
            ))
            .toList();
    }

    /** Sessions scheduled in one of the teacher's rooms on a given date, earliest first. */
    @Transactional(readOnly = true)
    public List<SessionResponse> roomSessions(UUID roomId, LocalDate date, User teacher) {
        if (!teacherAccess.canAccessRoom(teacher, roomId)) {
            // 404 rather than 403: a teacher should not be able to probe which
            // room ids exist in other nurseries.
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found");
        }
        UUID nurseryId = teacher.getNursery().getId();
        return sessionRepository
            .findByNurseryIdAndRoomIdAndSessionDateOrderByStartTimeAsc(nurseryId, roomId, date)
            .stream()
            .map(SessionResponse::of)
            .toList();
    }
}
