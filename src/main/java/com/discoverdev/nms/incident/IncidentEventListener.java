package com.discoverdev.nms.incident;

import com.discoverdev.nms.notification.NotificationFanout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class IncidentEventListener {

    private static final Logger log = LoggerFactory.getLogger(IncidentEventListener.class);

    private final NotificationFanout fanout;

    public IncidentEventListener(NotificationFanout fanout) {
        this.fanout = fanout;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onReported(IncidentReportedEvent event) {
        log.debug("Incident reported — nursery={} child={} incident={}",
            event.nurseryId(), event.childId(), event.incidentId());
        try {
            fanout.fanoutIncidentReported(event);
        } catch (RuntimeException e) {
            log.error("Failed to fan out incident-reported notifications: {}", e.getMessage(), e);
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onAcknowledged(IncidentAcknowledgedEvent event) {
        log.debug("Incident acknowledged — nursery={} incident={} guardian={}",
            event.nurseryId(), event.incidentId(), event.guardianUserId());
        try {
            fanout.fanoutIncidentAcknowledged(event);
        } catch (RuntimeException e) {
            log.error("Failed to fan out incident-acknowledged notifications: {}", e.getMessage(), e);
        }
    }
}
