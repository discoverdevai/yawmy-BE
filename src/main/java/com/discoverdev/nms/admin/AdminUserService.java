package com.discoverdev.nms.admin;

import com.discoverdev.nms.admin.dto.AdminUserResponse;
import com.discoverdev.nms.auth.RefreshTokenRepository;
import com.discoverdev.nms.user.AccessTokensInvalidatedEvent;
import com.discoverdev.nms.user.User;
import com.discoverdev.nms.user.UserRepository;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AdminUserService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final ApplicationEventPublisher events;

    public AdminUserService(
        UserRepository userRepository,
        RefreshTokenRepository refreshTokenRepository,
        ApplicationEventPublisher events
    ) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.events = events;
    }

    @Transactional
    public AdminUserResponse disable(UUID targetUserId, User actor) {
        if (actor.getId().equals(targetUserId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You cannot disable your own account");
        }
        User user = loadUser(targetUserId);
        if (user.isEnabled()) {
            user.setEnabled(false);
            user.invalidateAccessTokens();
            userRepository.save(user);
            refreshTokenRepository.revokeAllForUser(user, Instant.now());
            events.publishEvent(new AccessTokensInvalidatedEvent(user.getId()));
        }
        return AdminUserResponse.of(user);
    }

    @Transactional
    public AdminUserResponse enable(UUID targetUserId) {
        User user = loadUser(targetUserId);
        if (!user.isEnabled()) {
            user.setEnabled(true);
            userRepository.save(user);
        }
        return AdminUserResponse.of(user);
    }

    private User loadUser(UUID id) {
        return userRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }
}
