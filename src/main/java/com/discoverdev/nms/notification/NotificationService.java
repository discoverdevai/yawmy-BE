package com.discoverdev.nms.notification;

import com.discoverdev.nms.notification.dto.NotificationResponse;
import com.discoverdev.nms.user.User;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class NotificationService {

    private final NotificationRepository repository;

    public NotificationService(NotificationRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> list(User user, String status) {
        boolean unreadOnly = "unread".equalsIgnoreCase(status);
        List<Notification> items = unreadOnly
            ? repository.findByRecipientIdAndReadAtIsNullAndDeletedAtIsNullOrderByCreatedAtDesc(user.getId())
            : repository.findByRecipientIdAndDeletedAtIsNullOrderByCreatedAtDesc(user.getId());
        return items.stream().map(NotificationResponse::of).toList();
    }

    @Transactional(readOnly = true)
    public long unreadCount(User user) {
        return repository.countByRecipientIdAndReadAtIsNullAndDeletedAtIsNull(user.getId());
    }

    @Transactional
    public NotificationResponse markRead(UUID id, User user) {
        Notification n = repository.findByIdAndRecipientId(id, user.getId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification not found"));
        if (n.getReadAt() == null) {
            n.setReadAt(Instant.now());
            repository.save(n);
        }
        return NotificationResponse.of(n);
    }

    @Transactional
    public int markAllRead(User user) {
        return repository.markAllRead(user.getId(), Instant.now());
    }

    @Transactional
    public void softDelete(UUID id, User user) {
        Notification n = repository.findByIdAndRecipientId(id, user.getId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification not found"));
        if (n.getDeletedAt() == null) {
            n.setDeletedAt(Instant.now());
            repository.save(n);
        }
    }

    @Transactional
    public int deleteAll(User user) {
        return repository.softDeleteAll(user.getId(), Instant.now());
    }
}
