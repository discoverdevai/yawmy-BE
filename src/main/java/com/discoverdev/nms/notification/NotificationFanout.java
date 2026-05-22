package com.discoverdev.nms.notification;

import com.discoverdev.nms.announcement.Announcement;
import com.discoverdev.nms.announcement.AnnouncementPublishedEvent;
import com.discoverdev.nms.announcement.AnnouncementRepository;
import com.discoverdev.nms.announcement.AnnouncementScope;
import com.discoverdev.nms.attendance.AttendanceDates;
import com.discoverdev.nms.attendance.AttendanceEventRecordedEvent;
import com.discoverdev.nms.attendance.AttendanceEventType;
import com.discoverdev.nms.child.Child;
import com.discoverdev.nms.child.ChildRepository;
import com.discoverdev.nms.dailylog.DailyLog;
import com.discoverdev.nms.dailylog.DailyLogEntriesPublishedEvent;
import com.discoverdev.nms.dailylog.DailyLogRepository;
import com.discoverdev.nms.event.EventCreatedEvent;
import com.discoverdev.nms.event.NurseryEvent;
import com.discoverdev.nms.event.NurseryEventRepository;
import com.discoverdev.nms.incident.IncidentAcknowledgedEvent;
import com.discoverdev.nms.incident.IncidentReport;
import com.discoverdev.nms.incident.IncidentReportRepository;
import com.discoverdev.nms.incident.IncidentReportedEvent;
import com.discoverdev.nms.media.MediaItem;
import com.discoverdev.nms.media.MediaItemRepository;
import com.discoverdev.nms.media.MediaUploadedEvent;
import com.discoverdev.nms.nursery.Nursery;
import com.discoverdev.nms.nursery.NurseryRepository;
import com.discoverdev.nms.user.User;
import com.discoverdev.nms.user.UserRepository;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationFanout {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("h:mm a");

    private final NotificationRepository notificationRepository;
    private final NotificationPreferenceRepository prefRepository;
    private final NotificationAudienceService audience;
    private final NurseryRepository nurseryRepository;
    private final UserRepository userRepository;
    private final ChildRepository childRepository;
    private final DailyLogRepository dailyLogRepository;
    private final IncidentReportRepository incidentRepository;
    private final MediaItemRepository mediaRepository;
    private final AnnouncementRepository announcementRepository;
    private final NurseryEventRepository eventRepository;

    public NotificationFanout(
        NotificationRepository notificationRepository,
        NotificationPreferenceRepository prefRepository,
        NotificationAudienceService audience,
        NurseryRepository nurseryRepository,
        UserRepository userRepository,
        ChildRepository childRepository,
        DailyLogRepository dailyLogRepository,
        IncidentReportRepository incidentRepository,
        MediaItemRepository mediaRepository,
        AnnouncementRepository announcementRepository,
        NurseryEventRepository eventRepository
    ) {
        this.notificationRepository = notificationRepository;
        this.prefRepository = prefRepository;
        this.audience = audience;
        this.nurseryRepository = nurseryRepository;
        this.userRepository = userRepository;
        this.childRepository = childRepository;
        this.dailyLogRepository = dailyLogRepository;
        this.incidentRepository = incidentRepository;
        this.mediaRepository = mediaRepository;
        this.announcementRepository = announcementRepository;
        this.eventRepository = eventRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void fanoutDailyLogPublished(DailyLogEntriesPublishedEvent event) {
        Child child = childRepository.findById(event.childId()).orElse(null);
        DailyLog log = dailyLogRepository.findById(event.dailyLogId()).orElse(null);
        if (child == null || log == null) return;
        Nursery nursery = nurseryRepository.findById(event.nurseryId()).orElse(null);
        if (nursery == null) return;
        String title = "Today's log is ready";
        String body = String.format("%s's daily log has been updated with %d new %s",
            child.getFullName(),
            event.entryIds().size(),
            event.entryIds().size() == 1 ? "entry" : "entries");
        writeForUsers(
            audience.guardiansOfChild(event.childId(), true),
            nursery, NotificationType.DAILY_LOG_PUBLISHED,
            title, body, "daily_log", event.dailyLogId(), event.childId(), null
        );
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void fanoutAttendance(AttendanceEventRecordedEvent event) {
        Child child = childRepository.findById(event.childId()).orElse(null);
        if (child == null) return;
        Nursery nursery = nurseryRepository.findById(event.nurseryId()).orElse(null);
        if (nursery == null) return;
        User actor = userRepository.findById(event.recordedByUserId()).orElse(null);
        boolean isCheckIn = event.type() == AttendanceEventType.CHECK_IN;
        NotificationType notifType = isCheckIn
            ? NotificationType.ATTENDANCE_CHECK_IN
            : NotificationType.ATTENDANCE_CHECK_OUT;
        LocalTime t = AttendanceDates.localTimeOf(nursery, event.recordedAt());
        String title = String.format("%s %s",
            child.getFullName(), isCheckIn ? "checked in" : "checked out");
        String body = String.format("At %s%s",
            TIME.format(t),
            actor != null ? " by " + actor.getFullName() : "");
        writeForUsers(
            audience.guardiansOfChild(event.childId(), true),
            nursery, notifType, title, body,
            "attendance_event", event.eventId(), event.childId(),
            event.recordedByUserId()
        );
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void fanoutIncidentReported(IncidentReportedEvent event) {
        IncidentReport incident = incidentRepository.findById(event.incidentId()).orElse(null);
        if (incident == null) return;
        Nursery nursery = nurseryRepository.findById(event.nurseryId()).orElse(null);
        if (nursery == null) return;
        Child child = incident.getChild();
        String title = String.format("Incident report for %s", child.getFullName());
        String body = String.format("%s filed an incident report",
            incident.getAuthor().getFullName());

        List<User> recipients = new ArrayList<>();
        recipients.addAll(audience.adminsOfNursery(event.nurseryId()));
        recipients.addAll(audience.guardiansOfChild(event.childId(), true));
        recipients = dedupe(recipients);
        writeForUsers(recipients, nursery,
            NotificationType.INCIDENT_REPORTED, title, body,
            "incident", event.incidentId(), event.childId(),
            event.authorUserId());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void fanoutIncidentAcknowledged(IncidentAcknowledgedEvent event) {
        IncidentReport incident = incidentRepository.findById(event.incidentId()).orElse(null);
        if (incident == null) return;
        Nursery nursery = nurseryRepository.findById(event.nurseryId()).orElse(null);
        if (nursery == null) return;
        User guardian = userRepository.findById(event.guardianUserId()).orElse(null);
        String guardianName = guardian != null ? guardian.getFullName() : "A parent";
        String title = String.format("Incident acknowledged: %s", incident.getChild().getFullName());
        String body = String.format("%s acknowledged the incident report", guardianName);
        writeForUsers(
            audience.adminsOfNursery(event.nurseryId()),
            nursery, NotificationType.INCIDENT_ACKNOWLEDGED,
            title, body, "incident", event.incidentId(), event.childId(),
            event.guardianUserId()
        );
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void fanoutMediaUploaded(MediaUploadedEvent event) {
        if (event.mediaItemIds().isEmpty()) return;
        Nursery nursery = nurseryRepository.findById(event.nurseryId()).orElse(null);
        if (nursery == null) return;
        MediaItem first = mediaRepository.findById(event.mediaItemIds().get(0)).orElse(null);
        int count = event.mediaItemIds().size();

        List<User> recipients;
        UUID childIdForBody = event.childId();
        String childName = null;
        if (event.childId() != null) {
            Child child = childRepository.findById(event.childId()).orElse(null);
            if (child != null) childName = child.getFullName();
            recipients = audience.guardiansOfChild(event.childId(), true);
        } else if (event.roomId() != null) {
            recipients = audience.parentsInRoom(event.nurseryId(), event.roomId());
        } else {
            recipients = List.of();
        }

        String label;
        if (first != null && first.getType().name().equals("VIDEO")) {
            label = count == 1 ? "video" : "videos";
        } else {
            label = count == 1 ? "photo" : "photos";
        }
        String title = childName != null
            ? String.format("New media for %s", childName)
            : "New room media";
        String body = String.format("%d new %s uploaded", count, label);
        writeForUsers(recipients, nursery, NotificationType.MEDIA_UPLOADED,
            title, body, "media", event.mediaItemIds().get(0), childIdForBody,
            event.uploaderUserId());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void fanoutAnnouncementPublished(AnnouncementPublishedEvent event) {
        Announcement ann = announcementRepository.findById(event.announcementId()).orElse(null);
        if (ann == null) return;
        Nursery nursery = nurseryRepository.findById(event.nurseryId()).orElse(null);
        if (nursery == null) return;

        List<User> recipients;
        if (event.scope() == AnnouncementScope.NURSERY) {
            recipients = audience.adminsOfNursery(event.nurseryId()); // placeholder — really want all parents
            // For NURSERY scope, every parent in the nursery is a recipient.
            // Compose: all parents whose enrolled children are in any room of this nursery.
            recipients = userRepository.findByNurseryIdAndRoleAndEnabledTrue(
                event.nurseryId(), com.discoverdev.nms.user.Role.PARENT);
        } else if (event.roomId() != null) {
            recipients = audience.parentsInRoom(event.nurseryId(), event.roomId());
        } else {
            recipients = List.of();
        }
        recipients = filterByAnnouncementPref(recipients);

        String title = ann.getTitle();
        String body = ann.getBody().length() > 200
            ? ann.getBody().substring(0, 200) + "…"
            : ann.getBody();
        writeForUsers(recipients, nursery, NotificationType.ANNOUNCEMENT_PUBLISHED,
            title, body, "announcement", event.announcementId(), null,
            event.authorUserId());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void fanoutEventCreated(EventCreatedEvent event) {
        NurseryEvent ev = eventRepository.findById(event.eventId()).orElse(null);
        if (ev == null) return;
        Nursery nursery = nurseryRepository.findById(event.nurseryId()).orElse(null);
        if (nursery == null) return;

        List<User> recipients;
        if (event.scope() == AnnouncementScope.NURSERY) {
            recipients = userRepository.findByNurseryIdAndRoleAndEnabledTrue(
                event.nurseryId(), com.discoverdev.nms.user.Role.PARENT);
        } else if (event.roomId() != null) {
            recipients = audience.parentsInRoom(event.nurseryId(), event.roomId());
        } else {
            recipients = List.of();
        }

        String title = "New event: " + ev.getTitle();
        String body = ev.getLocation() != null && !ev.getLocation().isBlank()
            ? String.format("Starts %s at %s",
                AttendanceDates.localDateOf(nursery, ev.getStartsAt()), ev.getLocation())
            : "Starts " + AttendanceDates.localDateOf(nursery, ev.getStartsAt());
        writeForUsers(recipients, nursery, NotificationType.EVENT_CREATED,
            title, body, "event", event.eventId(), null,
            event.authorUserId());
    }

    private void writeForUsers(
        List<User> recipients, Nursery nursery, NotificationType type,
        String title, String body, String entityType, UUID entityId,
        UUID childId, UUID excludeUserId
    ) {
        if (recipients.isEmpty()) return;
        for (User u : recipients) {
            if (excludeUserId != null && u.getId().equals(excludeUserId)) continue;
            notificationRepository.save(Notification.builder()
                .nursery(nursery)
                .recipient(u)
                .type(type)
                .title(title)
                .body(body)
                .entityType(entityType)
                .entityId(entityId)
                .childId(childId)
                .build());
        }
    }

    private List<User> filterByAnnouncementPref(List<User> users) {
        List<User> out = new ArrayList<>();
        for (User u : users) {
            NotificationPreference pref = prefRepository.findByUserId(u.getId()).orElse(null);
            if (pref == null || pref.isAnnouncementsEnabled()) out.add(u);
        }
        return out;
    }

    private List<User> dedupe(List<User> users) {
        Map<UUID, User> map = new LinkedHashMap<>();
        for (User u : users) map.putIfAbsent(u.getId(), u);
        return new ArrayList<>(map.values());
    }
}
