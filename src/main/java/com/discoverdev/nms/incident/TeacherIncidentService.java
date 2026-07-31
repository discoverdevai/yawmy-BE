package com.discoverdev.nms.incident;

import com.discoverdev.nms.child.Child;
import com.discoverdev.nms.child.ChildRepository;
import com.discoverdev.nms.child.GuardianLink;
import com.discoverdev.nms.child.GuardianLinkRepository;
import com.discoverdev.nms.incident.dto.AcknowledgmentRow;
import com.discoverdev.nms.incident.dto.CreateIncidentRequest;
import com.discoverdev.nms.incident.dto.IncidentResponse;
import com.discoverdev.nms.incident.dto.UpdateIncidentRequest;
import com.discoverdev.nms.attendance.AttendanceDates;
import com.discoverdev.nms.tenant.TeacherAccessService;
import com.discoverdev.nms.user.User;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class TeacherIncidentService {

    private final IncidentReportRepository incidentRepository;
    private final IncidentAcknowledgmentRepository ackRepository;
    private final ChildRepository childRepository;
    private final GuardianLinkRepository guardianLinkRepository;
    private final TeacherAccessService teacherAccess;
    private final ApplicationEventPublisher events;

    public TeacherIncidentService(
        IncidentReportRepository incidentRepository,
        IncidentAcknowledgmentRepository ackRepository,
        ChildRepository childRepository,
        GuardianLinkRepository guardianLinkRepository,
        TeacherAccessService teacherAccess,
        ApplicationEventPublisher events
    ) {
        this.incidentRepository = incidentRepository;
        this.ackRepository = ackRepository;
        this.childRepository = childRepository;
        this.guardianLinkRepository = guardianLinkRepository;
        this.teacherAccess = teacherAccess;
        this.events = events;
    }

    @Transactional
    public IncidentResponse create(UUID childId, CreateIncidentRequest req, User teacher) {
        Child child = requireAccessibleChild(childId, teacher);
        IncidentReport saved = incidentRepository.save(IncidentReport.builder()
            .nursery(child.getNursery())
            .child(child)
            .room(child.getRoom())
            .author(teacher)
            .incidentAt(req.incidentAt())
            .incidentType(req.incidentType())
            .description(req.description())
            .actionTaken(req.actionTaken())
            .injuryArea(req.injuryArea())
            .build());
        events.publishEvent(new IncidentReportedEvent(
            saved.getNursery().getId(), child.getId(), saved.getId(),
            teacher.getId(), saved.getIncidentAt()
        ));
        return toResponse(saved);
    }

    @Transactional
    public IncidentResponse update(UUID incidentId, UpdateIncidentRequest req, User teacher) {
        IncidentReport incident = loadAccessibleByTeacher(incidentId, teacher);
        if (!incident.getAuthor().getId().equals(teacher.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the author can edit this incident");
        }
        if (ackRepository.countByIncidentId(incident.getId()) > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Incident already acknowledged by a parent and cannot be edited — ask an admin");
        }
        if (req.incidentAt() != null) incident.setIncidentAt(req.incidentAt());
        if (req.incidentType() != null) incident.setIncidentType(req.incidentType());
        if (req.description() != null) incident.setDescription(req.description());
        if (req.actionTaken() != null) incident.setActionTaken(req.actionTaken());
        if (req.injuryArea() != null) incident.setInjuryArea(req.injuryArea());
        return toResponse(incidentRepository.save(incident));
    }

    @Transactional(readOnly = true)
    public IncidentResponse get(UUID incidentId, User teacher) {
        return toResponse(loadAccessibleByTeacher(incidentId, teacher));
    }

    @Transactional(readOnly = true)
    /**
     * A child's incidents, newest first — the whole history, or a single day.
     *
     * The day is bounded in the NURSERY's timezone, not the server's, so an
     * incident logged at 11pm in Cairo belongs to that day rather than the next.
     */
    public List<IncidentResponse> listForChild(UUID childId, LocalDate date, User teacher) {
        Child child = requireAccessibleChild(childId, teacher);
        List<IncidentReport> found = date == null
            ? incidentRepository.findByChildIdOrderByIncidentAtDesc(childId)
            : incidentRepository.findByChildIdAndIncidentAtBetweenOrderByIncidentAtDesc(
                childId,
                AttendanceDates.startOfDay(child.getNursery(), date),
                AttendanceDates.startOfNextDay(child.getNursery(), date));
        return found.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<IncidentResponse> listForRoom(UUID roomId, User teacher) {
        if (!teacherAccess.canAccessRoom(teacher, roomId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found");
        }
        return incidentRepository.findByRoomIdOrderByIncidentAtDesc(roomId).stream()
            .map(this::toResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<AcknowledgmentRow> listAcknowledgments(UUID incidentId, User teacher) {
        IncidentReport incident = loadAccessibleByTeacher(incidentId, teacher);
        List<GuardianLink> links = guardianLinkRepository.findByChildIdOrderByCreatedAtAsc(incident.getChild().getId());
        List<IncidentAcknowledgment> acks = ackRepository.findByIncidentId(incident.getId());
        Map<UUID, Instant> ackedByGuardian = new HashMap<>();
        for (IncidentAcknowledgment a : acks) {
            ackedByGuardian.put(a.getGuardian().getId(), a.getAcknowledgedAt());
        }
        List<AcknowledgmentRow> rows = new ArrayList<>();
        for (GuardianLink link : links) {
            User g = link.getGuardian();
            Instant when = ackedByGuardian.get(g.getId());
            rows.add(new AcknowledgmentRow(
                g.getId(), g.getFullName(), g.getEmail(),
                link.getRelationship(), when != null, when
            ));
        }
        return rows;
    }

    private Child requireAccessibleChild(UUID childId, User teacher) {
        Child child = childRepository.findById(childId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Child not found"));
        if (!teacherAccess.canAccessChild(teacher, child)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Child not found");
        }
        return child;
    }

    private IncidentReport loadAccessibleByTeacher(UUID incidentId, User teacher) {
        IncidentReport incident = incidentRepository.findById(incidentId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Incident not found"));
        if (!teacherAccess.canAccessChild(teacher, incident.getChild())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Incident not found");
        }
        return incident;
    }

    private IncidentResponse toResponse(IncidentReport r) {
        long total = guardianLinkRepository.findByChildIdOrderByCreatedAtAsc(r.getChild().getId()).size();
        long acked = ackRepository.countByIncidentId(r.getId());
        return IncidentResponse.of(r, total, acked);
    }
}
