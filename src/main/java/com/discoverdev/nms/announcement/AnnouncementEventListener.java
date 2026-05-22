package com.discoverdev.nms.announcement;

import com.discoverdev.nms.notification.NotificationFanout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class AnnouncementEventListener {

    private static final Logger log = LoggerFactory.getLogger(AnnouncementEventListener.class);

    private final NotificationFanout fanout;

    public AnnouncementEventListener(NotificationFanout fanout) {
        this.fanout = fanout;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onPublished(AnnouncementPublishedEvent event) {
        log.debug(
            "Announcement published — nursery={} announcement={} scope={}",
            event.nurseryId(), event.announcementId(), event.scope()
        );
        try {
            fanout.fanoutAnnouncementPublished(event);
        } catch (RuntimeException e) {
            log.error("Failed to fan out announcement notifications: {}", e.getMessage(), e);
        }
    }
}
