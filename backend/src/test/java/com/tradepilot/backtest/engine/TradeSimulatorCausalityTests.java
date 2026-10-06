package com.tradepilot.backtest.engine;

import com.tradepilot.backtest.model.*;
import com.tradepilot.marketdata.model.MarketInstrument;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import static org.assertj.core.api.Assertions.assertThat;

class TradeSimulatorCausalityTests {
    private static final Instant SETUP = Instant.parse("2026-09-08T14:50:00Z");

    @Test
    void usesOnlyOpposingSwingConfirmedBeforeEntryForTarget() {
        PlaybookV1Strategy.Signal signal = signal();
        List<BacktestBar> bars = List.of(
                bar(SETUP, "100", "101", "99", "100"),
                bar(SETUP.plusSeconds(60), "100", "111", "99", "110"));
        List<SwingDetector.Swing> swings = List.of(
                swing(SwingDetector.Type.LOW, "95", SETUP.minusSeconds(180)),
                swing(SwingDetector.Type.HIGH, "110", SETUP.minusSeconds(120)),
                swing(SwingDetector.Type.HIGH, "130", SETUP.plusSeconds(120)));

        List<CanonicalBacktestTrade> results = new TradeSimulator().simulate(bars, List.of(signal), swings, RunnerObjectiveSelector.Objectives.empty(), config());

        assertThat(results).singleElement().satisfies(trade -> {
            assertThat(trade.entryTime()).isEqualTo(SETUP.plusSeconds(60));
            assertThat(trade.stopPrice()).isEqualByComparingTo("94.50");
            assertThat(trade.tp1Price()).isEqualByComparingTo("110");
            assertThat(trade.runnerTargetPrice()).isEqualByComparingTo("110");
            assertThat(trade.modelA().exitPrice()).isEqualByComparingTo("110");
            assertThat(trade.modelA().exitTime()).isEqualTo(SETUP.plusSeconds(120));
        });
    }

    @Test void midpointFillIsNotRecordedUntilTheLaterRetraceBarCloses() {
        List<BacktestBar> bars = List.of(bar(SETUP, "100", "101", "99", "100"), bar(SETUP.plusSeconds(60), "100", "111", "99", "110"));
        List<SwingDetector.Swing> swings = List.of(swing(SwingDetector.Type.LOW, "95", SETUP.minusSeconds(180)), swing(SwingDetector.Type.HIGH, "110", SETUP.minusSeconds(120)));
        assertThat(new TradeSimulator().simulate(bars, List.of(signal()), swings, RunnerObjectiveSelector.Objectives.empty(), config()))
                .singleElement().satisfies(trade -> assertThat(trade.entryTime()).isEqualTo(SETUP.plusSeconds(60)));
    }

    @Test void preservesTheRawMidpointAsTheRetracementTriggerBeforeNormalizingALongFill() {
        PriceZone fractionalZone = new PriceZone(PriceZone.Direction.BULLISH, new BigDecimal("100"), new BigDecimal("100.25"), SETUP.minusSeconds(60), 1, false);
        PlaybookV1Strategy.Signal signal = new PlaybookV1Strategy.Signal(SETUP, BacktestTradeResult.Direction.LONG, fractionalZone, fractionalZone, 1);
        List<BacktestBar> bars = List.of(
                bar(SETUP, "100.5", "101", "100.25", "100.5"),
                bar(SETUP.plusSeconds(60), "100.5", "111", "100", "110"));
        List<SwingDetector.Swing> swings = List.of(
                swing(SwingDetector.Type.LOW, "95", SETUP.minusSeconds(180)),
                swing(SwingDetector.Type.HIGH, "110", SETUP.minusSeconds(120)));

        assertThat(new TradeSimulator().simulate(bars, List.of(signal), swings, RunnerObjectiveSelector.Objectives.empty(), config()))
                .singleElement().satisfies(trade -> {
                    assertThat(trade.entryTime()).isEqualTo(SETUP.plusSeconds(120));
                    assertThat(trade.entryPrice()).isEqualByComparingTo("100.25");
                    assertThat(trade.metadata()).containsEntry("midpointReferencePrice", new BigDecimal("100.125"));
                    assertThat(trade.metadata().get("targetR")).isEqualTo(new BigDecimal("1.695652"));
                });
    }

    @Test void rejectsATargetThatFallsBelowMinimumRUsingTheNormalizedEntry() {
        PriceZone fractionalZone = new PriceZone(PriceZone.Direction.BULLISH, new BigDecimal("100"), new BigDecimal("100.25"), SETUP.minusSeconds(60), 1, false);
        PlaybookV1Strategy.Signal signal = new PlaybookV1Strategy.Signal(SETUP, BacktestTradeResult.Direction.LONG, fractionalZone, fractionalZone, 1);
        List<BacktestBar> bars = List.of(bar(SETUP, "100.5", "111", "100", "110"));
        List<SwingDetector.Swing> swings = List.of(
                swing(SwingDetector.Type.LOW, "90.5", SETUP.minusSeconds(180)),
                swing(SwingDetector.Type.HIGH, "105.25", SETUP.minusSeconds(120)));

        TradeSimulator.Evaluation result = new TradeSimulator().evaluate(bars, List.of(signal), swings, RunnerObjectiveSelector.Objectives.empty(), config());

        assertThat(result.trades()).isEmpty();
        assertThat(result.rejectionCounts()).containsEntry("NO_VALID_SWING_TARGET", 1);
    }

    @Test void recalculatesRunnerRUsingTheNormalizedEntry() {
        PriceZone fractionalZone = new PriceZone(PriceZone.Direction.BULLISH, new BigDecimal("100"), new BigDecimal("100.25"), SETUP.minusSeconds(60), 1, false);
        PlaybookV1Strategy.Signal signal = new PlaybookV1Strategy.Signal(SETUP, BacktestTradeResult.Direction.LONG, fractionalZone, fractionalZone, 1);
        List<BacktestBar> bars = List.of(bar(SETUP, "100.5", "121", "100", "120"));
        List<SwingDetector.Swing> swings = List.of(
                swing(SwingDetector.Type.LOW, "95", SETUP.minusSeconds(180)),
                swing(SwingDetector.Type.HIGH, "110", SETUP.minusSeconds(120)));
        PriceZone runnerFvg = new PriceZone(PriceZone.Direction.BULLISH, new BigDecimal("120"), new BigDecimal("121"), SETUP.minusSeconds(120), 1, false);

        assertThat(new TradeSimulator().simulate(bars, List.of(signal), swings,
                new RunnerObjectiveSelector.Objectives(List.of(runnerFvg), List.of(), List.of()), config()))
                .singleElement().satisfies(trade -> {
                    assertThat(trade.entryPrice()).isEqualByComparingTo("100.25");
                    assertThat(trade.metadata().get("runnerR")).isEqualTo(new BigDecimal("3.434783"));
                });
    }

    @Test
    void rejectsEntryWhenNoOpposingSwingWasConfirmedBeforeEntry() {
        List<BacktestBar> bars = List.of(bar(SETUP, "100", "101", "99", "100"), bar(SETUP.plusSeconds(60), "100", "111", "99", "110"));
        List<SwingDetector.Swing> swings = List.of(
                swing(SwingDetector.Type.LOW, "95", SETUP.minusSeconds(180)),
                swing(SwingDetector.Type.HIGH, "110", SETUP.plusSeconds(120)));

        assertThat(new TradeSimulator().simulate(bars, List.of(signal()), swings, RunnerObjectiveSelector.Objectives.empty(), config())).isEmpty();
    }

    @Test void exposesDistinctSimulatorRejectionReasons() {
        List<BacktestBar> bars = List.of(bar(SETUP, "100", "101", "99", "100"), bar(SETUP.plusSeconds(60), "100", "101", "99", "100"));
        TradeSimulator.Evaluation noStop = new TradeSimulator().evaluate(bars, List.of(signal()), List.of(swing(SwingDetector.Type.HIGH, "110", SETUP.minusSeconds(60))), RunnerObjectiveSelector.Objectives.empty(), config());
        TradeSimulator.Evaluation noTarget = new TradeSimulator().evaluate(bars, List.of(signal()), List.of(swing(SwingDetector.Type.LOW, "95", SETUP.minusSeconds(60))), RunnerObjectiveSelector.Objectives.empty(), config());
        assertThat(noStop.rejectionCounts()).containsEntry("NO_CAUSAL_STOP", 1);
        assertThat(noTarget.rejectionCounts()).containsEntry("NO_VALID_SWING_TARGET", 1);
    }

    @Test void rejectsAQuarterRTargetWhenMinimumTargetIsHalfR() {
        TradeSimulator.Evaluation result = evaluateTargets(List.of(swing(SwingDetector.Type.LOW, "90.5", SETUP.minusSeconds(60)), swing(SwingDetector.Type.HIGH, "100.25", SETUP.minusSeconds(60))));
        assertThat(result.trades()).isEmpty();
        assertThat(result.rejectionCounts()).containsEntry("NO_VALID_SWING_TARGET", 1);
    }

    @Test void selectsNearestMeaningfulTargetRatherThanNearestPrice() {
        TradeSimulator.Evaluation result = evaluateTargets(List.of(
                swing(SwingDetector.Type.LOW, "90.5", SETUP.minusSeconds(60)),
                swing(SwingDetector.Type.HIGH, "101", SETUP.minusSeconds(60)),
                swing(SwingDetector.Type.HIGH, "105", SETUP.minusSeconds(60)),
                swing(SwingDetector.Type.HIGH, "110", SETUP.minusSeconds(60))));
        assertThat(result.trades()).singleElement().satisfies(trade -> assertThat(trade.tp1Price()).isEqualByComparingTo("105"));
    }

    @Test void rejectsStaleTargetEvenWhenItIsOtherwiseMeaningful() {
        TradeSimulator.Evaluation result = evaluateTargets(List.of(
                swing(SwingDetector.Type.LOW, "90.5", SETUP.minusSeconds(60)),
                swing(SwingDetector.Type.HIGH, "110", SETUP.minus(Duration.ofDays(8)))));
        assertThat(result.trades()).isEmpty();
        assertThat(result.rejectionCounts()).containsEntry("NO_VALID_SWING_TARGET", 1);
    }

    private PlaybookV1Strategy.Signal signal() {
        PriceZone zone = new PriceZone(PriceZone.Direction.BULLISH, new BigDecimal("99"), new BigDecimal("101"), SETUP.minusSeconds(60), 1, false);
        return new PlaybookV1Strategy.Signal(SETUP, BacktestTradeResult.Direction.LONG, zone, zone, 1);
    }
    private TradeSimulator.Evaluation evaluateTargets(List<SwingDetector.Swing> swings) {
        List<BacktestBar> bars = List.of(bar(SETUP, "100", "101", "99", "100"), bar(SETUP.plusSeconds(60), "100", "111", "99", "110"));
        return new TradeSimulator().evaluate(bars, List.of(signal()), swings, RunnerObjectiveSelector.Objectives.empty(), config());
    }
    private SwingDetector.Swing swing(SwingDetector.Type type, String price, Instant confirmedAt) {
        return new SwingDetector.Swing(type, new BigDecimal(price), confirmedAt.minusSeconds(60), confirmedAt, 1);
    }
    private BacktestBar bar(Instant time, String open, String high, String low, String close) {
        return new BacktestBar(time, 1, new BigDecimal(open), new BigDecimal(high), new BigDecimal(low), new BigDecimal(close), 1);
    }
    private BacktestConfig config() { return BacktestConfig.conservative(MarketInstrument.NQ, LocalDate.of(2026, 9, 8), LocalDate.of(2026, 9, 8)); }
}
