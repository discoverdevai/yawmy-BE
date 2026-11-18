package com.discoverdev.nms.admin;

import com.discoverdev.nms.nursery.RoomRepository;
import com.discoverdev.nms.tenant.TenantContext;
import jakarta.persistence.EntityManager;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Delete Room (Jira YC-90): only a room that never had children or any activity can be
 * deleted; anything with history can only be archived. Reached with
 * DELETE /api/admin/rooms/{id}?permanent=true — plain DELETE keeps archiving, which the
 * current admin app relies on.
 */
@Service
public class RoomDeletionService {

    /** Every table that points at a room (besides its teachers and its group chat). */
    private static final String[] HISTORY_TABLES = {
        "children", "attendance_events", "incident_reports", "media_items",
        "room_sessions", "announcements", "nursery_events"
    };

    private final RoomRepository roomRepository;
    private final EntityManager em;

    public RoomDeletionService(RoomRepository roomRepository, EntityManager em) {
        this.roomRepository = roomRepository;
        this.em = em;
    }

    @Transactional
    public void delete(UUID roomId) {
        roomRepository.findByIdAndNurseryId(roomId, TenantContext.currentNurseryId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found"));

        long history = 0;
        for (String table : HISTORY_TABLES) {
            history += count("SELECT count(*) FROM " + table + " WHERE room_id = :room", roomId);
        }
        history += count("SELECT count(*) FROM chat_messages m JOIN chat_threads t ON t.id = m.thread_id"
            + " WHERE t.room_id = :room", roomId);
        if (history > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "This room has had children or activity, so it can only be archived");
        }

        // Nothing but its (empty) group chat and teacher links refer to it: remove them, then the room.
        String threads = "(SELECT id FROM chat_threads WHERE room_id = :room)";
        exec("DELETE FROM chat_thread_reads WHERE thread_id IN " + threads, roomId);
        exec("DELETE FROM chat_participants WHERE thread_id IN " + threads, roomId);
        exec("DELETE FROM chat_threads WHERE room_id = :room", roomId);
        exec("DELETE FROM teacher_room_assignments WHERE room_id = :room", roomId);
        exec("DELETE FROM rooms WHERE id = :room", roomId);
        em.clear();
    }

    private long count(String sql, UUID roomId) {
        return ((Number) em.createNativeQuery(sql).setParameter("room", roomId).getSingleResult()).longValue();
    }

    private void exec(String sql, UUID roomId) {
        em.createNativeQuery(sql).setParameter("room", roomId).executeUpdate();
    }
}
