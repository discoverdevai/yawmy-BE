package com.discoverdev.nms.dailylog;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class DailyLogPublishListener {

    private static final Logger log = LoggerFactory.getLogger(DailyLogPublishListener.class);

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onPublished(DailyLogEntriesPublishedEvent event) {
        log.info(
            "DailyLog entries published — nursery={} child={} log={} entries={} by={}",
            event.nurseryId(), event.childId(), event.dailyLogId(),
            event.entryIds().size(), event.publishedByUserId()
        );
    }
}
