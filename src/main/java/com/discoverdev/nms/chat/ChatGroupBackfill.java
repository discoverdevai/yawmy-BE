package com.discoverdev.nms.chat;

import com.discoverdev.nms.nursery.Room;
import com.discoverdev.nms.nursery.RoomRepository;
import com.discoverdev.nms.user.Role;
import com.discoverdev.nms.user.User;
import com.discoverdev.nms.user.UserRepository;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Makes every room's group chat match the rules, once per boot.
 *
 * Two things need repairing on a database that predates this feature. Rooms
 * created before group chats existed have no thread at all, so nobody can open
 * one until a teacher happens to. And rooms whose thread was built under the
 * older rule are missing their admins, which is the confusing case: the teachers
 * and parents are there, an admin looking for the same conversation cannot find
 * it, cannot post to it, and is refused the WebSocket subscription — with no
 * error that explains why.
 *
 * From then on the event listener keeps membership current, so this exists for
 * history rather than for steady-state. It stays on anyway: reconciling is
 * idempotent, so a boot after any drift — a failed event, a manual database
 * edit, a restore — silently puts things right.
 *
 * Runs after the application is ready, not during startup, so a cold start still
 * begins serving requests at the usual moment.
 */
@Component
public class ChatGroupBackfill {

    private static final Logger log = LoggerFactory.getLogger(ChatGroupBackfill.class);

    private final RoomRepository roomRepository;
    private final UserRepository userRepository;
    private final ChatAccessService chatAccess;
    private final RoomGroupSyncService sync;
    private final boolean enabled;

    public ChatGroupBackfill(
        RoomRepository roomRepository,
        UserRepository userRepository,
        ChatAccessService chatAccess,
        RoomGroupSyncService sync,
        @Value("${app.chat.reconcile-on-startup:true}") boolean enabled
    ) {
        this.roomRepository = roomRepository;
        this.userRepository = userRepository;
        this.chatAccess = chatAccess;
        this.sync = sync;
        this.enabled = enabled;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void reconcileAllRooms() {
        if (!enabled) {
            log.info("Chat group reconcile skipped (app.chat.reconcile-on-startup=false).");
            return;
        }
        List<Room> rooms = roomRepository.findAll();
        int created = 0;
        int reconciled = 0;
        int failed = 0;

        for (Room room : rooms) {
            try {
                if (room.getArchivedAt() != null) {
                    // An archived room's chat should hold nobody. reconcile()
                    // empties it, and does nothing if there is no thread.
                    sync.reconcile(room.getId());
                    reconciled++;
                    continue;
                }
                // createdBy cannot be null, and a backfilled thread has no real
                // author — an admin of the same nursery is the closest honest
                // answer, and the only user guaranteed to belong in every group.
                User owner = firstAdminOf(room);
                if (owner == null) {
                    // A nursery with no admin yet (invited, not accepted). The
                    // group will be created the first time anyone opens it.
                    sync.reconcile(room.getId());
                    reconciled++;
                    continue;
                }
                chatAccess.ensureRoomGroup(room, owner);
                created++;
            } catch (RuntimeException e) {
                failed++;
                log.warn("Could not reconcile group chat for room {}: {}", room.getId(), e.getMessage());
            }
        }

        if (failed > 0) {
            log.warn("Chat group reconcile finished with {} failures — {} rooms ensured, {} reconciled.",
                failed, created, reconciled);
        } else {
            log.info("Chat group reconcile complete — {} rooms ensured, {} reconciled.",
                created, reconciled);
        }
    }

    private User firstAdminOf(Room room) {
        if (room.getNursery() == null) return null;
        return userRepository
            .findByNurseryIdAndRoleAndEnabledTrueOrderByFullNameAsc(room.getNursery().getId(), Role.ADMIN)
            .stream()
            .findFirst()
            .orElse(null);
    }
}
