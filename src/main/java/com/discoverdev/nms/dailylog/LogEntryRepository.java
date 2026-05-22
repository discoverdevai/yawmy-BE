package com.discoverdev.nms.dailylog;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LogEntryRepository extends JpaRepository<LogEntry, UUID> {

    List<LogEntry> findByDailyLogIdOrderByEntryTimeAscCreatedAtAsc(UUID dailyLogId);

    List<LogEntry> findByDailyLogIdAndPublishedAtIsNotNullOrderByEntryTimeAscCreatedAtAsc(UUID dailyLogId);

    List<LogEntry> findByDailyLogIdAndPublishedAtIsNullOrderByEntryTimeAscCreatedAtAsc(UUID dailyLogId);

    long countByDailyLogIdAndPublishedAtIsNotNull(UUID dailyLogId);

    long countByDailyLogId(UUID dailyLogId);

    Optional<LogEntry> findByIdAndDailyLogId(UUID id, UUID dailyLogId);
}
