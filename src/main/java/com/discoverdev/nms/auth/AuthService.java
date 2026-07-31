package com.discoverdev.nms.auth;

import com.discoverdev.nms.auth.dto.ForgotPasswordResponse;
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
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

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
    private final Duration otpTtl;

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
        this.otpTtl = Duration.ofSeconds(resetProps.otpTtlSeconds());
    }

    @Transactional(readOnly = true)
    public void requestReactivation(String email, String message) {
        // The email must belong to someone who was actually invited and hasn't
        // activated yet (i.e. an outstanding/expired invite). Otherwise reject.
        User user = userRepository.findByEmailIgnoreCase(email)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                "Email not found or incorrect email. Please enter the email your invitation was sent to."));

        if (user.isEnabled()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "This account is already active. Please sign in instead.");
        }

        // Notify whoever can actually re-issue the invite. For a teacher that is the
        // admins of their own nursery — they sent the invite and they own the staff
        // list. Super-admins are the fallback, and remain the only route for a
        // pending admin, whose nursery has no active admin yet.
        String nurseryName = user.getNursery() != null ? user.getNursery().getName() : null;
        List<User> recipients = List.of();
        if (user.getRole() == Role.TEACHER && user.getNursery() != null) {
            recipients = userRepository
                .findByNurseryIdAndRoleAndEnabledTrueAndDeletedAtIsNullOrderByFullNameAsc(
                    user.getNursery().getId(), Role.ADMIN);
        }
        String audience = "nursery admin(s)";
        if (recipients.isEmpty()) {
            recipients = userRepository.findByRole(Role.SUPER_ADMIN);
            audience = "super-admin(s)";
        }

        for (User recipient : recipients) {
            mailService.sendReactivationNotice(
                recipient.getEmail(), user.getEmail(), roleLabel(user.getRole()), nurseryName, message);
        }
        log.info("Reactivation requested for {} ({}) -> notified {} {}",
            email, user.getRole(), recipients.size(), audience);
    }

    private static String roleLabel(Role role) {
        return role == Role.TEACHER ? "teacher" : "admin";
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
        String nurseryName = user.getNursery() != null ? user.getNursery().getName() : null;
        return new InviteValidationResponse(
            status,
            user.getEmail(),
            user.getFullName(),
            user.getRole(),
            nurseryName,
            invite.getExpiresAt()
        );
    }

    /**
     * Start a password reset: validate the email exists, issue a reset token +
     * a short-lived OTP, and email the OTP. Returns the token (kept by the client)
     * and the OTP countdown in seconds.
     */
    @Transactional
    public ForgotPasswordResponse forgotPassword(String email) {
        User user = userRepository.findByEmailIgnoreCase(email)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                "This email is not registered"));

        // Clear any prior attempt so only the newest OTP is valid.
        passwordResetTokenRepository.deleteByUserId(user.getId());

        String rawToken = Tokens.generateRawToken();
        String otp = Tokens.generateOtp();
        PasswordResetToken reset = PasswordResetToken.builder()
            .user(user)
            .tokenHash(Tokens.hash(rawToken))
            .otpHash(Tokens.hash(otp))
            .expiresAt(Instant.now().plus(otpTtl))
            .build();
        passwordResetTokenRepository.save(reset);

        mailService.sendPasswordResetOtp(user.getEmail(), otp);
        log.info("Password reset OTP issued for {}", user.getEmail());
        return new ForgotPasswordResponse(rawToken, otpTtl.toSeconds());
    }

    /** Verify the OTP the user typed against the emailed code. */
    @Transactional
    public void verifyResetOtp(String rawToken, String otp) {
        PasswordResetToken token = loadResetToken(rawToken);
        if (token.isExpired()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "The code has expired. Please resend a new code.");
        }
        if (token.getOtpHash() == null || !token.getOtpHash().equals(Tokens.hash(otp))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Incorrect verification code");
        }
        token.setVerifiedAt(Instant.now());
        passwordResetTokenRepository.save(token);
    }

    /** Issue a fresh OTP for an in-progress reset (the "Resend code" action). */
    @Transactional
    public ForgotPasswordResponse resendResetOtp(String rawToken) {
        PasswordResetToken token = loadResetToken(rawToken);
        String otp = Tokens.generateOtp();
        token.setOtpHash(Tokens.hash(otp));
        token.setExpiresAt(Instant.now().plus(otpTtl));
        token.setVerifiedAt(null); // a new code must be re-verified
        passwordResetTokenRepository.save(token);

        mailService.sendPasswordResetOtp(token.getUser().getEmail(), otp);
        return new ForgotPasswordResponse(rawToken, otpTtl.toSeconds());
    }

    /** Final step: set the new password once the OTP has been verified. */
    @Transactional
    public void resetPassword(String rawResetToken, String newPassword) {
        PasswordResetToken token = loadResetToken(rawResetToken);
        if (!token.isVerified()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Please verify the code before resetting your password");
        }
        if (Instant.now().isAfter(token.getCreatedAt().plus(passwordResetTtl))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "This reset session has expired. Please start again.");
        }

        User user = token.getUser();
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        token.setUsedAt(Instant.now());
        passwordResetTokenRepository.save(token);

        refreshTokenRepository.revokeAllForUser(user, Instant.now());
    }

    private PasswordResetToken loadResetToken(String rawToken) {
        PasswordResetToken token = passwordResetTokenRepository.findByTokenHash(Tokens.hash(rawToken))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid reset request"));
        if (token.isUsed()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This reset link was already used");
        }
        return token;
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
