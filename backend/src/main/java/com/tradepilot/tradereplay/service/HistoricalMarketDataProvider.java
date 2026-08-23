package com.tradepilot.tradereplay.service;

public interface HistoricalMarketDataProvider {
    String providerName();
    HistoricalCandleResult getCandles(MarketDataRequest request);
}
