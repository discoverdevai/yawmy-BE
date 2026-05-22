package com.discoverdev.nms.incident;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class IncidentEventListener {

    private static final Logger log = LoggerFactory.getLogger(IncidentEventListener.class);

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onReported(IncidentReportedEvent event) {
        log.info(
            "Incident reported — nursery={} child={} incident={} author={} at={}",
            event.nurseryId(), event.childId(), event.incidentId(),
            event.authorUserId(), event.incidentAt()
        );
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onAcknowledged(IncidentAcknowledgedEvent event) {
        log.info(
            "Incident acknowledged — nursery={} child={} incident={} guardian={} at={}",
            event.nurseryId(), event.childId(), event.incidentId(),
            event.guardianUserId(), event.acknowledgedAt()
        );
    }
}
