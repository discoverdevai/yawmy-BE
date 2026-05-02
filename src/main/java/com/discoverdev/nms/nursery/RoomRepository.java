package com.discoverdev.nms.nursery;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomRepository extends JpaRepository<Room, UUID> {

    List<Room> findByNurseryIdAndArchivedAtIsNullOrderByNameAsc(UUID nurseryId);

    List<Room> findByNurseryIdOrderByNameAsc(UUID nurseryId);

    Optional<Room> findByIdAndNurseryId(UUID id, UUID nurseryId);

    boolean existsByNurseryIdAndNameIgnoreCaseAndArchivedAtIsNull(UUID nurseryId, String name);
}