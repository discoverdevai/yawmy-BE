package com.discoverdev.nms.incident;

import com.discoverdev.nms.child.Child;
import com.discoverdev.nms.child.EnrollmentStatus;
import com.discoverdev.nms.child.GuardianLink;
import com.discoverdev.nms.child.GuardianLinkRepository;
import com.discoverdev.nms.incident.dto.ParentIncidentResponse;
import com.discoverdev.nms.user.User;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ParentIncidentService {

    private final IncidentReportRepository incidentRepository;
    private final IncidentAcknowledgmentRepository ackRepository;
    private final GuardianLinkRepository guardianLinkRepository;
    private final ApplicationEventPublisher events;

    public ParentIncidentService(
        IncidentReportRepository incidentRepository,
        IncidentAcknowledgmentRepository ackRepository,
        GuardianLinkRepository guardianLinkRepository,
        ApplicationEventPublisher events
    ) {
        this.incidentRepository = incidentRepository;
        this.ackRepository = ackRepository;
        this.guardianLinkRepository = guardianLinkRepository;
        this.events = events;
    }

    @Transactional(readOnly = true)
    public List<ParentIncidentResponse> listForChild(UUID childId, User parent) {
        Child child = requireLinkedEnrolledChild(childId, parent);
        return incidentRepository.findByChildIdOrderByIncidentAtDesc(child.getId()).stream()
            .map(r -> toParentResponse(r, parent))
            .toList();
    }

    @Transactional(readOnly = true)
    public ParentIncidentResponse get(UUID incidentId, User parent) {
        IncidentReport incident = loadLinked(incidentId, parent);
        return toParentResponse(incident, parent);
    }

    @Transactional
    public ParentIncidentResponse acknowledge(UUID incidentId, User parent) {
        IncidentReport incident = loadLinked(incidentId, parent);
        IncidentAcknowledgment ack = ackRepository
            .findByIncidentIdAndGuardianId(incidentId, parent.getId())
            .orElse(null);
        if (ack == null) {
            Instant now = Instant.now();
            IncidentAcknowledgment saved = ackRepository.save(IncidentAcknowledgment.builder()
                .incident(incident)
                .guardian(parent)
                .acknowledgedAt(now)
                .build());
            events.publishEvent(new IncidentAcknowledgedEvent(
                incident.getNursery().getId(), incident.getChild().getId(),
                incident.getId(), parent.getId(), saved.getAcknowledgedAt()
            ));
        }
        return toParentResponse(incident, parent);
    }

    private IncidentReport loadLinked(UUID incidentId, User parent) {
        IncidentReport incident = incidentRepository.findById(incidentId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Incident not found"));
        Child child = incident.getChild();
        if (child.getEnrollmentStatus() != EnrollmentStatus.ENROLLED) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Incident not found");
        }
        if (!guardianLinkRepository.existsByChildIdAndGuardianId(child.getId(), parent.getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Incident not found");
        }
        return incident;
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

    private ParentIncidentResponse toParentResponse(IncidentReport r, User parent) {
        Instant ackedAt = ackRepository.findByIncidentIdAndGuardianId(r.getId(), parent.getId())
            .map(IncidentAcknowledgment::getAcknowledgedAt)
            .orElse(null);
        return ParentIncidentResponse.of(r, ackedAt);
    }
}
