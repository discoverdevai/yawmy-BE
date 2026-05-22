package com.discoverdev.nms.dailylog;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DailyLogRepository extends JpaRepository<DailyLog, UUID> {

    Optional<DailyLog> findByChildIdAndLogDate(UUID childId, LocalDate date);

    Optional<DailyLog> findByIdAndNurseryId(UUID id, UUID nurseryId);

    List<DailyLog> findByChildIdAndLogDateBetweenOrderByLogDateDesc(
        UUID childId, LocalDate from, LocalDate to);

    List<DailyLog> findByChildIdOrderByLogDateDesc(UUID childId);

    List<DailyLog> findByNurseryIdAndLogDate(UUID nurseryId, LocalDate date);
}
