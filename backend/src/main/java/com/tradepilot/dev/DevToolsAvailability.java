package com.tradepilot.dev;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import java.util.Arrays;

@Component
public class DevToolsAvailability {
    @java.lang.SuppressWarnings("all")
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(DevToolsAvailability.class);
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

    @java.lang.SuppressWarnings("all")
    public DevToolsAvailability(final Environment environment) {
        this.environment = environment;
    }
}
