package com.discoverdev.nms.auth;

import com.discoverdev.nms.auth.dto.InviteValidationResponse;
import com.discoverdev.nms.auth.dto.TokenResponse;
import com.discoverdev.nms.config.JwtProperties;
import com.discoverdev.nms.config.PasswordResetProperties;
import com.discoverdev.nms.user.Role;
import com.discoverdev.nms.user.User;
import com.discoverdev.nms.user.UserRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final InviteRepository inviteRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final MailService mailService;
    private final Duration refreshTokenTtl;
    private final Duration passwordResetTtl;

    public AuthService(
        AuthenticationManager authenticationManager,
        UserRepository userRepository,
        InviteRepository inviteRepository,
        RefreshTokenRepository refreshTokenRepository,
        PasswordResetTokenRepository passwordResetTokenRepository,
        PasswordEncoder passwordEncoder,
        JwtService jwtService,
        MailService mailService,
        JwtProperties jwtProps,
        PasswordResetProperties resetProps
    ) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.inviteRepository = inviteRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.mailService = mailService;
        this.refreshTokenTtl = Duration.ofDays(jwtProps.refreshTokenTtlDays());
        this.passwordResetTtl = Duration.ofMinutes(resetProps.ttlMinutes());
    }

    @Transactional(readOnly = true)
    public void requestReactivation(String email, String message) {
        List<User> superAdmins = userRepository.findByRole(Role.SUPER_ADMIN);
        for (User superAdmin : superAdmins) {
            mailService.sendReactivationNotice(superAdmin.getEmail(), email, message);
        }
        log.info("Reactivation requested for {} -> notified {} super-admin(s)", email, superAdmins.size());
    }

    @Transactional
    public TokenResponse login(String email, String rawPassword) {
        try {
            authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, rawPassword));
        } catch (Exception e) {
            throw new AuthException("Invalid email or password");
        }

        User user = userRepository.findByEmailIgnoreCase(email)
            .orElseThrow(() -> new AuthException("Invalid email or password"));

        if (user.getPasswordHash() == null) {
            throw new AuthException("Account not activated. Please accept your invite first.");
        }

        return issueTokens(user);
    }

    @Transactional
    public TokenResponse refresh(String rawRefreshToken) {
        String tokenHash = Tokens.hash(rawRefreshToken);
        RefreshToken token = refreshTokenRepository.findByTokenHash(tokenHash)
            .orElseThrow(() -> new AuthException("Invalid refresh token"));

        if (!token.isActive()) {
            throw new AuthException("Refresh token expired or revoked");
        }

        token.setRevokedAt(Instant.now());
        refreshTokenRepository.save(token);

        return issueTokens(token.getUser());
    }

    /**
     * Sign out a single device: revokes only the refresh token that device holds.
     * If no refresh token is supplied we cannot tell the devices apart, so we fall
     * back to revoking every session (safer than revoking nothing).
     */
    @Transactional
    public void logout(User user, String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            logoutAll(user);
            return;
        }
        refreshTokenRepository.findByTokenHash(Tokens.hash(rawRefreshToken))
            .filter(token -> token.getUser().getId().equals(user.getId()))
            .ifPresent(token -> {
                if (token.getRevokedAt() == null) {
                    token.setRevokedAt(Instant.now());
                    refreshTokenRepository.save(token);
                }
            });
        log.info("Signed out one device for user {}", user.getId());
    }

    /** Sign out everywhere: revokes every active refresh token for the user. */
    @Transactional
    public void logoutAll(User user) {
        int revoked = refreshTokenRepository.revokeAllForUser(user, Instant.now());
        log.info("Revoked {} refresh tokens for user {}", revoked, user.getId());
    }

    @Transactional
    public TokenResponse acceptInvite(String rawInviteToken, String newPassword) {
        String tokenHash = Tokens.hash(rawInviteToken);
        Invite invite = inviteRepository.findByTokenHash(tokenHash)
            .orElseThrow(() -> new AuthException("Invalid invite"));

        if (invite.isUsed()) {
            throw new AuthException("Invite already used");
        }
        if (invite.isExpired()) {
            throw new AuthException("Invite expired");
        }

        User user = invite.getUser();
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setEnabled(true);
        userRepository.save(user);

        invite.setUsedAt(Instant.now());
        inviteRepository.save(invite);

        return issueTokens(user);
    }

    @Transactional(readOnly = true)
    public InviteValidationResponse validateInvite(String rawInviteToken) {
        String tokenHash = Tokens.hash(rawInviteToken);
        Invite invite = inviteRepository.findByTokenHash(tokenHash).orElse(null);
        if (invite == null) {
            return InviteValidationResponse.invalid();
        }

        String status;
        if (invite.isUsed()) {
            status = "USED";
        } else if (invite.isExpired()) {
            status = "EXPIRED";
        } else {
            status = "VALID";
        }

        User user = invite.getUser();
        return new InviteValidationResponse(
            status,
            user.getEmail(),
            user.getFullName(),
            user.getRole(),
            invite.getExpiresAt()
        );
    }

    @Transactional
    public void forgotPassword(String email) {
        userRepository.findByEmailIgnoreCase(email).ifPresentOrElse(user -> {
            String raw = Tokens.generateRawToken();
            PasswordResetToken reset = PasswordResetToken.builder()
                .user(user)
                .tokenHash(Tokens.hash(raw))
                .expiresAt(Instant.now().plus(passwordResetTtl))
                .build();
            passwordResetTokenRepository.save(reset);
            log.info("Password reset token issued for {}: {}", user.getEmail(), raw);
        }, () -> log.info("Password reset requested for unknown email: {}", email));
    }

    @Transactional
    public void resetPassword(String rawResetToken, String newPassword) {
        String tokenHash = Tokens.hash(rawResetToken);
        PasswordResetToken token = passwordResetTokenRepository.findByTokenHash(tokenHash)
            .orElseThrow(() -> new AuthException("Invalid reset token"));

        if (token.isUsed()) {
            throw new AuthException("Reset token already used");
        }
        if (token.isExpired()) {
            throw new AuthException("Reset token expired");
        }

        User user = token.getUser();
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        token.setUsedAt(Instant.now());
        passwordResetTokenRepository.save(token);

        refreshTokenRepository.revokeAllForUser(user, Instant.now());
    }

    /**
     * Change the password of an already-authenticated user. Requires the current
     * password (no emailed token). Revokes refresh tokens so other sessions must
     * sign in again with the new password.
     */
    @Transactional
    public void changePassword(User user, String currentPassword, String newPassword) {
        if (user.getPasswordHash() == null
            || !passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new AuthException("Current password is incorrect");
        }
        if (passwordEncoder.matches(newPassword, user.getPasswordHash())) {
            throw new AuthException("New password must be different from the current password");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        refreshTokenRepository.revokeAllForUser(user, Instant.now());
        log.info("Password changed for user {}", user.getId());
    }

    private TokenResponse issueTokens(User user) {
        String accessToken = jwtService.issueAccessToken(user);
        String rawRefresh = Tokens.generateRawToken();
        RefreshToken refresh = RefreshToken.builder()
            .user(user)
            .tokenHash(Tokens.hash(rawRefresh))
            .expiresAt(Instant.now().plus(refreshTokenTtl))
            .build();
        refreshTokenRepository.save(refresh);
        return TokenResponse.bearer(accessToken, rawRefresh, jwtService.getAccessTokenTtl().toSeconds());
    }
}
