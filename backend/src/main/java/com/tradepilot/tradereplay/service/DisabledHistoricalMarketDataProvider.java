package com.tradepilot.tradereplay.service;

import org.springframework.stereotype.Component;

@Component
public class DisabledHistoricalMarketDataProvider implements HistoricalMarketDataProvider {
    @Override
    public String providerName() {
        return "disabled";
    }

    @Override
    public HistoricalCandleResult getCandles(MarketDataRequest request) {
        return HistoricalCandleResult.failure("PROVIDER_NOT_CONFIGURED", "Historical market data is not configured for this environment.");
    }
}
