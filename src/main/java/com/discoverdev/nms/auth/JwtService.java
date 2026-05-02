package com.discoverdev.nms.auth;

import com.discoverdev.nms.config.JwtProperties;
import com.discoverdev.nms.user.Role;
import com.discoverdev.nms.user.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    private final SecretKey signingKey;
    private final Duration accessTokenTtl;

    public JwtService(JwtProperties props) {
        byte[] keyBytes = Base64.getDecoder().decode(props.secret());
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
        // Access tokens have no exp claim; revocation is handled by toggling User.enabled.
        this.accessTokenTtl = Duration.ofDays(36500);
    }

    public String issueAccessToken(User user) {
        return Jwts.builder()
            .subject(user.getId().toString())
            .claim("email", user.getEmail())
            .claim("role", user.getRole().name())
            .issuedAt(Date.from(Instant.now()))
            .signWith(signingKey, Jwts.SIG.HS256)
            .compact();
    }

    public ParsedToken parse(String jwt) {
        Claims claims = Jwts.parser()
            .verifyWith(signingKey)
            .build()
            .parseSignedClaims(jwt)
            .getPayload();
        return new ParsedToken(
            UUID.fromString(claims.getSubject()),
            claims.get("email", String.class),
            Role.valueOf(claims.get("role", String.class))
        );
    }

    public Duration getAccessTokenTtl() {
        return accessTokenTtl;
    }

    public record ParsedToken(UUID userId, String email, Role role) {}
}
