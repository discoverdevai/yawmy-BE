package com.discoverdev.nms.auth;

import com.discoverdev.nms.auth.dto.AcceptInviteRequest;
import com.discoverdev.nms.auth.dto.ChangePasswordRequest;
import com.discoverdev.nms.auth.dto.ForgotPasswordRequest;
import com.discoverdev.nms.auth.dto.InviteValidationResponse;
import com.discoverdev.nms.auth.dto.LoginRequest;
import com.discoverdev.nms.auth.dto.LogoutRequest;
import com.discoverdev.nms.auth.dto.MeResponse;
import com.discoverdev.nms.auth.dto.ReactivationRequest;
import com.discoverdev.nms.auth.dto.RefreshRequest;
import com.discoverdev.nms.auth.dto.ResetPasswordRequest;
import com.discoverdev.nms.auth.dto.TokenResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest req) {
        return authService.login(req.email(), req.password());
    }

    @PostMapping("/refresh")
    public TokenResponse refresh(@Valid @RequestBody RefreshRequest req) {
        return authService.refresh(req.refreshToken());
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
        @RequestBody(required = false) LogoutRequest req,
        @AuthenticationPrincipal AppUserDetails principal
    ) {
        authService.logout(principal.getUser(), req != null ? req.refreshToken() : null);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/logout-all")
    public ResponseEntity<Void> logoutAll(@AuthenticationPrincipal AppUserDetails principal) {
        authService.logoutAll(principal.getUser());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/invite")
    public InviteValidationResponse validateInvite(@RequestParam String token) {
        return authService.validateInvite(token);
    }

    @PostMapping("/accept-invite")
    public TokenResponse acceptInvite(@Valid @RequestBody AcceptInviteRequest req) {
        return authService.acceptInvite(req.token(), req.password());
    }

    @PostMapping("/invite/reactivation-request")
    public ResponseEntity<Void> requestReactivation(@Valid @RequestBody ReactivationRequest req) {
        authService.requestReactivation(req.email(), req.message());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/change-password")
    public ResponseEntity<Void> changePassword(
        @Valid @RequestBody ChangePasswordRequest req,
        @AuthenticationPrincipal AppUserDetails principal
    ) {
        authService.changePassword(principal.getUser(), req.currentPassword(), req.newPassword());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest req) {
        authService.forgotPassword(req.email());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest req) {
        authService.resetPassword(req.token(), req.newPassword());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public MeResponse me(@AuthenticationPrincipal AppUserDetails principal) {
        return MeResponse.of(principal.getUser());
    }
}
