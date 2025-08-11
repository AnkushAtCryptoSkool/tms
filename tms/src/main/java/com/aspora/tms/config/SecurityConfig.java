package com.aspora.tms.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(authz -> authz
                .requestMatchers("/h2-console/**").permitAll()
                .requestMatchers("/actuator/**").permitAll()
                .requestMatchers("/api/**").authenticated()
                .anyRequest().authenticated()
            )
            .addFilterBefore(rateLimitFilter(), UsernamePasswordAuthenticationFilter.class)
            .headers(headers -> headers.frameOptions().disable()); // For H2 console

        return http.build();
    }

    @Bean
    public OncePerRequestFilter rateLimitFilter() {
        return new OncePerRequestFilter() {
            private final ConcurrentHashMap<String, AtomicInteger> requestCounts = new ConcurrentHashMap<>();
            private final ConcurrentHashMap<String, Long> lastResetTime = new ConcurrentHashMap<>();
            private static final int MAX_REQUESTS_PER_MINUTE = 100;
            private static final long RESET_INTERVAL = 60000; // 1 minute

            @Override
            protected void doFilterInternal(HttpServletRequest request, 
                                        HttpServletResponse response, 
                                        FilterChain filterChain) throws ServletException, IOException {
                
                String clientIp = getClientIpAddress(request);
                long currentTime = System.currentTimeMillis();
                
                // Reset counter if interval has passed
                lastResetTime.computeIfPresent(clientIp, (ip, lastReset) -> {
                    if (currentTime - lastReset > RESET_INTERVAL) {
                        requestCounts.put(ip, new AtomicInteger(0));
                        return currentTime;
                    }
                    return lastReset;
                });
                
                // Check rate limit
                AtomicInteger count = requestCounts.computeIfAbsent(clientIp, k -> {
                    lastResetTime.put(k, currentTime);
                    return new AtomicInteger(0);
                });
                
                if (count.incrementAndGet() > MAX_REQUESTS_PER_MINUTE) {
                    response.setStatus(429); // Too Many Requests
                    response.getWriter().write("Rate limit exceeded. Please try again later.");
                    return;
                }
                
                filterChain.doFilter(request, response);
            }
            
            private String getClientIpAddress(HttpServletRequest request) {
                String xForwardedFor = request.getHeader("X-Forwarded-For");
                if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
                    return xForwardedFor.split(",")[0].trim();
                }
                return request.getRemoteAddr();
            }
        };
    }
}
