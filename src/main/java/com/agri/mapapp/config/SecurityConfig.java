// src/main/java/com/agri/mapapp/config/SecurityConfig.java
package com.agri.mapapp.config;

import com.agri.mapapp.auth.JwtAuthFilter;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // JWT + SPA
                .csrf(csrf -> csrf.disable())
                .cors(cors -> { /* CORS konfiguratsiyasi CorsConfig ichidagi bean’dan olinadi */ })
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .authorizeHttpRequests(reg -> reg
                        // --- CORS preflight ---
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // --- PUBLIC endpoints ---
                        .requestMatchers("/auth/login", "/auth/refresh", "/auth/logout").permitAll()
                        .requestMatchers("/captcha/**").permitAll()

                        // (agar /api prefiksi bilan ham chaqirilsa)
                        .requestMatchers("/api/auth/login", "/api/auth/refresh", "/api/auth/logout").permitAll()
                        .requestMatchers("/api/captcha/**").permitAll()

                        // Swagger (ixtiyoriy)
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/api-docs/**", "/swagger-ui.html").permitAll()

                        // Health
                        .requestMatchers(HttpMethod.GET, "/actuator/health").permitAll()

                        // Statik fayllar
                        .requestMatchers(HttpMethod.GET, "/uploads/**").permitAll()

                        // --- Qolganlar token talab qiladi ---
                        .anyRequest().authenticated()
                )

                // 401/403 javoblari
                .exceptionHandling(e -> e
                        .authenticationEntryPoint((req, res, ex) -> res.sendError(HttpServletResponse.SC_UNAUTHORIZED))
                        .accessDeniedHandler((req, res, ex) -> res.sendError(HttpServletResponse.SC_FORBIDDEN))
                )

                // HSTS
                .headers(h -> h.httpStrictTransportSecurity(hsts -> hsts
                        .includeSubDomains(true)
                        .preload(true)
                        .maxAgeInSeconds(31536000)
                ))

                // JWT filter
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authManager(AuthenticationConfiguration cfg) throws Exception {
        return cfg.getAuthenticationManager();
    }
}
