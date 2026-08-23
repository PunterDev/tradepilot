package com.tradepilot;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradepilot.tradereplay.service.DatabentoHistoricalMarketDataProvider;
import com.tradepilot.tradereplay.service.DisabledHistoricalMarketDataProvider;
import com.tradepilot.tradereplay.service.HistoricalMarketDataProviderRegistry;
import com.tradepilot.tradereplay.service.MarketDataProperties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class MarketDataProviderConfigurationTests {
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(TestConfiguration.class);

    @Test
    void marketDataProviderAliasSelectsDatabentoWhenKeyIsConfigured() {
        contextRunner
                .withPropertyValues(
                        "MARKET_DATA_PROVIDER=databento",
                        "DATABENTO_API_KEY=test-databento-key",
                        "tradepilot.market-data.provider=${MARKET_DATA_PROVIDER:disabled}",
                        "tradepilot.market-data.databento.api-key=${DATABENTO_API_KEY:}",
                        "tradepilot.market-data.databento.dataset=${DATABENTO_DATASET:GLBX.MDP3}",
                        "tradepilot.market-data.databento.schema=${DATABENTO_SCHEMA:ohlcv-1m}"
                )
                .run(context -> {
                    MarketDataProperties properties = context.getBean(MarketDataProperties.class);
                    HistoricalMarketDataProviderRegistry registry = context.getBean(HistoricalMarketDataProviderRegistry.class);

                    assertThat(properties.getProvider()).isEqualTo("databento");
                    assertThat(properties.getDatabento().getApiKey()).isEqualTo("test-databento-key");
                    assertThat(properties.getDatabento().getDataset()).isEqualTo("GLBX.MDP3");
                    assertThat(properties.getDatabento().getSchema()).isEqualTo("ohlcv-1m");
                    assertThat(registry.activeProvider().providerName()).isEqualTo("databento");
                    assertThat(registry.databentoApiKeyConfigured()).isTrue();
                    assertThat(registry.databentoProviderAvailable()).isTrue();
                });
    }

    @Test
    void marketDataProviderDefaultsToDisabledWhenAliasIsMissing() {
        contextRunner
                .withPropertyValues(
                        "tradepilot.market-data.provider=${MARKET_DATA_PROVIDER:disabled}",
                        "tradepilot.market-data.databento.api-key=${DATABENTO_API_KEY:}"
                )
                .run(context -> {
                    MarketDataProperties properties = context.getBean(MarketDataProperties.class);
                    HistoricalMarketDataProviderRegistry registry = context.getBean(HistoricalMarketDataProviderRegistry.class);

                    assertThat(properties.getProvider()).isEqualTo("disabled");
                    assertThat(registry.activeProvider().providerName()).isEqualTo("disabled");
                    assertThat(registry.databentoApiKeyConfigured()).isFalse();
                });
    }

    @EnableConfigurationProperties(MarketDataProperties.class)
    static class TestConfiguration {
        @org.springframework.context.annotation.Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }

        @org.springframework.context.annotation.Bean
        DatabentoHistoricalMarketDataProvider databentoHistoricalMarketDataProvider(
                MarketDataProperties properties,
                ObjectMapper objectMapper
        ) {
            return new DatabentoHistoricalMarketDataProvider(properties, objectMapper);
        }

        @org.springframework.context.annotation.Bean
        DisabledHistoricalMarketDataProvider disabledHistoricalMarketDataProvider() {
            return new DisabledHistoricalMarketDataProvider();
        }

        @org.springframework.context.annotation.Bean
        HistoricalMarketDataProviderRegistry historicalMarketDataProviderRegistry(
                MarketDataProperties properties,
                DatabentoHistoricalMarketDataProvider databentoProvider,
                DisabledHistoricalMarketDataProvider disabledProvider
        ) {
            return new HistoricalMarketDataProviderRegistry(properties, databentoProvider, disabledProvider);
        }
    }
}
