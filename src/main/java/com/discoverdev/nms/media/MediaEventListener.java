package com.discoverdev.nms.media;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class MediaEventListener {

    private static final Logger log = LoggerFactory.getLogger(MediaEventListener.class);

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onUploaded(MediaUploadedEvent event) {
        log.info(
            "Media uploaded — nursery={} child={} room={} count={} by={}",
            event.nurseryId(), event.childId(), event.roomId(),
            event.mediaItemIds().size(), event.uploaderUserId()
        );
    }
}
