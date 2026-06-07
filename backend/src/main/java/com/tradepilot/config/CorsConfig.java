package com.tradepilot.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.util.StringUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;
import java.util.List;

@Configuration
@lombok.extern.slf4j.Slf4j
public class CorsConfig {
    @Bean
    public CorsFilter corsFilter(
            @Value("${app.cors.allowed-origins:http://localhost:5173,http://localhost:5174,http://localhost:3000}") String allowedOrigins,
            Environment environment
    ) {
        CorsConfiguration config = new CorsConfiguration();
        List<String> origins = parseAllowedOrigins(allowedOrigins);
        if (origins.contains("*")) {
            throw new IllegalStateException("CORS wildcard origin cannot be used when credentials are allowed");
        }
        if (environment.acceptsProfiles(Profiles.of("prod", "production"))
                && origins.stream().anyMatch(origin -> origin.contains("localhost") || origin.contains("127.0.0.1"))) {
            log.warn("Production CORS includes localhost origins. Configure APP_ALLOWED_ORIGINS with deployed frontend origins only.");
        }
        config.setAllowedOrigins(origins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }

    private List<String> parseAllowedOrigins(String allowedOrigins) {
        return Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .distinct()
                .toList();
    }
}
