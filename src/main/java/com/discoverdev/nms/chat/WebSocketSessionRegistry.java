package com.discoverdev.nms.chat;

import com.discoverdev.nms.auth.AppUserDetails;
import com.discoverdev.nms.user.AccessTokensInvalidatedEvent;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.WebSocketHandlerDecorator;
import org.springframework.web.socket.handler.WebSocketHandlerDecoratorFactory;

/** Tracks local WebSocket connections so credential changes can close them. */
@Component
public class WebSocketSessionRegistry implements WebSocketHandlerDecoratorFactory {

    private static final Logger log = LoggerFactory.getLogger(WebSocketSessionRegistry.class);
    private static final CloseStatus ACCESS_REVOKED = new CloseStatus(1008, "Access revoked");

    private final ConcurrentMap<UUID, ConcurrentMap<String, WebSocketSession>> sessionsByUser =
        new ConcurrentHashMap<>();

    @Override
    public WebSocketHandler decorate(WebSocketHandler handler) {
        return new WebSocketHandlerDecorator(handler) {
            @Override
            public void afterConnectionEstablished(WebSocketSession session) throws Exception {
                super.afterConnectionEstablished(session);
                Object principal = session.getAttributes().get(WebSocketConfig.USER_ATTR);
                if (principal instanceof AppUserDetails details) {
                    UUID userId = details.getUser().getId();
                    sessionsByUser.computeIfAbsent(userId, ignored -> new ConcurrentHashMap<>())
                        .put(session.getId(), session);
                }
            }

            @Override
            public void afterConnectionClosed(WebSocketSession session, CloseStatus status)
                throws Exception {
                unregister(session);
                super.afterConnectionClosed(session, status);
            }
        };
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void closeSessionsForInvalidatedUser(AccessTokensInvalidatedEvent event) {
        ConcurrentMap<String, WebSocketSession> sessions = sessionsByUser.remove(event.userId());
        if (sessions == null) {
            return;
        }
        sessions.values().forEach(session -> {
            if (session.isOpen()) {
                try {
                    session.close(ACCESS_REVOKED);
                } catch (IOException exception) {
                    log.debug("Could not close revoked WebSocket session {}: {}",
                        session.getId(), exception.getMessage());
                }
            }
        });
    }

    private void unregister(WebSocketSession session) {
        Object principal = session.getAttributes().get(WebSocketConfig.USER_ATTR);
        if (!(principal instanceof AppUserDetails details)) {
            return;
        }
        UUID userId = details.getUser().getId();
        sessionsByUser.computeIfPresent(userId, (ignored, sessions) -> {
            sessions.remove(session.getId());
            return sessions.isEmpty() ? null : sessions;
        });
    }
}
