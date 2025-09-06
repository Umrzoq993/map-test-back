package com.agri.mapapp.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Configuration
@ConditionalOnProperty(name = "app.cors.enabled", havingValue = "true", matchIfMissing = true)
public class CorsConfig {

    @Value("${app.cors.allowed-origins:http://localhost:5173}")
    private String allowedOriginsCsv;

    @Value("${app.cors.allowed-methods:GET,POST,PATCH,DELETE,OPTIONS}")
    private String allowedMethodsCsv;

    @Value("${app.cors.allowed-headers:*}")
    private String allowedHeadersCsv;

    @Value("${app.cors.exposed-headers:Authorization}")
    private String exposedHeadersCsv;

    @Value("${app.cors.allow-credentials:false}")
    private boolean allowCredentials;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        List<String> origins = Arrays.stream(allowedOriginsCsv.split(","))
                .map(String::trim).filter(s -> !s.isEmpty())
                .collect(Collectors.toList());

        // Agar "null" yoki "*" kerak bo'lsa, AllowedOriginPatterns ishlatiladi.
        // Hozir biz aniq domenlar bilan ishlaymiz:
        origins.forEach(config::addAllowedOrigin);

        Arrays.stream(allowedMethodsCsv.split(","))
                .map(String::trim).filter(s -> !s.isEmpty())
                .forEach(config::addAllowedMethod);

        Arrays.stream(allowedHeadersCsv.split(","))
                .map(String::trim).filter(s -> !s.isEmpty())
                .forEach(config::addAllowedHeader);

        Arrays.stream(exposedHeadersCsv.split(","))
                .map(String::trim).filter(s -> !s.isEmpty())
                .forEach(config::addExposedHeader);

        config.setAllowCredentials(allowCredentials);
        config.setMaxAge(3600L); // 1 soat preflight cache

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
