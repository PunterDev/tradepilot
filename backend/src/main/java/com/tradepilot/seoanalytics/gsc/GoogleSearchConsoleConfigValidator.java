package com.tradepilot.seoanalytics.gsc;

import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Configuration
public class GoogleSearchConsoleConfigValidator {
    @Bean
    ApplicationRunner googleSearchConsoleConfigurationValidator(GoogleSearchConsoleProperties properties) {
        return args -> {
            if (!properties.isEnabled()) return;
            if (!StringUtils.hasText(properties.getProperty())) {
                throw new IllegalStateException("GSC_PROPERTY is required when GSC_ENABLED=true.");
            }
            if (!properties.hasCredentials()) {
                throw new IllegalStateException("GSC_SERVICE_ACCOUNT_JSON_BASE64 or GSC_SERVICE_ACCOUNT_JSON is required when GSC_ENABLED=true.");
            }
        };
    }
}
