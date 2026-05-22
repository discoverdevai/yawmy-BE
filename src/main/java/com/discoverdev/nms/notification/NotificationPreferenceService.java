package com.discoverdev.nms.notification;

import com.discoverdev.nms.notification.dto.PreferencesResponse;
import com.discoverdev.nms.notification.dto.UpdatePreferencesRequest;
import com.discoverdev.nms.user.User;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class NotificationPreferenceService {

    private final NotificationPreferenceRepository repository;

    public NotificationPreferenceService(NotificationPreferenceRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public PreferencesResponse get(User user) {
        return repository.findByUserId(user.getId())
            .map(PreferencesResponse::of)
            .orElseGet(PreferencesResponse::defaults);
    }

    @Transactional
    public PreferencesResponse update(User user, UpdatePreferencesRequest req) {
        NotificationPreference pref = repository.findByUserId(user.getId())
            .orElseGet(() -> NotificationPreference.builder()
                .user(user)
                .announcementsEnabled(true)
                .build());
        if (Boolean.TRUE.equals(req.clearQuietHours())) {
            pref.setQuietHoursStart(null);
            pref.setQuietHoursEnd(null);
        } else {
            if (req.quietHoursStart() != null) pref.setQuietHoursStart(req.quietHoursStart());
            if (req.quietHoursEnd() != null) pref.setQuietHoursEnd(req.quietHoursEnd());
        }
        if (req.announcementsEnabled() != null) pref.setAnnouncementsEnabled(req.announcementsEnabled());
        if (pref.getQuietHoursStart() != null && pref.getQuietHoursEnd() != null
            && pref.getQuietHoursStart().equals(pref.getQuietHoursEnd())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "quietHoursStart and quietHoursEnd cannot be identical");
        }
        return PreferencesResponse.of(repository.save(pref));
    }
}
