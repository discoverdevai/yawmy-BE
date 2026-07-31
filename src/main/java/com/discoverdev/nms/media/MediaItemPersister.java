package com.discoverdev.nms.media;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Writes one media row in its own transaction, immediately.
 *
 * MediaItem's id is generated in the application, so Hibernate has no reason to
 * touch the database until the surrounding transaction commits. That defeats
 * MediaWriter's per-file error handling entirely: a row the database rejects
 * fails *after* the loop has finished, escapes the per-file catch, and rolls
 * back every good file uploaded alongside it. (That is how one unsupported PDF
 * took a perfectly valid video down with it.)
 *
 * REQUIRES_NEW plus an explicit flush moves the failure back inside the loop,
 * where it belongs to a single file and the rest of the batch survives — which
 * is what BulkUploadResponse's `uploaded` / `failed` split has always promised.
 *
 * This has to be a separate bean: @Transactional on a method MediaWriter calls
 * on itself bypasses the proxy and does nothing at all.
 */
@Component
public class MediaItemPersister {

    private final MediaItemRepository repository;

    public MediaItemPersister(MediaItemRepository repository) {
        this.repository = repository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public MediaItem saveNow(MediaItem item) {
        return repository.saveAndFlush(item);
    }
}
