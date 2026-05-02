package com.discoverdev.nms.auth;

import com.discoverdev.nms.config.InviteProperties;
import com.discoverdev.nms.nursery.Nursery;
import com.discoverdev.nms.user.Locale;
import com.discoverdev.nms.user.Role;
import com.discoverdev.nms.user.User;
import com.discoverdev.nms.user.UserRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class UserInvitationFactory {

    private final UserRepository userRepository;
    private final InviteRepository inviteRepository;
    private final Duration inviteTtl;

    public UserInvitationFactory(
        UserRepository userRepository,
        InviteRepository inviteRepository,
        InviteProperties props
    ) {
        this.userRepository = userRepository;
        this.inviteRepository = inviteRepository;
        this.inviteTtl = Duration.ofHours(props.ttlHours());
    }

    public IssuedInvite createInvite(
        String email,
        String fullName,
        Role role,
        Locale locale,
        String phone,
        Nursery nursery,
        User invitedBy
    ) {
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
        }

        User user = User.builder()
            .nursery(nursery)
            .email(email)
            .fullName(fullName)
            .role(role)
            .locale(locale != null ? locale : Locale.EN)
            .phone(phone)
            .enabled(false)
            .build();
        userRepository.save(user);

        String rawToken = Tokens.generateRawToken();
        Instant expiresAt = Instant.now().plus(inviteTtl);
        Invite invite = Invite.builder()
            .user(user)
            .tokenHash(Tokens.hash(rawToken))
            .expiresAt(expiresAt)
            .invitedBy(invitedBy)
            .build();
        inviteRepository.save(invite);

        return new IssuedInvite(
            invite.getId(),
            user.getId(),
            user.getEmail(),
            user.getRole(),
            rawToken,
            expiresAt
        );
    }

    public record IssuedInvite(
        UUID inviteId,
        UUID userId,
        String email,
        Role role,
        String token,
        Instant expiresAt
    ) {}
}
