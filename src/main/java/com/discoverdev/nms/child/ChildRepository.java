package com.discoverdev.nms.child;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChildRepository extends JpaRepository<Child, UUID> {

    Optional<Child> findByIdAndNurseryId(UUID id, UUID nurseryId);

    List<Child> findByNurseryIdOrderByFullNameAsc(UUID nurseryId);

    List<Child> findByNurseryIdAndEnrollmentStatusOrderByFullNameAsc(
        UUID nurseryId, EnrollmentStatus status);

    List<Child> findByNurseryIdAndRoomIdAndEnrollmentStatusOrderByFullNameAsc(
        UUID nurseryId, UUID roomId, EnrollmentStatus status);

    List<Child> findByNurseryIdAndRoomIdOrderByFullNameAsc(UUID nurseryId, UUID roomId);
}
