package com.discoverdev.nms.admin;

import com.discoverdev.nms.admin.dto.TeacherAssignmentResponse;
import com.discoverdev.nms.nursery.Room;
import com.discoverdev.nms.nursery.RoomRepository;
import com.discoverdev.nms.nursery.TeacherRoomAssignment;
import com.discoverdev.nms.nursery.TeacherRoomAssignmentRepository;
import com.discoverdev.nms.tenant.TenantContext;
import com.discoverdev.nms.user.Role;
import com.discoverdev.nms.user.User;
import com.discoverdev.nms.user.UserRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AdminTeacherAssignmentService {

    private final TeacherRoomAssignmentRepository assignmentRepository;
    private final RoomRepository roomRepository;
    private final UserRepository userRepository;

    public AdminTeacherAssignmentService(
        TeacherRoomAssignmentRepository assignmentRepository,
        RoomRepository roomRepository,
        UserRepository userRepository
    ) {
        this.assignmentRepository = assignmentRepository;
        this.roomRepository = roomRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public TeacherAssignmentResponse assign(UUID roomId, UUID teacherId) {
        UUID nurseryId = TenantContext.currentNurseryId();
        Room room = loadRoom(roomId, nurseryId);
        if (room.getArchivedAt() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot assign to an archived room");
        }
        User teacher = loadTeacher(teacherId, nurseryId);
        if (assignmentRepository.existsByTeacherIdAndRoomId(teacherId, roomId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Teacher is already assigned to this room");
        }
        TeacherRoomAssignment saved = assignmentRepository.save(
            TeacherRoomAssignment.builder().teacher(teacher).room(room).build()
        );
        return TeacherAssignmentResponse.of(saved);
    }

    @Transactional
    public void unassign(UUID roomId, UUID teacherId) {
        UUID nurseryId = TenantContext.currentNurseryId();
        loadRoom(roomId, nurseryId);
        loadTeacher(teacherId, nurseryId);
        if (!assignmentRepository.existsByTeacherIdAndRoomId(teacherId, roomId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Assignment not found");
        }
        assignmentRepository.deleteByTeacherIdAndRoomId(teacherId, roomId);
    }

    @Transactional(readOnly = true)
    public List<TeacherAssignmentResponse> listByRoom(UUID roomId) {
        UUID nurseryId = TenantContext.currentNurseryId();
        loadRoom(roomId, nurseryId);
        return assignmentRepository.findByRoomIdOrderByCreatedAtAsc(roomId).stream()
            .map(TeacherAssignmentResponse::of)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<TeacherAssignmentResponse> listByTeacher(UUID teacherId) {
        UUID nurseryId = TenantContext.currentNurseryId();
        loadTeacher(teacherId, nurseryId);
        return assignmentRepository.findByTeacherIdOrderByCreatedAtAsc(teacherId).stream()
            .map(TeacherAssignmentResponse::of)
            .toList();
    }

    private Room loadRoom(UUID roomId, UUID nurseryId) {
        return roomRepository.findByIdAndNurseryId(roomId, nurseryId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found"));
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
