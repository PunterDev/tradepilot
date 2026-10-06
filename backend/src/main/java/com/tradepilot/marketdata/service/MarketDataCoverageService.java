package com.tradepilot.marketdata.service;

import com.tradepilot.marketdata.dto.MarketDataCoverageResponse;
import com.tradepilot.marketdata.model.MarketInstrument;
import com.tradepilot.marketdata.repository.MarketBarStore;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
public class MarketDataCoverageService {
    private final MarketBarStore marketBarStore;

    public MarketDataCoverageService(MarketBarStore marketBarStore) {
        this.marketBarStore = marketBarStore;
    }

    public List<MarketDataCoverageResponse> coverage() {
        return Arrays.stream(MarketInstrument.values()).map(marketBarStore::coverage).toList();
    }
}
