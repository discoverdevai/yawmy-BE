package com.discoverdev.nms.schedule;

import com.discoverdev.nms.child.Child;
import com.discoverdev.nms.child.EnrollmentStatus;
import com.discoverdev.nms.child.GuardianLink;
import com.discoverdev.nms.child.GuardianLinkRepository;
import com.discoverdev.nms.schedule.dto.SessionResponse;
import com.discoverdev.nms.user.User;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Read-only access to a child's room schedule for a parent. A "schedule" is the
 * set of {@link RoomSession}s for the child's room on a specific date. Access is
 * authorized through the guardian link, mirroring {@code ParentDailyLogService}.
 */
@Service
public class ParentScheduleService {

    private final RoomSessionRepository sessionRepository;
    private final GuardianLinkRepository guardianLinkRepository;

    public ParentScheduleService(
        RoomSessionRepository sessionRepository,
        GuardianLinkRepository guardianLinkRepository
    ) {
        this.sessionRepository = sessionRepository;
        this.guardianLinkRepository = guardianLinkRepository;
    }

    @Transactional(readOnly = true)
    public List<SessionResponse> schedule(UUID childId, LocalDate date, User parent) {
        Child child = requireLinkedEnrolledChild(childId, parent);
        if (child.getRoom() == null || parent.getNursery() == null) {
            return List.of();
        }
        UUID nurseryId = parent.getNursery().getId();
        return sessionRepository
            .findByNurseryIdAndRoomIdAndSessionDateOrderByStartTimeAsc(nurseryId, child.getRoom().getId(), date)
            .stream()
            .map(SessionResponse::of)
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
