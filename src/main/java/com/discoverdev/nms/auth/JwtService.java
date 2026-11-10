package com.discoverdev.nms.auth;

import com.discoverdev.nms.config.JwtProperties;
import com.discoverdev.nms.user.Role;
import com.discoverdev.nms.user.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
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
        if (props.accessTokenTtlMinutes() <= 0) {
            throw new IllegalArgumentException("app.jwt.access-token-ttl-minutes must be positive");
        }
        this.accessTokenTtl = Duration.ofMinutes(props.accessTokenTtlMinutes());
    }

    public String issueAccessToken(User user) {
        Instant issuedAt = Instant.now();
        return Jwts.builder()
            .subject(user.getId().toString())
            .claim("email", user.getEmail())
            .claim("role", user.getRole().name())
            .claim("token_version", user.getAccessTokenVersion())
            .issuedAt(Date.from(issuedAt))
            .expiration(Date.from(issuedAt.plus(accessTokenTtl)))
            .signWith(signingKey, Jwts.SIG.HS256)
            .compact();
    }

    public ParsedToken parse(String jwt) {
        Claims claims = Jwts.parser()
            .verifyWith(signingKey)
            .build()
            .parseSignedClaims(jwt)
            .getPayload();
        if (claims.getExpiration() == null) {
            throw new MalformedJwtException("Access token is missing its expiration claim");
        }
        Integer tokenVersion = claims.get("token_version", Integer.class);
        if (tokenVersion == null || tokenVersion < 0) {
            throw new MalformedJwtException("Access token is missing a valid token version");
        }
        return new ParsedToken(
            UUID.fromString(claims.getSubject()),
            claims.get("email", String.class),
            Role.valueOf(claims.get("role", String.class)),
            tokenVersion
        );
    }

    public boolean isCurrentFor(ParsedToken token, User user) {
        return user.isEnabled()
            && token.userId().equals(user.getId())
            && token.tokenVersion() == user.getAccessTokenVersion();
    }

    public Duration getAccessTokenTtl() {
        return accessTokenTtl;
    }

    public record ParsedToken(UUID userId, String email, Role role, int tokenVersion) {}
}
