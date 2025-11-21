package org.example.persion.config;

import org.example.persion.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
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

import java.nio.charset.StandardCharsets;
import java.util.Map;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic", "/queue");
        config.setApplicationDestinationPrefixes("/app");
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws-chat")
                .setAllowedOriginPatterns("*")
                .addInterceptors(new HandshakeInterceptor() {
                    @Override
                    public boolean beforeHandshake(@NonNull ServerHttpRequest request, @NonNull ServerHttpResponse response, @NonNull WebSocketHandler wsHandler, @NonNull Map<String, Object> attributes) {
                        System.out.println("WebSocket handshake request: " + request.getURI());
                        
                        // 暂时跳过token验证，先测试连接
                        // TODO: 重新启用token验证
                        
                        // 设置一个测试用户ID
                        attributes.put("userId", 7L);
                        attributes.put("username", "3745");
                        attributes.put("role", "MEDICAL");
                        
                        System.out.println("WebSocket handshake successful (token validation disabled for testing)");
                        return true;
                    }

                    @Override
                    public void afterHandshake(@NonNull ServerHttpRequest request, @NonNull ServerHttpResponse response, @NonNull WebSocketHandler wsHandler, Exception exception) {
                        System.out.println("WebSocket handshake completed");
                    }
                })
                .withSockJS();
    }


}
