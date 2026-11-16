package com.discoverdev.nms.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Per-client limits on the public auth endpoints (login, password reset, invite
 * acceptance) so passwords and reset codes cannot be guessed at speed. Counts
 * are kept in memory per instance, in fixed windows.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AuthRateLimitFilter extends OncePerRequestFilter {

    private record Limit(int maxRequests, Duration window) {}

    private static final Map<String, Limit> LIMITS = Map.of(
        "/api/auth/login", new Limit(10, Duration.ofMinutes(1)),
        "/api/auth/forgot-password", new Limit(5, Duration.ofMinutes(15)),
        "/api/auth/resend-reset-otp", new Limit(5, Duration.ofMinutes(15)),
        "/api/auth/verify-reset-otp", new Limit(10, Duration.ofMinutes(15)),
        "/api/auth/reset-password", new Limit(10, Duration.ofMinutes(15)),
        "/api/auth/accept-invite", new Limit(10, Duration.ofMinutes(15)),
        "/api/auth/invite/reactivation-request", new Limit(3, Duration.ofHours(1))
    );
    private static final int MAX_TRACKED_KEYS = 50_000;

    private static final class Window {
        final long startedAt;
        final AtomicInteger count = new AtomicInteger();

        Window(long startedAt) {
            this.startedAt = startedAt;
        }
    }

    private final Map<String, Window> windows = new ConcurrentHashMap<>();
    private final boolean enabled;

    public AuthRateLimitFilter(@Value("${app.rate-limit.enabled:true}") boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !enabled || !"POST".equals(request.getMethod()) || !LIMITS.containsKey(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
        throws ServletException, IOException {
        String path = request.getRequestURI();
        Limit limit = LIMITS.get(path);
        long now = System.currentTimeMillis();
        long windowMs = limit.window().toMillis();
        if (windows.size() > MAX_TRACKED_KEYS) {
            windows.values().removeIf(w -> now - w.startedAt >= windowMs);
        }
        Window window = windows.compute(path + "|" + clientIp(request),
            (key, current) -> current == null || now - current.startedAt >= windowMs ? new Window(now) : current);
        if (window.count.incrementAndGet() > limit.maxRequests()) {
            long retryAfterSeconds = Math.max(1, (window.startedAt + windowMs - now) / 1000);
            response.setStatus(429);
            response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));
            response.setContentType("application/json");
            response.getWriter().write("{\"timestamp\":\"" + Instant.now() + "\",\"status\":429,"
                + "\"error\":\"Too Many Requests\","
                + "\"message\":\"Too many attempts. Please wait a few minutes and try again.\","
                + "\"path\":\"" + path + "\"}");
            return;
        }
        chain.doFilter(request, response);
    }

    /**
     * Behind Render's proxy every request arrives from the proxy, so the client
     * is the last address the proxy appended to X-Forwarded-For.
     */
    private static String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            String[] hops = forwarded.split(",");
            return hops[hops.length - 1].trim();
        }
        return request.getRemoteAddr();
    }
}
