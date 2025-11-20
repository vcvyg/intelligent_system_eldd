package org.example.persion.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.lang.NonNull;

import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Value("${jwt.secret}")
    private String secretKey;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic");
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws-chat")
                .setAllowedOriginPatterns("*")
                .addInterceptors(new HandshakeInterceptor() {
                    @Override
                    public boolean beforeHandshake(@NonNull ServerHttpRequest request, @NonNull ServerHttpResponse response, @NonNull WebSocketHandler wsHandler, @NonNull Map<String, Object> attributes) {
                        String authHeader = request.getHeaders().getFirst("Authorization");
                        System.out.println("Authorization Header: " + authHeader);

                        if (authHeader != null && authHeader.startsWith("Bearer ")) {
                            String token = authHeader.substring(7);
                            System.out.println("Extracted Token: " + token);

                            if (validateToken(token)) {
                                System.out.println("Token validation successful.");
                                return true;
                            } else {
                                System.out.println("Token validation failed.");
                            }
                        } else {
                            System.out.println("Authorization header is missing or invalid.");
                        }

                        response.setStatusCode(HttpStatus.UNAUTHORIZED);
                        return false;
                    }

                    @Override
                    public void afterHandshake(@NonNull ServerHttpRequest request, @NonNull ServerHttpResponse response, @NonNull WebSocketHandler wsHandler, Exception exception) {
                        // 握手后逻辑（如果需要）
                    }
                })
                .withSockJS();
    }

    private boolean validateToken(String token) {
        try {
            byte[] keyBytes = secretKey.getBytes(StandardCharsets.UTF_8);

            System.out.println("Loaded secretKey: " + secretKey);

            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(Keys.hmacShaKeyFor(keyBytes))
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            if (claims.getExpiration().before(new Date())) {
                System.out.println("Token is expired.");
                return false;
            }

            System.out.println("Token claims: " + claims);
            return true;
        } catch (Exception e) {
            System.out.println("Token validation exception: " + e.getMessage());
            return false;
        }
    }

    @PostConstruct
    public void init() {
        System.out.println("Application started. Loaded jwt.secret: " + secretKey);
    }
}
