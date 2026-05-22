package com.discoverdev.nms.attendance;

import com.discoverdev.nms.notification.NotificationFanout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class AttendanceEventListener {

    private static final Logger log = LoggerFactory.getLogger(AttendanceEventListener.class);

    private final NotificationFanout fanout;

    public AttendanceEventListener(NotificationFanout fanout) {
        this.fanout = fanout;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onRecorded(AttendanceEventRecordedEvent event) {
        log.debug(
            "Attendance recorded — nursery={} child={} type={} source={} by={} at={}",
            event.nurseryId(), event.childId(), event.type(),
            event.source(), event.recordedByUserId(), event.recordedAt()
        );
        try {
            fanout.fanoutAttendance(event);
        } catch (RuntimeException e) {
            log.error("Failed to fan out attendance notifications: {}", e.getMessage(), e);
        }
    }
}
