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
import com.discoverdev.nms.attendance.AttendanceDates;
import com.discoverdev.nms.nursery.Nursery;
import com.discoverdev.nms.tenant.TeacherAccessService;
import com.discoverdev.nms.user.User;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
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

        List<LogEntry> stored =
            entryRepository.findByDailyLogIdOrderByEntryTimeAscCreatedAtAsc(log.getId());
        boolean alreadyPublished = stored.stream().anyMatch(e -> e.getPublishedAt() != null);
        boolean publishNow = Boolean.TRUE.equals(req.publish()) || alreadyPublished;

        // Appending is for the teacher who left an existing draft alone and
        // wrote more logs beside it — the form holds only the new ones, so the
        // stored ones must survive. Publishing never appends: it settles the
        // day as exactly what was sent.
        boolean append = Boolean.TRUE.equals(req.append()) && !publishNow;

        Map<UUID, LogEntry> storedById = stored.stream()
            .collect(Collectors.toMap(LogEntry::getId, e -> e));

        // Reconcile rather than wipe-and-reinsert. An entry the request still
        // carries by id keeps its row, and with it any publication it already
        // has — which is what stops an edit to one meal re-notifying parents
        // about the whole day. Only the entries actually dropped are deleted.
        if (!append) {
            Set<UUID> kept = req.entries().stream()
                .map(UpsertDailyLogRequest.Entry::id)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
            List<LogEntry> removed = stored.stream()
                .filter(e -> !kept.contains(e.getId()))
                .toList();
            if (!removed.isEmpty()) {
                entryRepository.deleteAll(removed);
                entryRepository.flush();
            }
        }

        Instant now = Instant.now();
        List<UUID> newlyPublished = new ArrayList<>();

        for (UpsertDailyLogRequest.Entry e : req.entries()) {
            // Appending never touches stored rows, so everything it carries is new.
            LogEntry existing = !append && e.id() != null ? storedById.get(e.id()) : null;

            LogEntry values = applyEntry(
                LogEntry.builder(),
                child.getNursery(),
                e.category(), e.entryTime(), e.title(), e.detail(),
                e.mood(), e.mealAmount(), e.napStartTime(), e.napEndTime(), e.napQuality()
            ).build();

            if (existing != null) {
                copyValues(values, existing);
                // A draft entry being published for the first time still counts
                // as news; one already published does not.
                if (publishNow && existing.getPublishedAt() == null) {
                    existing.setPublishedAt(now);
                    existing.setPublishedBy(teacher);
                    newlyPublished.add(existing.getId());
                }
                entryRepository.save(existing);
            } else {
                LogEntry.LogEntryBuilder builder = applyEntry(
                    LogEntry.builder().dailyLog(log).author(teacher),
                    child.getNursery(),
                    e.category(), e.entryTime(), e.title(), e.detail(),
                    e.mood(), e.mealAmount(), e.napStartTime(), e.napEndTime(), e.napQuality()
                );
                if (publishNow) {
                    builder.publishedAt(now).publishedBy(teacher);
                }
                LogEntry saved = entryRepository.save(builder.build());
                if (publishNow) newlyPublished.add(saved.getId());
            }
        }

        // Only what the parent has not already been told about.
        if (!newlyPublished.isEmpty()) {
            firePublishedEvent(log, newlyPublished, teacher);
        }
        return loadLogResponse(log);
    }

    /** Copy an entry's editable values onto a stored row, leaving its identity,
     *  author and publication alone. */
    private void copyValues(LogEntry from, LogEntry to) {
        to.setCategory(from.getCategory());
        to.setEntryTime(from.getEntryTime());
        to.setTitle(from.getTitle());
        to.setDetail(from.getDetail());
        to.setMood(from.getMood());
        to.setMealAmount(from.getMealAmount());
        to.setNapStartTime(from.getNapStartTime());
        to.setNapEndTime(from.getNapEndTime());
        to.setNapQuality(from.getNapQuality());
        to.setNapMinutes(from.getNapMinutes());
    }

    /**
     * Applies one entry's fields to a builder, checking that the answer its
     * category actually requires is present, and filling in what the form does
     * not ask the teacher for.
     *
     * These rules live here rather than as bean validation because they depend
     * on {@code category}, which annotations cannot express — a nap needs a
     * window and a quality, a meal needs a name and an amount, and a diaper
     * change needs neither.
     *
     * {@code title} is always set because the column is NOT NULL and every
     * consumer can then show a row without understanding categories; for the
     * categories with no name of their own it is derived from the answer.
     */
    private LogEntry.LogEntryBuilder applyEntry(
        LogEntry.LogEntryBuilder builder,
        Nursery nursery,
        LogEntryCategory category,
        LocalTime entryTime,
        String title,
        String detail,
        MoodOption mood,
        MealAmount mealAmount,
        LocalTime napStartTime,
        LocalTime napEndTime,
        NapQuality napQuality
    ) {
        String name = title != null && !title.isBlank() ? title.trim() : null;
        LocalTime when = entryTime;
        Integer napMinutes = null;

        switch (category) {
            case MOOD -> {
                require(mood != null, "Select how the child seemed");
                if (name == null) name = mood.getLabel();
            }
            case MEAL -> {
                require(name != null, "A meal name is required");
                require(mealAmount != null, "Select how much was eaten");
            }
            case NAP -> {
                require(napStartTime != null && napEndTime != null,
                    "A nap start and end time are required");
                require(napEndTime.isAfter(napStartTime),
                    "The nap end time must be after the start time");
                require(napQuality != null, "Select how the nap went");
                napMinutes = (int) Duration.between(napStartTime, napEndTime).toMinutes();
                // The nap's own start is when it happened.
                if (when == null) when = napStartTime;
                if (name == null) name = napQuality.getLabel();
            }
            case MEDICATION -> {
                require(name != null, "A medication name is required");
                require(when != null, "A medication time is required");
            }
            case ACTIVITY -> require(name != null, "An activity name is required");
            case DIAPER -> {
                // Nothing to record but the fact and the moment — a day's
                // diaper count is simply how many of these the child has.
                if (name == null) name = "Diaper change";
            }
        }

        // Every entry carries a time. Where the form does not ask for one it is
        // the moment it was recorded, read in the nursery's zone rather than the
        // server's — otherwise a Cairo nursery would stamp entries in UTC.
        if (when == null) {
            when = AttendanceDates.localTimeOf(nursery, Instant.now()).withSecond(0).withNano(0);
        }

        boolean isNap = category == LogEntryCategory.NAP;
        return builder
            .category(category)
            .entryTime(when)
            .title(name)
            .detail(detail)
            // Only the answering category's fields are stored, so an entry
            // switched from Nap to Meal cannot keep a stale nap window.
            .mood(category == LogEntryCategory.MOOD ? mood : null)
            .mealAmount(category == LogEntryCategory.MEAL ? mealAmount : null)
            .napStartTime(isNap ? napStartTime : null)
            .napEndTime(isNap ? napEndTime : null)
            .napQuality(isNap ? napQuality : null)
            .napMinutes(napMinutes);
    }

    private static void require(boolean satisfied, String message) {
        if (!satisfied) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
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

        LogEntry.LogEntryBuilder builder = applyEntry(
            LogEntry.builder().dailyLog(log).author(teacher),
            child.getNursery(),
            req.category(), req.entryTime(), req.title(), req.detail(),
            req.mood(), req.mealAmount(), req.napStartTime(), req.napEndTime(), req.napQuality()
        );

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
        // Merge the patch over what is stored, then re-check the whole entry:
        // editing one field must not be able to leave the category's required
        // answer missing or the nap window inverted.
        LogEntryCategory category = req.category() != null ? req.category() : entry.getCategory();
        LogEntry merged = applyEntry(
            LogEntry.builder(),
            entry.getDailyLog().getChild().getNursery(),
            category,
            req.entryTime() != null ? req.entryTime() : entry.getEntryTime(),
            req.title() != null ? req.title() : entry.getTitle(),
            req.detail() != null ? req.detail() : entry.getDetail(),
            req.mood() != null ? req.mood() : entry.getMood(),
            req.mealAmount() != null ? req.mealAmount() : entry.getMealAmount(),
            req.napStartTime() != null ? req.napStartTime() : entry.getNapStartTime(),
            req.napEndTime() != null ? req.napEndTime() : entry.getNapEndTime(),
            req.napQuality() != null ? req.napQuality() : entry.getNapQuality()
        ).build();

        entry.setCategory(merged.getCategory());
        entry.setEntryTime(merged.getEntryTime());
        entry.setTitle(merged.getTitle());
        entry.setDetail(merged.getDetail());
        entry.setMood(merged.getMood());
        entry.setMealAmount(merged.getMealAmount());
        entry.setNapStartTime(merged.getNapStartTime());
        entry.setNapEndTime(merged.getNapEndTime());
        entry.setNapQuality(merged.getNapQuality());
        entry.setNapMinutes(merged.getNapMinutes());
        return LogEntryResponse.of(entryRepository.save(entry));
    }

    @Transactional
    public void deleteEntry(UUID entryId, User teacher) {
        LogEntry entry = entryRepository.findById(entryId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Log entry not found"));
        requireAccessibleChild(entry.getDailyLog().getChild().getId(), teacher);
        entryRepository.delete(entry);
    }

    /**
     * Discard a child's unpublished log for a date — the "delete draft" the app
     * offers when a teacher would rather start the day over.
     *
     * Refuses once anything has been published: parents may already have read
     * it, so withdrawing it is a different decision from throwing away a draft
     * they never saw. Unpublish the entries first if that is really the intent.
     *
     * Idempotent — deleting a day with no log is a no-op, not a 404, so a
     * retried request cannot fail the second time.
     */
    @Transactional
    public void deleteDraft(UUID childId, LocalDate date, User teacher) {
        Child child = requireAccessibleChild(childId, teacher);
        dailyLogRepository.findByChildIdAndLogDate(child.getId(), date).ifPresent(log -> {
            long published = entryRepository.countByDailyLogIdAndPublishedAtIsNotNull(log.getId());
            if (published > 0) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "This log has already been published and cannot be discarded");
            }
            entryRepository.deleteAll(
                entryRepository.findByDailyLogIdOrderByEntryTimeAscCreatedAtAsc(log.getId()));
            // The log row goes too, so the child reads as NOT_STARTED again
            // rather than lingering as an empty log.
            dailyLogRepository.delete(log);
        });
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
                        child.getId(), child.getFullName(), photoUrl, child.getDob(),
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
                    child.getId(), child.getFullName(), photoUrl, child.getDob(),
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
