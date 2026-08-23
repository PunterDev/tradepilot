package com.tradepilot.ai.memory.service;

import com.tradepilot.ai.memory.config.TradingMemoryProperties;
import com.tradepilot.ai.memory.vector.TradingMemoryVectorClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class TradingMemoryStartupValidator {
    private static final Logger log = LoggerFactory.getLogger(TradingMemoryStartupValidator.class);
    private final TradingMemoryProperties properties;
    private final TradingMemoryVectorClient vectorClient;

    public TradingMemoryStartupValidator(TradingMemoryProperties properties, TradingMemoryVectorClient vectorClient) {
        this.properties = properties;
        this.vectorClient = vectorClient;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void validateOnStartup() {
        if (!properties.isEnabled() || !properties.isQdrantValidateOnStartup()) {
            return;
        }
        try {
            vectorClient.validateCollection();
        } catch (RuntimeException ex) {
            log.warn("Trading Memory startup validation failed without blocking application startup. reason={}", ex.getClass().getSimpleName());
        }
    }
}
