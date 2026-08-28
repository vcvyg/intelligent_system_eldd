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
 * Spring Security configuration.
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
                .cors(cors -> {})
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        // Login shell and static assets are public. Business APIs enforce JWT.
                        .requestMatchers("/", "/*.html", "/admin-*.html").permitAll()
                        .requestMatchers("/css/**", "/js/**", "/images/**").permitAll()

                        // Chat attachment rendering/downloading remains public for the current
                        // browser implementation, but the serving controllers constrain every
                        // resolved path to the configured upload root. Upload APIs themselves
                        // are NOT public.
                        .requestMatchers("/uploads/**").permitAll()
                        .requestMatchers("/download", "/download/**").permitAll()

                        // Authentication endpoints must remain public.
                        .requestMatchers("/api/auth/**").permitAll()

                        // SockJS handshake authenticates the token supplied by the chat client.
                        .requestMatchers("/ws-chat/**").permitAll()

                        // Everything else, including upload/file-access/test APIs, requires JWT.
                        .anyRequest().authenticated()
                )
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                        .accessDeniedHandler(jwtAccessDeniedHandler)
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .headers(headers -> headers.frameOptions().sameOrigin());

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
