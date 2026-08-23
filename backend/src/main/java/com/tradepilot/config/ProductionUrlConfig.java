package com.tradepilot.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.util.StringUtils;

@Configuration
public class ProductionUrlConfig {

    @Bean
    ApplicationRunner productionUrlValidator(
            @Value("${app.frontend-url:http://localhost:5173}") String frontendUrl,
            Environment environment
    ) {
        return args -> validateFrontendUrl(frontendUrl, environment);
    }

    static void validateFrontendUrl(String frontendUrl, Environment environment) {
        if (!environment.acceptsProfiles(Profiles.of("prod", "production"))) {
            return;
        }
        if (!StringUtils.hasText(frontendUrl)) {
            throw new IllegalStateException("Production APP_FRONTEND_URL is required.");
        }
        String normalized = frontendUrl.trim().toLowerCase();
        if (normalized.contains("localhost") || normalized.contains("127.0.0.1")) {
            throw new IllegalStateException("Production APP_FRONTEND_URL cannot use localhost. Configure APP_FRONTEND_URL with the deployed frontend URL.");
        }
    }
}
