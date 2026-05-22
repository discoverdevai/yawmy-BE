package com.discoverdev.nms.nursery;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeacherRoomAssignmentRepository extends JpaRepository<TeacherRoomAssignment, UUID> {

    List<TeacherRoomAssignment> findByRoomIdOrderByCreatedAtAsc(UUID roomId);

    List<TeacherRoomAssignment> findByTeacherIdOrderByCreatedAtAsc(UUID teacherId);

    boolean existsByTeacherIdAndRoomId(UUID teacherId, UUID roomId);

    void deleteByTeacherIdAndRoomId(UUID teacherId, UUID roomId);

    long deleteByRoomId(UUID roomId);

    long deleteByTeacherId(UUID teacherId);
}
