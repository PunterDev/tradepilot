package com.tradepilot.tradereplay.service;

import java.time.Instant;
import java.util.Map;

public record MarketDataRequest(
        String canonicalInstrument,
        String actualContractSymbol,
        Instant startUtc,
        Instant endUtc,
        String candleInterval,
        Map<String, String> providerMetadata
) {}
