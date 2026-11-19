package com.discoverdev.nms.chat;

import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.time.ZoneOffset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Chat auto-deletion (review finding CHAT-01: the PRD keeps chat for six months). Every night,
 * messages older than the retention period are soft-deleted: they disappear from every
 * conversation like a "delete for everyone", but the rows stay.
 */
@Component
public class ChatRetentionJob {

    private static final Logger log = LoggerFactory.getLogger(ChatRetentionJob.class);

    private final EntityManager em;
    private final int retentionMonths;

    public ChatRetentionJob(EntityManager em, @Value("${app.chat.retention-months:6}") int retentionMonths) {
        this.em = em;
        this.retentionMonths = retentionMonths;
    }

    @Scheduled(cron = "0 45 3 * * *")
    @Transactional
    public void purgeOldMessages() {
        Instant cutoff = Instant.now().atZone(ZoneOffset.UTC).minusMonths(retentionMonths).toInstant();
        int n = em.createNativeQuery(
                "UPDATE chat_messages SET deleted_at = now() WHERE deleted_at IS NULL AND created_at < :cutoff")
            .setParameter("cutoff", cutoff)
            .executeUpdate();
        if (n > 0) {
            log.info("Chat retention: deleted {} messages older than {} months", n, retentionMonths);
        }
    }
}
