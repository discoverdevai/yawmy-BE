package com.discoverdev.nms.event;

import com.discoverdev.nms.notification.NotificationFanout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class EventListener {

    private static final Logger log = LoggerFactory.getLogger(EventListener.class);

    private final NotificationFanout fanout;

    public EventListener(NotificationFanout fanout) {
        this.fanout = fanout;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onCreated(EventCreatedEvent event) {
        log.debug(
            "Event created — nursery={} event={} scope={}",
            event.nurseryId(), event.eventId(), event.scope()
        );
        try {
            fanout.fanoutEventCreated(event);
        } catch (RuntimeException e) {
            log.error("Failed to fan out event notifications: {}", e.getMessage(), e);
        }
    }
}
