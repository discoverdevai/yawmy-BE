package com.discoverdev.nms.admin;

import com.discoverdev.nms.admin.dto.DashboardResponse;
import com.discoverdev.nms.admin.dto.DashboardResponse.DayAttendance;
import com.discoverdev.nms.attendance.AttendanceDates;
import com.discoverdev.nms.attendance.AttendanceEvent;
import com.discoverdev.nms.attendance.AttendanceEventRepository;
import com.discoverdev.nms.attendance.AttendanceEventType;
import com.discoverdev.nms.child.ChildRepository;
import com.discoverdev.nms.child.EnrollmentStatus;
import com.discoverdev.nms.incident.IncidentReportRepository;
import com.discoverdev.nms.nursery.Nursery;
import com.discoverdev.nms.nursery.NurseryRepository;
import com.discoverdev.nms.tenant.TenantContext;
import com.discoverdev.nms.user.Role;
import com.discoverdev.nms.user.UserRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AdminDashboardService {

    // Egyptian working week: Saturday .. Thursday (Friday off).
    private static final String[] WEEK_LABELS = {"Sat", "Sun", "Mon", "Tue", "Wed", "Thu"};
    private static final int WEEK_DAYS = 6;

    private final ChildRepository childRepository;
    private final UserRepository userRepository;
    private final IncidentReportRepository incidentRepository;
    private final AttendanceEventRepository attendanceRepository;
    private final NurseryRepository nurseryRepository;

    public AdminDashboardService(
        ChildRepository childRepository,
        UserRepository userRepository,
        IncidentReportRepository incidentRepository,
        AttendanceEventRepository attendanceRepository,
        NurseryRepository nurseryRepository
    ) {
        this.childRepository = childRepository;
        this.userRepository = userRepository;
        this.incidentRepository = incidentRepository;
        this.attendanceRepository = attendanceRepository;
        this.nurseryRepository = nurseryRepository;
    }

    @Transactional(readOnly = true)
    public DashboardResponse getDashboard() {
        UUID nurseryId = TenantContext.currentNurseryId();
        Nursery nursery = nurseryRepository.findById(nurseryId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Nursery not found"));

        ZoneId zone = AttendanceDates.zoneOf(nursery);
        LocalDate today = LocalDate.now(zone);

        long totalChildren = childRepository.countByNurseryIdAndEnrollmentStatus(
            nurseryId, EnrollmentStatus.ENROLLED);
        long totalTeachers = userRepository.countByNurseryIdAndRole(nurseryId, Role.TEACHER);

        Instant todayStart = AttendanceDates.startOfDay(nursery, today);
        Instant todayEnd = AttendanceDates.startOfNextDay(nursery, today);
        long incidentsToday = incidentRepository.countByNurseryIdAndIncidentAtBetween(
            nurseryId, todayStart, todayEnd);

        // How many distinct children checked in today.
        long checkedInToday = distinctCheckedInChildren(
            attendanceRepository.findByNurseryIdAndRecordedAtBetween(nurseryId, todayStart, todayEnd),
            nursery, today).getOrDefault(today, Set.of()).size();

        // Current week Saturday..Thursday.
        LocalDate weekStart = today.minusDays((today.getDayOfWeek().getValue() + 1) % 7); // back to Saturday
        LocalDate weekEndInclusive = weekStart.plusDays(WEEK_DAYS - 1); // Thursday
        List<AttendanceEvent> weekEvents = attendanceRepository.findByNurseryIdAndRecordedAtBetween(
            nurseryId,
            AttendanceDates.startOfDay(nursery, weekStart),
            AttendanceDates.startOfNextDay(nursery, weekEndInclusive));
        Map<LocalDate, Set<UUID>> presentByDay = distinctCheckedInChildren(weekEvents, nursery, null);

        List<DayAttendance> weekly = new ArrayList<>(WEEK_DAYS);
        for (int i = 0; i < WEEK_DAYS; i++) {
            LocalDate d = weekStart.plusDays(i);
            weekly.add(new DayAttendance(WEEK_LABELS[i], d, presentByDay.getOrDefault(d, Set.of()).size()));
        }

        return new DashboardResponse(
            nursery.getName(), nursery.getLogoUrl(),
            totalChildren, totalTeachers, checkedInToday, incidentsToday, weekly);
    }

    /** Group CHECK_IN events into a set of distinct child ids per local day. */
    private Map<LocalDate, Set<UUID>> distinctCheckedInChildren(
        List<AttendanceEvent> events, Nursery nursery, LocalDate onlyDay
    ) {
        Map<LocalDate, Set<UUID>> byDay = new HashMap<>();
        for (AttendanceEvent e : events) {
            if (e.getType() != AttendanceEventType.CHECK_IN) continue;
            LocalDate d = AttendanceDates.localDateOf(nursery, e.getRecordedAt());
            if (onlyDay != null && !onlyDay.equals(d)) continue;
            byDay.computeIfAbsent(d, k -> new HashSet<>()).add(e.getChild().getId());
        }
        return byDay;
    }
}
