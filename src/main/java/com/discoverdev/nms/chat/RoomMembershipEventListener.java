package com.discoverdev.nms.chat;

import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class RoomMembershipEventListener {

    private static final Logger log = LoggerFactory.getLogger(RoomMembershipEventListener.class);

    private final RoomGroupSyncService sync;

    public RoomMembershipEventListener(RoomGroupSyncService sync) {
        this.sync = sync;
    }

    /**
     * After commit, so the reconcile reads the move that actually happened rather
     * than the half-applied state inside the transaction that caused it.
     *
     * A failure here must not roll back the enrolment or the room change that
     * triggered it — a group chat one member out of date is a far smaller problem
     * than a child who could not be moved. The next reconcile of that room fixes
     * it, since the operation is idempotent.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onMembershipChanged(RoomMembershipChangedEvent event) {
        if (event.isEmpty()) return;
        try {
            sync.reconcileAll(event.roomIds().toArray(UUID[]::new));
        } catch (RuntimeException e) {
            log.error("Failed to reconcile room group membership for {}: {}",
                event.roomIds(), e.getMessage(), e);
        }
    }
}
