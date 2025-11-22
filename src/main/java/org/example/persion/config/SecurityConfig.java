package org.example.persion.config;

import org.example.persion.security.JwtAccessDeniedHandler;
import org.example.persion.security.JwtAuthenticationEntryPoint;
import org.example.persion.security.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security配置
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Autowired
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @Autowired
    private JwtAccessDeniedHandler jwtAccessDeniedHandler;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 启用CORS支持(必须在其他配置之前)
                .cors(cors -> {})

                // 禁用CSRF(使用JWT不需要CSRF保护)
                .csrf(AbstractHttpConfigurer::disable)

                // 配置请求授权 - 文件访问完全开放
                .authorizeHttpRequests(auth -> auth
                        // 根路径和静态资源(HTML、CSS、JS等)
                        .requestMatchers("/", "/*.html", "/admin-*.html").permitAll()
                        .requestMatchers("/css/**", "/js/**", "/images/**").permitAll()
                        
                        // 文件访问 - 完全开放，无需任何认证
                        .requestMatchers("/uploads/**").permitAll()
                        .requestMatchers("/api/files/**").permitAll()
                        .requestMatchers("/api/upload/**").permitAll()
                        .requestMatchers("/upload-**").permitAll()
                        .requestMatchers("/download").permitAll()
                        .requestMatchers("/download/**").permitAll()
                        .requestMatchers("/file-access/**").permitAll()
                        
                        // 公开接口(登录、注册等)
                        .requestMatchers("/api/auth/**").permitAll()
                        
                        // WebSocket端点
                        .requestMatchers("/ws-chat/**").permitAll()
                        
                        // 测试接口临时开放
                        .requestMatchers("/api/*/test").permitAll()
                        .requestMatchers("/test-**").permitAll()
                        
                        // 其他接口需要认证
                        .anyRequest().authenticated()
                )

                // 无状态session(使用JWT)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // 异常处理
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                        .accessDeniedHandler(jwtAccessDeniedHandler)
                )

                // 添加JWT过滤器
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                
                // 配置X-Frame-Options，允许同源嵌套
                .headers(headers -> headers
                        .frameOptions().sameOrigin()
                );

        return http.build();
    }

    /**
     * 密码加密器
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
