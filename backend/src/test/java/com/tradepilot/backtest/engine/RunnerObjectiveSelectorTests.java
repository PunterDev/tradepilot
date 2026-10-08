package com.tradepilot.backtest.engine;

import com.tradepilot.backtest.model.*;
import com.tradepilot.marketdata.model.MarketInstrument;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.*;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

class RunnerObjectiveSelectorTests {
    private static final Instant ENTRY = Instant.parse("2026-09-08T15:00:00Z");

    @Test void validFiveMinuteFvgBeatsHigherTimeframesAndUsesLongNearEdge() {
        RunnerObjectiveSelector.Objective objective = select(new RunnerObjectiveSelector.Objectives(
                List.of(zone("112.5", "114")), List.of(zone("111", "113")), List.of(zone("115", "117"))), List.of(swing("116")));
        assertThat(objective.type()).isEqualTo(RunnerObjectiveSelector.ObjectiveType.FVG_5M);
        assertThat(objective.price()).isEqualByComparingTo("112.5");
    }

    @Test void tooCloseFiveMinuteFvgDoesNotBlockValidOneHourObjective() {
        RunnerObjectiveSelector.Objective objective = select(new RunnerObjectiveSelector.Objectives(
                List.of(zone("105.5", "106")), List.of(zone("108", "110")), List.of()), List.of());
        assertThat(objective.type()).isEqualTo(RunnerObjectiveSelector.ObjectiveType.FVG_1H);
        assertThat(objective.price()).isEqualByComparingTo("108");
    }

    @Test void ignoresFutureAndStaleFvgsAndFallsBackToConfirmedSwing() {
        PriceZone future = zoneAt("112", "114", ENTRY.plusSeconds(60));
        PriceZone stale = zoneAt("112", "114", ENTRY.minus(Duration.ofDays(8)));
        RunnerObjectiveSelector.Objective objective = select(new RunnerObjectiveSelector.Objectives(List.of(future, stale), List.of(), List.of()), List.of(swing("116")));
        assertThat(objective.type()).isEqualTo(RunnerObjectiveSelector.ObjectiveType.CONFIRMED_SWING);
        assertThat(objective.price()).isEqualByComparingTo("116");
    }

    @Test void returnsTp1WhenNoCausalMeaningfulRunnerObjectiveExists() {
        RunnerObjectiveSelector.Objective objective = select(RunnerObjectiveSelector.Objectives.empty(), List.of());
        assertThat(objective.type()).isEqualTo(RunnerObjectiveSelector.ObjectiveType.NONE);
        assertThat(objective.price()).isEqualByComparingTo("105");
    }

    private RunnerObjectiveSelector.Objective select(RunnerObjectiveSelector.Objectives objectives, List<SwingDetector.Swing> swings) {
        return new RunnerObjectiveSelector().select(signal(), new BigDecimal("100"), new BigDecimal("105"), new BigDecimal("10"), objectives, swings, config());
    }
    private PlaybookV1Strategy.Signal signal() { PriceZone zone = zone("99", "101"); return new PlaybookV1Strategy.Signal(ENTRY, BacktestTradeResult.Direction.LONG, zone, zone, 1); }
    private PriceZone zone(String low, String high) { return zoneAt(low, high, ENTRY.minusSeconds(60)); }
    private PriceZone zoneAt(String low, String high, Instant availableAt) { return new PriceZone(PriceZone.Direction.BULLISH, new BigDecimal(low), new BigDecimal(high), availableAt, 1, false); }
    private SwingDetector.Swing swing(String price) { return new SwingDetector.Swing(SwingDetector.Type.HIGH, new BigDecimal(price), ENTRY.minusSeconds(120), ENTRY.minusSeconds(60), 1); }
    private BacktestConfig config() { return BacktestConfig.conservative(MarketInstrument.NQ, LocalDate.of(2026, 9, 8), LocalDate.of(2026, 9, 8)); }
}
