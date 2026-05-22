package com.discoverdev.nms.dailylog;

import com.discoverdev.nms.child.Child;
import com.discoverdev.nms.child.ChildRepository;
import com.discoverdev.nms.dailylog.dto.DailyLogResponse;
import com.discoverdev.nms.dailylog.dto.DailyLogSummaryResponse;
import com.discoverdev.nms.tenant.TenantContext;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AdminDailyLogService {

    private final DailyLogRepository dailyLogRepository;
    private final LogEntryRepository entryRepository;
    private final ChildRepository childRepository;

    public AdminDailyLogService(
        DailyLogRepository dailyLogRepository,
        LogEntryRepository entryRepository,
        ChildRepository childRepository
    ) {
        this.dailyLogRepository = dailyLogRepository;
        this.entryRepository = entryRepository;
        this.childRepository = childRepository;
    }

    @Transactional(readOnly = true)
    public List<DailyLogSummaryResponse> listForChild(UUID childId) {
        UUID nurseryId = TenantContext.currentNurseryId();
        Child child = childRepository.findByIdAndNurseryId(childId, nurseryId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Child not found"));
        return dailyLogRepository.findByChildIdOrderByLogDateDesc(child.getId()).stream()
            .map(log -> {
                long total = entryRepository.countByDailyLogId(log.getId());
                long published = entryRepository.countByDailyLogIdAndPublishedAtIsNotNull(log.getId());
                return DailyLogSummaryResponse.of(log, total, published);
            })
            .toList();
    }

    @Transactional(readOnly = true)
    public DailyLogResponse get(UUID logId) {
        UUID nurseryId = TenantContext.currentNurseryId();
        DailyLog log = dailyLogRepository.findByIdAndNurseryId(logId, nurseryId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Daily log not found"));
        return DailyLogResponse.of(
            log,
            entryRepository.findByDailyLogIdOrderByEntryTimeAscCreatedAtAsc(log.getId())
        );
    }
}
