package com.tradepilot.backtest.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record BacktestTradeChartResponse(
        String instrument,
        String continuousSymbol,
        Long instrumentId,
        String timezone,
        String timeframe,
        List<Candle> bars,
        Overlays overlays,
        Facts facts) {
    public record Candle(Instant timestamp, BigDecimal open, BigDecimal high, BigDecimal low, BigDecimal close, Long volume) {}
    public record Zone(String label, BigDecimal low, BigDecimal high, Instant availableAt, Instant inversionConfirmedAt) {}
    public record Level(String label, BigDecimal price, Instant timestamp, String detail) {}
    public record Swing(String label, BigDecimal price, Instant timestamp, Instant confirmedAt) {}
    public record Overlays(Zone setupZone, Zone confirmationZone, Level entry, Level stop, Level tp1, Level runner, Swing stopSwing, Swing targetSwing) {}
    public record Facts(String direction, Instant entryTime, BigDecimal entryPrice, BigDecimal stopPrice, BigDecimal riskPoints,
                        BigDecimal tp1Price, BigDecimal tp1R, BigDecimal runnerPrice, BigDecimal runnerR,
                        String runnerObjectiveType, BigDecimal modelAR, BigDecimal modelBR, Long instrumentId, boolean ambiguousIntrabar) {}
}
