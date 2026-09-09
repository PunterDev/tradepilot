package com.tradepilot.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SecurityProductionConfigValidatorTests {

    @Test
    void productionRejectsDevAuthTokenEndpoint() {
        assertThatThrownBy(() -> SecurityProductionConfigValidator.validate(true, false, "database", "external-mfa", productionEnvironment()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("DEV_AUTH_ENABLED must be false in production");
    }

    @Test
    void productionRejectsDevTools() {
        assertThatThrownBy(() -> SecurityProductionConfigValidator.validate(false, true, "database", "external-mfa", productionEnvironment()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("DEV_TOOLS_ENABLED must be false in production");
    }

    @Test
    void productionRejectsInMemoryRateLimiting() {
        assertThatThrownBy(() -> SecurityProductionConfigValidator.validate(false, false, "memory", "external-mfa", productionEnvironment()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("RATE_LIMIT_STORE must be database in production");
    }

    @Test
    void productionRejectsMissingAdminAccessProtection() {
        assertThatThrownBy(() -> SecurityProductionConfigValidator.validate(false, false, "database", "none", productionEnvironment()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("APP_ADMIN_ACCESS_PROTECTION");
    }

    @Test
    void productionAcceptsRequiredSecurityGates() {
        assertThatCode(() -> SecurityProductionConfigValidator.validate(false, false, "database", "external-mfa", productionEnvironment()))
                .doesNotThrowAnyException();
    }

    @Test
    void nonProductionAllowsDevFeaturesForLocalTesting() {
        assertThatCode(() -> SecurityProductionConfigValidator.validate(true, true, "memory", "none", new MockEnvironment().withProperty("spring.profiles.active", "test")))
                .doesNotThrowAnyException();
    }

    private MockEnvironment productionEnvironment() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("production");
        return environment;
    }
}
