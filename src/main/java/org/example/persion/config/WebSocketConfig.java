package org.example.persion.config;

import org.example.persion.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.lang.NonNull;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Autowired
    private JwtUtil jwtUtil;

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
        // 注册端点，同时支持原生WebSocket和SockJS
        // withSockJS()会自动处理SockJS协议协商，同时也支持原生WebSocket连接
        registry.addEndpoint("/ws-chat")
                .setAllowedOriginPatterns("*")
                .addInterceptors(new HandshakeInterceptor() {
                    @Override
                    public boolean beforeHandshake(@NonNull ServerHttpRequest request, @NonNull ServerHttpResponse response, @NonNull WebSocketHandler wsHandler, @NonNull Map<String, Object> attributes) throws Exception {
                        System.out.println("WebSocket handshake request: " + request.getURI());

                        String query = request.getURI().getQuery();
                        if (query != null && query.contains("token=")) {
                            String token = query.substring(query.indexOf("token=") + 6);
                            try {
                                token = java.net.URLDecoder.decode(token, StandardCharsets.UTF_8.name());
                                if (jwtUtil.validateToken(token)) {
                                    Long userId = jwtUtil.getUserIdFromToken(token);
                                    String username = jwtUtil.getUsernameFromToken(token);
                                    String role = jwtUtil.getRoleFromToken(token);

                                    // 验证通过，将用户信息存入WebSocket session
                                    attributes.put("userId", userId);
                                    attributes.put("username", username);
                                    attributes.put("role", role);
                                    
                                    System.out.println("WebSocket handshake successful for user: " + username + " with role: " + role);
                                    return true;
                                }
                            } catch (Exception e) {
                                System.err.println("WebSocket handshake failed due to token validation error: " + e.getMessage());
                            }
                        }

                        // 如果没有token或token无效，拒绝连接
                        System.err.println("WebSocket handshake failed: Missing or invalid token.");
                        response.setStatusCode(HttpStatus.UNAUTHORIZED);
                        return false;
                    }

                    @Override
                    public void afterHandshake(@NonNull ServerHttpRequest request, @NonNull ServerHttpResponse response, @NonNull WebSocketHandler wsHandler, Exception exception) {
                        System.out.println("WebSocket handshake completed");
                    }
                })
                .withSockJS();
    }
}