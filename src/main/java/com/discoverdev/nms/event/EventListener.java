package com.discoverdev.nms.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class EventListener {

    private static final Logger log = LoggerFactory.getLogger(EventListener.class);

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onCreated(EventCreatedEvent event) {
        log.info(
            "Event created — nursery={} event={} scope={} room={} starts={} by={}",
            event.nurseryId(), event.eventId(),
            event.scope(), event.roomId(),
            event.startsAt(), event.authorUserId()
        );
    }
}
