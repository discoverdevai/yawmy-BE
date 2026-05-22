package com.discoverdev.nms.tenant;

import com.discoverdev.nms.child.Child;
import com.discoverdev.nms.nursery.TeacherRoomAssignmentRepository;
import com.discoverdev.nms.user.User;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class TeacherAccessService {

    private final TeacherRoomAssignmentRepository assignmentRepository;

    public TeacherAccessService(TeacherRoomAssignmentRepository assignmentRepository) {
        this.assignmentRepository = assignmentRepository;
    }

    public boolean canAccessChild(User teacher, Child child) {
        if (teacher.getNursery() == null || child.getNursery() == null) return false;
        if (!teacher.getNursery().getId().equals(child.getNursery().getId())) return false;
        if (child.getRoom() == null) return false;
        return assignmentRepository.existsByTeacherIdAndRoomId(teacher.getId(), child.getRoom().getId());
    }

    public boolean canAccessRoom(User teacher, UUID roomId) {
        if (teacher.getNursery() == null) return false;
        return assignmentRepository.existsByTeacherIdAndRoomId(teacher.getId(), roomId);
    }
}
