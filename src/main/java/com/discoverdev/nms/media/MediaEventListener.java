package com.discoverdev.nms.media;

import com.discoverdev.nms.notification.NotificationFanout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class MediaEventListener {

    private static final Logger log = LoggerFactory.getLogger(MediaEventListener.class);

    private final NotificationFanout fanout;

    public MediaEventListener(NotificationFanout fanout) {
        this.fanout = fanout;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onUploaded(MediaUploadedEvent event) {
        log.debug(
            "Media uploaded — nursery={} child={} room={} count={} by={}",
            event.nurseryId(), event.childId(), event.roomId(),
            event.mediaItemIds().size(), event.uploaderUserId()
        );
        try {
            fanout.fanoutMediaUploaded(event);
        } catch (RuntimeException e) {
            log.error("Failed to fan out media notifications: {}", e.getMessage(), e);
        }
    }
}
