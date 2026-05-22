package com.discoverdev.nms.dailylog;

import com.discoverdev.nms.child.Child;
import com.discoverdev.nms.child.EnrollmentStatus;
import com.discoverdev.nms.child.GuardianLink;
import com.discoverdev.nms.child.GuardianLinkRepository;
import com.discoverdev.nms.dailylog.dto.DailyLogResponse;
import com.discoverdev.nms.dailylog.dto.DailyLogSummaryResponse;
import com.discoverdev.nms.user.User;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ParentDailyLogService {

    private final DailyLogRepository dailyLogRepository;
    private final LogEntryRepository entryRepository;
    private final GuardianLinkRepository guardianLinkRepository;

    public ParentDailyLogService(
        DailyLogRepository dailyLogRepository,
        LogEntryRepository entryRepository,
        GuardianLinkRepository guardianLinkRepository
    ) {
        this.dailyLogRepository = dailyLogRepository;
        this.entryRepository = entryRepository;
        this.guardianLinkRepository = guardianLinkRepository;
    }

    @Transactional(readOnly = true)
    public Optional<DailyLogResponse> getByChildAndDate(UUID childId, LocalDate date, User parent) {
        Child child = requireLinkedEnrolledChild(childId, parent);
        return dailyLogRepository.findByChildIdAndLogDate(child.getId(), date)
            .map(log -> DailyLogResponse.of(
                log,
                entryRepository.findByDailyLogIdAndPublishedAtIsNotNullOrderByEntryTimeAscCreatedAtAsc(log.getId())
            ));
    }

    @Transactional(readOnly = true)
    public List<DailyLogSummaryResponse> history(UUID childId, LocalDate from, LocalDate to, User parent) {
        Child child = requireLinkedEnrolledChild(childId, parent);
        return dailyLogRepository.findByChildIdAndLogDateBetweenOrderByLogDateDesc(child.getId(), from, to).stream()
            .map(log -> {
                long published = entryRepository.countByDailyLogIdAndPublishedAtIsNotNull(log.getId());
                return DailyLogSummaryResponse.of(log, published, published);
            })
            .filter(s -> s.publishedEntries() > 0)
            .toList();
    }

    private Child requireLinkedEnrolledChild(UUID childId, User parent) {
        GuardianLink link = guardianLinkRepository.findByChildIdAndGuardianId(childId, parent.getId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Child not found"));
        Child child = link.getChild();
        if (child.getEnrollmentStatus() != EnrollmentStatus.ENROLLED) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Child not found");
        }
        return child;
    }
}
