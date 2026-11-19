package com.discoverdev.nms.push;

import com.discoverdev.nms.auth.AppUserDetails;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * The apps register their device here after sign-in (and remove it on sign-out) so chat
 * messages can arrive as push notifications with sound (Jira YC-35, YC-60).
 */
@RestController
@RequestMapping("/api/me/push-tokens")
public class PushTokenController {

    public record RegisterRequest(@NotBlank @Size(max = 255) String token, @Size(max = 16) String platform) {}

    private final PushTokenRepository tokens;

    public PushTokenController(PushTokenRepository tokens) {
        this.tokens = tokens;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void register(@Valid @RequestBody RegisterRequest req, @AuthenticationPrincipal AppUserDetails principal) {
        // A device belongs to whoever signed in on it last.
        PushToken t = tokens.findByToken(req.token().trim())
            .orElseGet(() -> PushToken.builder().token(req.token().trim()).build());
        t.setUser(principal.getUser());
        t.setPlatform(req.platform());
        tokens.save(t);
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void unregister(@RequestParam("token") String token, @AuthenticationPrincipal AppUserDetails principal) {
        tokens.findByToken(token.trim())
            .filter(t -> t.getUser().getId().equals(principal.getUser().getId()))
            .ifPresent(tokens::delete);
    }
}
