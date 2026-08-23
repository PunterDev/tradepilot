package com.tradepilot.tradereplay.service;

import java.util.List;

public record HistoricalCandleResult(
        boolean success,
        String failureCode,
        String sanitizedMessage,
        List<HistoricalCandle> candles
) {
    public static HistoricalCandleResult success(List<HistoricalCandle> candles) {
        return new HistoricalCandleResult(true, null, null, candles == null ? List.of() : candles);
    }

    public static HistoricalCandleResult failure(String code, String message) {
        return new HistoricalCandleResult(false, code, message, List.of());
    }
}
