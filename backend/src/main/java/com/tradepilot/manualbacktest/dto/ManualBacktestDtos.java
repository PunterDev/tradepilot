package com.tradepilot.manualbacktest.dto;

import com.tradepilot.manualbacktest.entity.*;
import com.tradepilot.marketdata.model.MarketInstrument;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

public final class ManualBacktestDtos {
    private ManualBacktestDtos() { }
    public record Access(boolean allowed, String entitlementKey, String label, String badge) { }
    public record CreateSession(MarketInstrument symbol, LocalDate tradingDate, LocalTime replayStart, LocalTime replayEnd, ManualBacktestSession.RiskProfile riskProfile, String notes) { }
    public record Advance(int count, Instant expectedCursorTs) { }
    public record Order(ManualBacktestTrade.Direction direction, ManualBacktestTrade.EntryOrderType orderType, BigDecimal configuredEntryPrice, BigDecimal stopPrice, BigDecimal targetPrice, String notes) { }
    public record PositionChange(BigDecimal entryPrice, BigDecimal stopPrice, BigDecimal targetPrice, String notes) { }
    public record Session(Long id, String symbol, String continuousSymbol, LocalDate tradingDate, String timezone, Instant startTime, Instant endTime, Instant cursorTs, String status, String riskProfile, String notes, Instant createdAt, Instant updatedAt, Instant completedAt, Summary summary) { }
    public record Trade(Long id, String direction, String orderType, BigDecimal configuredEntryPrice, Long instrumentId, Instant orderPlacedTs, Instant entryTs, BigDecimal entryPrice, BigDecimal initialStopPrice, BigDecimal currentStopPrice, BigDecimal targetPrice, Instant exitTs, BigDecimal exitPrice, String exitReason, BigDecimal initialRiskPoints, BigDecimal rawR, BigDecimal riskMultiplier, BigDecimal weightedR, String status, String pendingAction, String notes) { }
    public record Candle(Instant timestamp, BigDecimal open, BigDecimal high, BigDecimal low, BigDecimal close, long volume, boolean complete) { }
    public record Chart(String symbol, String continuousSymbol, String timezone, String timeframe, Instant cursorTs, List<Candle> bars, Trade activeTrade, Trade latestClosedTrade) { }
    public record Summary(int trades, int pendingOrders, int wins, int losses, int breakEvens, BigDecimal winRate, BigDecimal rawR, BigDecimal weightedR, BigDecimal averageR, BigDecimal averageWinner, BigDecimal averageLoser, BigDecimal profitFactor, BigDecimal maxDrawdown, int maxLosingStreak) { }
}
