package com.tradepilot.backtest.model;

import com.tradepilot.marketdata.model.MarketInstrument;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

public record BacktestConfig(MarketInstrument instrument, LocalDate startDate, LocalDate endDate, Direction direction,
                             LocalTime contextStart, LocalTime entryStart, LocalTime entryEnd, int maxTradesPerSession,
                             BigDecimal mondayRisk, BigDecimal fridayRisk, int minFvgTicks, int displacementLookback,
                             BigDecimal bodyMultiplier, BigDecimal rangeMultiplier, BigDecimal closeLocationThreshold,
                             int swingLeftBars, int swingRightBars, int stopBufferTicks, int maxChaseTicks,
                             int slippageTicks, BigDecimal commissionRoundTrip, AmbiguityPolicy ambiguityPolicy,
                             RunnerStop runnerStopAfterTp1, boolean debugMode, Boolean requireIfvgConfirmation,
                             BigDecimal minimumTargetR, int targetSwingLookbackTradingDays,
                             BigDecimal minimumRunnerAdditionalR, int runnerObjectiveLookbackTradingDays) {
    public static BacktestConfig conservative(MarketInstrument instrument, LocalDate start, LocalDate end) {
        return new BacktestConfig(instrument, start, end, Direction.BOTH, LocalTime.of(9, 30), LocalTime.of(9, 45), LocalTime.of(11, 0), 1,
                new BigDecimal("0.5"), new BigDecimal("0.5"), 4, 20, new BigDecimal("1.5"), new BigDecimal("1.5"), new BigDecimal("0.70"),
                2, 2, 2, 24, 0, BigDecimal.ZERO, AmbiguityPolicy.CONSERVATIVE_STOP_FIRST, RunnerStop.BREAK_EVEN, false, true,
                new BigDecimal("0.5"), 5, new BigDecimal("0.25"), 5);
    }
    public boolean requiresIfvgConfirmation() { return requireIfvgConfirmation == null || requireIfvgConfirmation; }
    public BigDecimal effectiveMinimumTargetR() { return minimumTargetR == null ? new BigDecimal("0.5") : minimumTargetR; }
    public int effectiveTargetSwingLookbackTradingDays() { return targetSwingLookbackTradingDays <= 0 ? 5 : targetSwingLookbackTradingDays; }
    public BigDecimal effectiveMinimumRunnerAdditionalR() { return minimumRunnerAdditionalR == null ? new BigDecimal("0.25") : minimumRunnerAdditionalR; }
    public int effectiveRunnerObjectiveLookbackTradingDays() { return runnerObjectiveLookbackTradingDays <= 0 ? 5 : runnerObjectiveLookbackTradingDays; }
    public enum Direction { LONG, SHORT, BOTH }
    public enum AmbiguityPolicy { CONSERVATIVE_STOP_FIRST, TARGET_FIRST, SKIP_AMBIGUOUS }
    public enum RunnerStop { BREAK_EVEN, STRUCTURE, ORIGINAL_STOP }
}
