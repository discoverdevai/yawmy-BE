package com.discoverdev.nms.dailylog;

import com.discoverdev.nms.child.Child;
import com.discoverdev.nms.child.ChildRepository;
import com.discoverdev.nms.dailylog.dto.CreateLogEntryRequest;
import com.discoverdev.nms.dailylog.dto.DailyLogResponse;
import com.discoverdev.nms.dailylog.dto.DailyLogSummaryResponse;
import com.discoverdev.nms.dailylog.dto.LogEntryResponse;
import com.discoverdev.nms.dailylog.dto.PublishRequest;
import com.discoverdev.nms.dailylog.dto.UpdateLogEntryRequest;
import com.discoverdev.nms.tenant.TeacherAccessService;
import com.discoverdev.nms.user.User;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class TeacherDailyLogService {

    private final DailyLogRepository dailyLogRepository;
    private final LogEntryRepository entryRepository;
    private final ChildRepository childRepository;
    private final TeacherAccessService teacherAccess;
    private final ApplicationEventPublisher events;

    public TeacherDailyLogService(
        DailyLogRepository dailyLogRepository,
        LogEntryRepository entryRepository,
        ChildRepository childRepository,
        TeacherAccessService teacherAccess,
        ApplicationEventPublisher events
    ) {
        this.dailyLogRepository = dailyLogRepository;
        this.entryRepository = entryRepository;
        this.childRepository = childRepository;
        this.teacherAccess = teacherAccess;
        this.events = events;
    }

    @Transactional
    public LogEntryResponse addEntry(UUID childId, CreateLogEntryRequest req, User teacher) {
        Child child = requireAccessibleChild(childId, teacher);
        DailyLog log = dailyLogRepository.findByChildIdAndLogDate(child.getId(), req.date())
            .orElseGet(() -> dailyLogRepository.save(
                DailyLog.builder()
                    .nursery(child.getNursery())
                    .child(child)
                    .logDate(req.date())
                    .build()));

        LogEntry.LogEntryBuilder builder = LogEntry.builder()
            .dailyLog(log)
            .category(req.category())
            .entryTime(req.entryTime())
            .author(teacher)
            .title(req.title())
            .detail(req.detail())
            .napMinutes(req.napMinutes());

        boolean publishNow = Boolean.TRUE.equals(req.publish());
        if (publishNow) {
            builder.publishedAt(Instant.now()).publishedBy(teacher);
        }
        LogEntry saved = entryRepository.save(builder.build());

        if (publishNow) {
            firePublishedEvent(log, List.of(saved.getId()), teacher);
        }
        return LogEntryResponse.of(saved);
    }

    @Transactional
    public LogEntryResponse updateEntry(UUID entryId, UpdateLogEntryRequest req, User teacher) {
        LogEntry entry = entryRepository.findById(entryId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Log entry not found"));
        requireAccessibleChild(entry.getDailyLog().getChild().getId(), teacher);
        if (req.category() != null) entry.setCategory(req.category());
        if (req.entryTime() != null) entry.setEntryTime(req.entryTime());
        if (req.title() != null) entry.setTitle(req.title());
        if (req.detail() != null) entry.setDetail(req.detail());
        if (req.napMinutes() != null) entry.setNapMinutes(req.napMinutes());
        return LogEntryResponse.of(entryRepository.save(entry));
    }

    @Transactional
    public void deleteEntry(UUID entryId, User teacher) {
        LogEntry entry = entryRepository.findById(entryId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Log entry not found"));
        requireAccessibleChild(entry.getDailyLog().getChild().getId(), teacher);
        entryRepository.delete(entry);
    }

    @Transactional
    public LogEntryResponse publishEntry(UUID entryId, User teacher) {
        LogEntry entry = entryRepository.findById(entryId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Log entry not found"));
        DailyLog log = entry.getDailyLog();
        requireAccessibleChild(log.getChild().getId(), teacher);
        if (entry.getPublishedAt() == null) {
            entry.setPublishedAt(Instant.now());
            entry.setPublishedBy(teacher);
            entryRepository.save(entry);
            firePublishedEvent(log, List.of(entry.getId()), teacher);
        }
        return LogEntryResponse.of(entry);
    }

    @Transactional
    public LogEntryResponse unpublishEntry(UUID entryId, User teacher) {
        LogEntry entry = entryRepository.findById(entryId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Log entry not found"));
        requireAccessibleChild(entry.getDailyLog().getChild().getId(), teacher);
        if (entry.getPublishedAt() != null) {
            entry.setPublishedAt(null);
            entry.setPublishedBy(null);
            entryRepository.save(entry);
        }
        return LogEntryResponse.of(entry);
    }

    @Transactional
    public DailyLogResponse publishLog(UUID logId, PublishRequest req, User teacher) {
        DailyLog log = dailyLogRepository.findById(logId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Daily log not found"));
        requireAccessibleChild(log.getChild().getId(), teacher);

        List<LogEntry> targets;
        if (req == null || req.entryIds() == null || req.entryIds().isEmpty()) {
            targets = entryRepository.findByDailyLogIdAndPublishedAtIsNullOrderByEntryTimeAscCreatedAtAsc(logId);
        } else {
            targets = new ArrayList<>();
            for (UUID id : req.entryIds()) {
                LogEntry e = entryRepository.findByIdAndDailyLogId(id, logId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Log entry not found in this log"));
                if (e.getPublishedAt() == null) targets.add(e);
            }
        }

        List<UUID> newlyPublished = new ArrayList<>();
        Instant now = Instant.now();
        for (LogEntry e : targets) {
            e.setPublishedAt(now);
            e.setPublishedBy(teacher);
            entryRepository.save(e);
            newlyPublished.add(e.getId());
        }
        if (!newlyPublished.isEmpty()) {
            firePublishedEvent(log, newlyPublished, teacher);
        }
        return loadLogResponse(log);
    }

    @Transactional(readOnly = true)
    public Optional<DailyLogResponse> getByChildAndDate(UUID childId, LocalDate date, User teacher) {
        requireAccessibleChild(childId, teacher);
        return dailyLogRepository.findByChildIdAndLogDate(childId, date).map(this::loadLogResponse);
    }

    @Transactional(readOnly = true)
    public DailyLogResponse getById(UUID logId, User teacher) {
        DailyLog log = dailyLogRepository.findById(logId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Daily log not found"));
        requireAccessibleChild(log.getChild().getId(), teacher);
        return loadLogResponse(log);
    }

    @Transactional(readOnly = true)
    public List<DailyLogSummaryResponse> listRoomFeed(UUID roomId, LocalDate date, User teacher) {
        if (!teacherAccess.canAccessRoom(teacher, roomId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found");
        }
        UUID nurseryId = teacher.getNursery().getId();
        return dailyLogRepository.findByNurseryIdAndLogDate(nurseryId, date).stream()
            .filter(l -> l.getChild().getRoom() != null
                && roomId.equals(l.getChild().getRoom().getId()))
            .map(this::summary)
            .toList();
    }

    private Child requireAccessibleChild(UUID childId, User teacher) {
        Child child = childRepository.findById(childId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Child not found"));
        if (!teacherAccess.canAccessChild(teacher, child)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Child not found");
        }
        return child;
    }

    private DailyLogResponse loadLogResponse(DailyLog log) {
        List<LogEntry> entries = entryRepository.findByDailyLogIdOrderByEntryTimeAscCreatedAtAsc(log.getId());
        return DailyLogResponse.of(log, entries);
    }

    private DailyLogSummaryResponse summary(DailyLog log) {
        long total = entryRepository.countByDailyLogId(log.getId());
        long published = entryRepository.countByDailyLogIdAndPublishedAtIsNotNull(log.getId());
        return DailyLogSummaryResponse.of(log, total, published);
    }

    private void firePublishedEvent(DailyLog log, List<UUID> entryIds, User publishedBy) {
        events.publishEvent(new DailyLogEntriesPublishedEvent(
            log.getNursery().getId(),
            log.getChild().getId(),
            log.getId(),
            entryIds,
            publishedBy.getId()
        ));
    }
}
