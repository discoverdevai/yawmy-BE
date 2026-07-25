package com.discoverdev.nms.dailylog;

import com.discoverdev.nms.child.Child;
import com.discoverdev.nms.child.ChildRepository;
import com.discoverdev.nms.child.EnrollmentStatus;
import com.discoverdev.nms.dailylog.dto.ChildLogStatusResponse;
import com.discoverdev.nms.dailylog.dto.CreateLogEntryRequest;
import com.discoverdev.nms.dailylog.dto.DailyLogResponse;
import com.discoverdev.nms.dailylog.dto.DailyLogSummaryResponse;
import com.discoverdev.nms.dailylog.dto.LogEntryResponse;
import com.discoverdev.nms.dailylog.dto.PublishRequest;
import com.discoverdev.nms.dailylog.dto.UpdateLogEntryRequest;
import com.discoverdev.nms.dailylog.dto.UpsertDailyLogRequest;
import com.discoverdev.nms.tenant.TeacherAccessService;
import com.discoverdev.nms.user.User;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
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

    /**
     * Save a child's whole log for one date in a single call — what the app's
     * "Save as Draft" / "Publish" buttons hit.
     *
     * The form owns the complete day, so the log's entries are REPLACED by the
     * payload. That keeps the call idempotent: a save that failed on poor
     * nursery wifi can be retried as-is without duplicating entries.
     *
     * A log that already has published entries is never retracted here. Once a
     * parent has seen the day, saving again always republishes — you cannot
     * un-tell them. The app hides "Save as Draft" for a published log; this
     * coercion is the backstop for a stale or offline client.
     */
    @Transactional
    public DailyLogResponse upsertLog(
        UUID childId,
        LocalDate date,
        UpsertDailyLogRequest req,
        User teacher
    ) {
        Child child = requireAccessibleChild(childId, teacher);
        DailyLog log = dailyLogRepository.findByChildIdAndLogDate(child.getId(), date)
            .orElseGet(() -> dailyLogRepository.save(
                DailyLog.builder()
                    .nursery(child.getNursery())
                    .child(child)
                    .logDate(date)
                    .build()));

        boolean alreadyPublished = entryRepository.countByDailyLogIdAndPublishedAtIsNotNull(log.getId()) > 0;
        boolean publishNow = Boolean.TRUE.equals(req.publish()) || alreadyPublished;

        entryRepository.deleteAll(entryRepository.findByDailyLogIdOrderByEntryTimeAscCreatedAtAsc(log.getId()));
        // Flush the deletes before inserting so the replacement never collides
        // with the rows it is replacing.
        entryRepository.flush();

        Instant now = Instant.now();
        List<UUID> publishedIds = new ArrayList<>();
        for (UpsertDailyLogRequest.Entry e : req.entries()) {
            LogEntry.LogEntryBuilder builder = LogEntry.builder()
                .dailyLog(log)
                .category(e.category())
                .entryTime(e.entryTime())
                .author(teacher)
                .title(e.title())
                .detail(e.detail())
                .napMinutes(e.napMinutes());
            if (publishNow) {
                builder.publishedAt(now).publishedBy(teacher);
            }
            LogEntry saved = entryRepository.save(builder.build());
            if (publishNow) publishedIds.add(saved.getId());
        }

        if (!publishedIds.isEmpty()) {
            firePublishedEvent(log, publishedIds, teacher);
        }
        return loadLogResponse(log);
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

    /**
     * Every enrolled child in the room with their log state for the date —
     * what the Logs screen lists.
     *
     * The room feed above only returns children who already have a DailyLog
     * row, so children with nothing written are invisible in it. Those are
     * precisely the ones the teacher still has to do, so this starts from the
     * roster and left-joins the logs.
     */
    @Transactional(readOnly = true)
    public List<ChildLogStatusResponse> roomLogStatus(UUID roomId, LocalDate date, User teacher) {
        if (!teacherAccess.canAccessRoom(teacher, roomId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found");
        }
        UUID nurseryId = teacher.getNursery().getId();

        Map<UUID, DailyLog> logsByChild = dailyLogRepository
            .findByNurseryIdAndLogDate(nurseryId, date).stream()
            .filter(l -> l.getChild().getRoom() != null
                && roomId.equals(l.getChild().getRoom().getId()))
            .collect(Collectors.toMap(l -> l.getChild().getId(), l -> l, (a, b) -> a));

        return childRepository
            .findByNurseryIdAndRoomIdAndEnrollmentStatusOrderByFullNameAsc(
                nurseryId, roomId, EnrollmentStatus.ENROLLED)
            .stream()
            .map(child -> {
                DailyLog log = logsByChild.get(child.getId());
                String photoUrl = child.getPhotoKey() != null
                    ? "/api/files/children/" + child.getId() + "/photo"
                    : null;

                if (log == null) {
                    return new ChildLogStatusResponse(
                        child.getId(), child.getFullName(), photoUrl,
                        null, ChildLogStatusResponse.NOT_STARTED, 0, 0, null);
                }

                long total = entryRepository.countByDailyLogId(log.getId());
                long published = entryRepository.countByDailyLogIdAndPublishedAtIsNotNull(log.getId());
                // A log row with no entries is still "not started" to the
                // teacher — it can be left behind by a cleared draft.
                String status = total == 0
                    ? ChildLogStatusResponse.NOT_STARTED
                    : published > 0
                        ? ChildLogStatusResponse.COMPLETED
                        : ChildLogStatusResponse.DRAFT;

                return new ChildLogStatusResponse(
                    child.getId(), child.getFullName(), photoUrl,
                    log.getId(), status, total, published, log.getUpdatedAt());
            })
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
