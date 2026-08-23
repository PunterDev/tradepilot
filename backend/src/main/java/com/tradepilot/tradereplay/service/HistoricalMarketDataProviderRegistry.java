package com.tradepilot.tradereplay.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class HistoricalMarketDataProviderRegistry {
    private static final Logger log = LoggerFactory.getLogger(HistoricalMarketDataProviderRegistry.class);

    private final MarketDataProperties properties;
    private final DatabentoHistoricalMarketDataProvider databentoProvider;
    private final DisabledHistoricalMarketDataProvider disabledProvider;

    public HistoricalMarketDataProviderRegistry(MarketDataProperties properties, DatabentoHistoricalMarketDataProvider databentoProvider, DisabledHistoricalMarketDataProvider disabledProvider) {
        this.properties = properties;
        this.databentoProvider = databentoProvider;
        this.disabledProvider = disabledProvider;
    }

    public HistoricalMarketDataProvider activeProvider() {
        String provider = properties.getProvider() == null ? "" : properties.getProvider().trim().toLowerCase();
        if ("databento".equals(provider)) {
            return databentoProvider;
        }
        return disabledProvider;
    }

    public String configuredProvider() {
        return properties.getProvider() == null || properties.getProvider().isBlank()
                ? "disabled"
                : properties.getProvider().trim();
    }

    public boolean databentoProviderAvailable() {
        return databentoProvider != null;
    }

    public boolean databentoApiKeyConfigured() {
        String apiKey = properties.getDatabento() == null ? null : properties.getDatabento().getApiKey();
        return apiKey != null && !apiKey.isBlank();
    }

    @EventListener(ApplicationReadyEvent.class)
    public void logConfiguration() {
        MarketDataProperties.Databento databento = properties.getDatabento();
        log.info(
                "Market data configuration initialized. provider={}, activeProvider={}, databentoProviderAvailable={}, databentoKeyConfigured={}, databentoDataset={}, databentoSchema={}, replayDelayHours={}",
                configuredProvider(),
                activeProvider().providerName(),
                databentoProviderAvailable(),
                databentoApiKeyConfigured(),
                databento == null ? null : databento.getDataset(),
                databento == null ? null : databento.getSchema(),
                properties.getReplayDelayHours()
        );
    }
}
