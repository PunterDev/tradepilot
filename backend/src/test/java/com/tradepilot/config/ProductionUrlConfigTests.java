package com.tradepilot.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductionUrlConfigTests {

    @Test
    void productionRejectsLocalhostFrontendUrl() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("production");

        assertThatThrownBy(() -> ProductionUrlConfig.validateFrontendUrl("http://localhost:5173", environment))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Production APP_FRONTEND_URL cannot use localhost");
    }

    @Test
    void productionAcceptsDeployedFrontendUrl() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("production");

        assertThatCode(() -> ProductionUrlConfig.validateFrontendUrl("https://tradepilot.app", environment))
                .doesNotThrowAnyException();
    }

    @Test
    void developmentAllowsLocalFrontendUrl() {
        assertThatCode(() -> ProductionUrlConfig.validateFrontendUrl("http://localhost:5173", new MockEnvironment()))
                .doesNotThrowAnyException();
    }
}
