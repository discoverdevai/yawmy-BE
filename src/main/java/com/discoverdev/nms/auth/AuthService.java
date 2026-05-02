package com.discoverdev.nms.auth;

import com.discoverdev.nms.auth.dto.TokenResponse;
import com.discoverdev.nms.config.JwtProperties;
import com.discoverdev.nms.config.PasswordResetProperties;
import com.discoverdev.nms.user.User;
import com.discoverdev.nms.user.UserRepository;
import java.time.Duration;
import java.time.Instant;
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
        this.refreshTokenTtl = Duration.ofDays(jwtProps.refreshTokenTtlDays());
        this.passwordResetTtl = Duration.ofMinutes(resetProps.ttlMinutes());
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

    @Transactional
    public void logout(User user) {
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
