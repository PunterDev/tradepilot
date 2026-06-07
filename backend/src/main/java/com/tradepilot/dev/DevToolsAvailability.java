package com.tradepilot.dev;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Slf4j
@Component
@RequiredArgsConstructor
public class DevToolsAvailability {
    private final Environment environment;

    @Value("${app.dev-tools-enabled:false}")
    private boolean devToolsEnabled;

    public boolean enabled() {
        return devToolsEnabled || Arrays.asList(environment.getActiveProfiles()).contains("dev");
    }

    @PostConstruct
    void logMode() {
        log.info("DEV TOOLS ENABLED: {}", enabled());
    }
}
