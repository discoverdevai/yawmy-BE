package com.discoverdev.nms.child;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChildHealthEntryRepository extends JpaRepository<ChildHealthEntry, UUID> {

    List<ChildHealthEntry> findByChildIdAndTypeOrderByCreatedAtAscIdAsc(UUID childId, HealthEntryType type);

    void deleteByChildIdAndType(UUID childId, HealthEntryType type);
}
