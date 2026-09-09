package com.tradepilot.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;

@Configuration
public class SecurityProductionConfigValidator {

    @Bean
    ApplicationRunner securityProductionValidator(
            @Value("${app.dev-auth.enabled:false}") boolean devAuthEnabled,
            @Value("${app.dev-tools-enabled:false}") boolean devToolsEnabled,
            @Value("${app.rate-limit.store:memory}") String rateLimitStore,
            @Value("${app.admin-access.protection:none}") String adminAccessProtection,
            Environment environment
    ) {
        return args -> validate(devAuthEnabled, devToolsEnabled, rateLimitStore, adminAccessProtection, environment);
    }

    static void validate(boolean devAuthEnabled, boolean devToolsEnabled, String rateLimitStore, String adminAccessProtection, Environment environment) {
        if (!environment.acceptsProfiles(Profiles.of("prod", "production"))) {
            return;
        }
        if (devAuthEnabled) {
            throw new IllegalStateException("DEV_AUTH_ENABLED must be false in production.");
        }
        if (devToolsEnabled) {
            throw new IllegalStateException("DEV_TOOLS_ENABLED must be false in production.");
        }
        if (!"database".equalsIgnoreCase(nullSafe(rateLimitStore))) {
            throw new IllegalStateException("RATE_LIMIT_STORE must be database in production so abuse limits are shared across instances.");
        }
        String protection = nullSafe(adminAccessProtection);
        if (!"external-mfa".equalsIgnoreCase(protection) && !"privileged-access-gateway".equalsIgnoreCase(protection)) {
            throw new IllegalStateException("APP_ADMIN_ACCESS_PROTECTION must document an enforced production admin MFA or privileged access gateway control.");
        }
    }

    private static String nullSafe(String value) {
        return value == null ? "" : value.trim();
    }
}
