package com.discoverdev.nms.chat;

import com.discoverdev.nms.media.MediaWriter;
import com.discoverdev.nms.media.dto.BulkUploadResponse;
import com.discoverdev.nms.nursery.Nursery;
import com.discoverdev.nms.user.User;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

/**
 * Uploads for chat attachments and voice notes.
 *
 * Sending a file is two steps: upload here to get media ids, then reference them
 * as `attachmentMediaIds` on the message. The upload is deliberately not tied to
 * a thread — a photo may be picked before the user decides which conversation to
 * send it to, and an unreferenced media row is harmless.
 */
@Service
public class ChatMediaService {

    private static final int MAX_FILES_PER_REQUEST = 10;

    private final MediaWriter writer;

    public ChatMediaService(MediaWriter writer) {
        this.writer = writer;
    }

    @Transactional
    public BulkUploadResponse upload(User uploader, List<MultipartFile> files, Long durationMs) {
        Nursery nursery = uploader.getNursery();
        if (nursery == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                "User is not attached to a nursery");
        }
        if (files == null || files.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No files provided");
        }
        if (files.size() > MAX_FILES_PER_REQUEST) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "At most " + MAX_FILES_PER_REQUEST + " files per upload");
        }
        return writer.writeForChat(nursery, files, uploader, durationMs);
    }
}
