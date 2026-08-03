package com.discoverdev.nms.chat;

import com.discoverdev.nms.auth.AppUserDetails;
import com.discoverdev.nms.auth.AppUserDetailsService;
import com.discoverdev.nms.auth.JwtService;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.server.HandshakeInterceptor;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private static final Logger log = LoggerFactory.getLogger(WebSocketConfig.class);
    private static final String USER_ATTR = "appUserDetails";
    private static final String THREAD_TOPIC_PREFIX = "/topic/threads/";
    private static final String USER_QUEUE_PREFIX = "/user/queue/";

    private final JwtService jwtService;
    private final AppUserDetailsService userDetailsService;
    private final ChatParticipantRepository participantRepository;

    public WebSocketConfig(
        JwtService jwtService,
        AppUserDetailsService userDetailsService,
        ChatParticipantRepository participantRepository
    ) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.participantRepository = participantRepository;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // /topic — one destination per thread, for an open conversation.
        // /queue — per-user destinations behind the /user prefix, so the chat list
        //          can update without subscribing to every thread it lists.
        registry.enableSimpleBroker("/topic", "/queue");
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws-chat")
            .setAllowedOriginPatterns("*")
            .addInterceptors(new JwtHandshakeInterceptor());
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new SubscriptionAccessInterceptor());
    }

    private class JwtHandshakeInterceptor implements HandshakeInterceptor {
        @Override
        public boolean beforeHandshake(
            ServerHttpRequest request, ServerHttpResponse response,
            WebSocketHandler wsHandler, Map<String, Object> attributes
        ) {
            if (!(request instanceof ServletServerHttpRequest servletReq)) return false;
            HttpServletRequest http = servletReq.getServletRequest();
            String token = http.getParameter("access_token");
            if (token == null || token.isBlank()) {
                log.debug("WS handshake rejected — missing access_token");
                return false;
            }
            try {
                JwtService.ParsedToken parsed = jwtService.parse(token);
                AppUserDetails details = (AppUserDetails)
                    userDetailsService.loadUserByUsername(parsed.email());
                if (!details.isEnabled()) {
                    log.debug("WS handshake rejected — user disabled");
                    return false;
                }
                attributes.put(USER_ATTR, details);
                return true;
            } catch (JwtException | IllegalArgumentException e) {
                log.debug("WS handshake rejected — invalid JWT: {}", e.getMessage());
                return false;
            }
        }

        @Override
        public void afterHandshake(
            ServerHttpRequest request, ServerHttpResponse response,
            WebSocketHandler wsHandler, Exception exception
        ) {}
    }

    private class SubscriptionAccessInterceptor implements ChannelInterceptor {
        @Override
        public Message<?> preSend(Message<?> message, MessageChannel channel) {
            StompHeaderAccessor accessor = MessageHeaderAccessor
                .getAccessor(message, StompHeaderAccessor.class);
            if (accessor == null) return message;

            if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                AppUserDetails details = (AppUserDetails) accessor.getSessionAttributes()
                    .get(USER_ATTR);
                if (details == null) return null;
                accessor.setUser(principalOf(details));
                return message;
            }

            if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
                Principal principal = accessor.getUser();
                if (!(principal instanceof AppUserPrincipal app)) return null;
                String dest = accessor.getDestination();
                if (dest == null) return null;

                // A /user/** destination is resolved by Spring against the
                // subscriber's own session, so it can only ever deliver to the
                // person who asked. There is nothing further to authorize.
                if (dest.startsWith(USER_QUEUE_PREFIX)) {
                    return message;
                }
                if (!dest.startsWith(THREAD_TOPIC_PREFIX)) {
                    return null;
                }
                String threadIdStr = dest.substring(THREAD_TOPIC_PREFIX.length());
                try {
                    UUID threadId = UUID.fromString(threadIdStr);
                    boolean ok = participantRepository
                        .existsByThreadIdAndUserIdAndLeftAtIsNull(threadId, app.userId());
                    if (!ok) {
                        log.debug("WS SUBSCRIBE denied — user {} not in thread {}",
                            app.userId(), threadId);
                        return null;
                    }
                } catch (IllegalArgumentException e) {
                    return null;
                }
            }
            return message;
        }
    }

    private static Principal principalOf(AppUserDetails details) {
        return new AppUserPrincipal(
            details.getUser().getId(),
            details.getUsername(),
            List.of(details.getUser().getRole().name())
        );
    }

    public record AppUserPrincipal(UUID userId, String email, List<String> roles)
        implements Principal {
        @Override
        public String getName() { return email; }
    }
}
