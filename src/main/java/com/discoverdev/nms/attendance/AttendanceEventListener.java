package com.discoverdev.nms.attendance;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class AttendanceEventListener {

    private static final Logger log = LoggerFactory.getLogger(AttendanceEventListener.class);

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onRecorded(AttendanceEventRecordedEvent event) {
        log.info(
            "Attendance recorded — nursery={} child={} type={} source={} by={} at={}",
            event.nurseryId(), event.childId(), event.type(),
            event.source(), event.recordedByUserId(), event.recordedAt()
        );
    }
}
