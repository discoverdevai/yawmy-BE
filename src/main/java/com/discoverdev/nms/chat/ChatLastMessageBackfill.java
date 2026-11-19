package com.discoverdev.nms.chat;

import jakarta.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Fills in last_message_at for threads that have messages but no value. Sending a message used to
 * store null there (the message's creation time wasn't set yet), which left the inbox unordered
 * (Jira YC-34, YC-60). Idempotent: once every thread has its value it updates nothing.
 */
@Component
public class ChatLastMessageBackfill {

    private static final Logger log = LoggerFactory.getLogger(ChatLastMessageBackfill.class);

    private final EntityManager em;

    public ChatLastMessageBackfill(EntityManager em) {
        this.em = em;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void fillMissing() {
        int fixed = em.createNativeQuery("""
            UPDATE chat_threads t
               SET last_message_at = (SELECT max(m.created_at) FROM chat_messages m WHERE m.thread_id = t.id)
             WHERE t.last_message_at IS NULL
               AND EXISTS (SELECT 1 FROM chat_messages m WHERE m.thread_id = t.id)
            """).executeUpdate();
        if (fixed > 0) log.info("Chat inbox order: filled last_message_at on {} threads.", fixed);
    }
}
