package com.discoverdev.nms.announcement;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class AnnouncementEventListener {

    private static final Logger log = LoggerFactory.getLogger(AnnouncementEventListener.class);

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onPublished(AnnouncementPublishedEvent event) {
        log.info(
            "Announcement published — nursery={} announcement={} scope={} room={} by={}",
            event.nurseryId(), event.announcementId(),
            event.scope(), event.roomId(), event.authorUserId()
        );
    }
}
