package com.tradepilot.seoanalytics.gsc;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GoogleSearchConsoleConfigValidatorTests {
    @Test
    void enabledConfigurationRequiresPropertyAndCredentials() {
        GoogleSearchConsoleProperties properties = new GoogleSearchConsoleProperties();
        properties.setEnabled(true);
        var runner = new GoogleSearchConsoleConfigValidator().googleSearchConsoleConfigurationValidator(properties);

        assertThatThrownBy(() -> runner.run(null)).hasMessageContaining("GSC_PROPERTY");

        properties.setProperty("sc-domain:tradepilotcoach.com");
        assertThatThrownBy(() -> runner.run(null)).hasMessageContaining("GSC_SERVICE_ACCOUNT_JSON_BASE64");
    }
}
