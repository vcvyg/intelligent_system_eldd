package org.example.persion.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.persion.security.JwtUtil;
import org.example.persion.service.ChatGroupAccessService;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.lang.NonNull;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.Map;

/**
 * WebSocket authentication and authorization for elderly-centered chat groups.
 */
@Slf4j
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private static final String GROUP_APP_PREFIX = "/app/chat/group/";
    private static final String GROUP_TOPIC_PREFIX = "/topic/group/";

    private final JwtUtil jwtUtil;
    private final ChatGroupAccessService chatGroupAccessService;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        ThreadPoolTaskScheduler heartBeatScheduler = new ThreadPoolTaskScheduler();
        heartBeatScheduler.setPoolSize(1);
        heartBeatScheduler.setThreadNamePrefix("wss-heartbeat-");
        heartBeatScheduler.initialize();

        config.enableSimpleBroker("/topic", "/queue")
                .setHeartbeatValue(new long[]{10000, 10000})
                .setTaskScheduler(heartBeatScheduler);
        config.setApplicationDestinationPrefixes("/app");
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws-chat")
                .setAllowedOriginPatterns("*")
                .addInterceptors(new HandshakeInterceptor() {
                    @Override
                    public boolean beforeHandshake(@NonNull ServerHttpRequest request,
                                                   @NonNull ServerHttpResponse response,
                                                   @NonNull WebSocketHandler wsHandler,
                                                   @NonNull Map<String, Object> attributes) {
                        String token = queryParameter(request.getURI().getRawQuery(), "token");
                        if (token == null || token.isBlank()) {
                            response.setStatusCode(HttpStatus.UNAUTHORIZED);
                            return false;
                        }

                        try {
                            if (!jwtUtil.validateToken(token)) {
                                response.setStatusCode(HttpStatus.UNAUTHORIZED);
                                return false;
                            }

                            Long userId = jwtUtil.getUserIdFromToken(token);
                            String username = jwtUtil.getUsernameFromToken(token);
                            String role = jwtUtil.getRoleFromToken(token);
                            if (userId == null || username == null || username.isBlank()) {
                                response.setStatusCode(HttpStatus.UNAUTHORIZED);
                                return false;
                            }

                            attributes.put("userId", userId);
                            attributes.put("username", username);
                            attributes.put("role", role);
                            log.debug("WebSocket handshake authenticated userId={}", userId);
                            return true;
                        } catch (Exception exception) {
                            // Never log the URI or token. Query-string JWTs can otherwise leak
                            // through application logs.
                            log.warn("WebSocket token validation failed: {}",
                                    exception.getClass().getSimpleName());
                            response.setStatusCode(HttpStatus.UNAUTHORIZED);
                            return false;
                        }
                    }

                    @Override
                    public void afterHandshake(@NonNull ServerHttpRequest request,
                                               @NonNull ServerHttpResponse response,
                                               @NonNull WebSocketHandler wsHandler,
                                               Exception exception) {
                        // No token-bearing URI logging here.
                    }
                })
                .setHandshakeHandler(new DefaultHandshakeHandler() {
                    @Override
                    protected Principal determineUser(@NonNull ServerHttpRequest request,
                                                      @NonNull WebSocketHandler wsHandler,
                                                      @NonNull Map<String, Object> attributes) {
                        Object username = attributes.get("username");
                        if (username instanceof String value && !value.isBlank()) {
                            // The Principal name intentionally matches sys_user.username because
                            // SimpMessagingTemplate.convertAndSendToUser uses that name to resolve
                            // /user/queue destinations.
                            return () -> value;
                        }
                        return null;
                    }
                })
                .withSockJS();
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
                StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
                StompCommand command = accessor.getCommand();
                if (command == null || command == StompCommand.CONNECT || command == StompCommand.DISCONNECT) {
                    return message;
                }

                Long userId = sessionUserId(accessor);
                if (userId == null) {
                    throw new AccessDeniedException("WebSocket session is not authenticated");
                }

                String destination = accessor.getDestination();
                if (command == StompCommand.SUBSCRIBE && destination != null) {
                    authorizeSubscription(userId, destination);
                }
                if (command == StompCommand.SEND && destination != null) {
                    authorizeSend(userId, destination);
                }
                return message;
            }
        });
    }

    private void authorizeSubscription(Long userId, String destination) {
        if (destination.startsWith(GROUP_TOPIC_PREFIX)) {
            Long groupId = parseTrailingId(destination, GROUP_TOPIC_PREFIX);
            requireGroupAccess(userId, groupId);
        }
    }

    private void authorizeSend(Long userId, String destination) {
        if (destination.startsWith("/topic/") || destination.startsWith("/queue/")
                || destination.startsWith("/user/")) {
            throw new AccessDeniedException("Direct broker publishing is not allowed");
        }

        if (destination.startsWith(GROUP_APP_PREFIX)) {
            Long groupId = parseTrailingId(destination, GROUP_APP_PREFIX);
            requireGroupAccess(userId, groupId);
        }
    }

    private void requireGroupAccess(Long userId, Long groupId) {
        if (groupId == null || !chatGroupAccessService.canAccess(userId, groupId)) {
            throw new AccessDeniedException("No access to this chat group");
        }
    }

    private Long sessionUserId(StompHeaderAccessor accessor) {
        Map<String, Object> attributes = accessor.getSessionAttributes();
        if (attributes == null) {
            return null;
        }
        Object userId = attributes.get("userId");
        return userId instanceof Long ? (Long) userId : null;
    }

    private Long parseTrailingId(String destination, String prefix) {
        String raw = destination.substring(prefix.length());
        int slash = raw.indexOf('/');
        if (slash >= 0) {
            raw = raw.substring(0, slash);
        }
        try {
            return Long.valueOf(raw);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private String queryParameter(String rawQuery, String name) {
        if (rawQuery == null || rawQuery.isBlank()) {
            return null;
        }
        for (String pair : rawQuery.split("&")) {
            String[] parts = pair.split("=", 2);
            String key = URLDecoder.decode(parts[0], StandardCharsets.UTF_8);
            if (name.equals(key)) {
                return parts.length == 2
                        ? URLDecoder.decode(parts[1], StandardCharsets.UTF_8)
                        : "";
            }
        }
        return null;
    }
}
