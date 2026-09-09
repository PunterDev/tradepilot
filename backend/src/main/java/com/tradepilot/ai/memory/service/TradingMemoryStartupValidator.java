package com.tradepilot.ai.memory.service;

import com.tradepilot.ai.memory.config.TradingMemoryProperties;
import com.tradepilot.ai.memory.vector.TradingMemoryVectorClient;
import com.tradepilot.ai.memory.vector.model.QdrantCollectionValidationResult;
import com.tradepilot.billing.Feature;
import com.tradepilot.config.FeatureFlagService;
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
    private final FeatureFlagService featureFlagService;

    public TradingMemoryStartupValidator(TradingMemoryProperties properties, TradingMemoryVectorClient vectorClient, FeatureFlagService featureFlagService) {
        this.properties = properties;
        this.vectorClient = vectorClient;
        this.featureFlagService = featureFlagService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void validateOnStartup() {
        if (!properties.isEnabled() || !properties.isQdrantValidateOnStartup() || !featureFlagService.isEnabled(Feature.TRADING_MEMORY)) {
            return;
        }
        try {
            QdrantCollectionValidationResult validation = vectorClient.validateCollection();
            if (!validation.operational()) {
                log.warn("Trading Memory startup validation failed. collection={}, errors={}", properties.getCollectionName(), validation.errors());
            }
        } catch (RuntimeException ex) {
            log.warn("Trading Memory startup validation failed without blocking application startup. reason={}", ex.getClass().getSimpleName());
        }
    }
}
