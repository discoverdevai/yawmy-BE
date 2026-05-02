package com.discoverdev.nms.nursery;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NurseryRepository extends JpaRepository<Nursery, UUID> {

    Optional<Nursery> findFirstByOrderByCreatedAtAsc();

    List<Nursery> findAllByOrderByNameAsc();
}