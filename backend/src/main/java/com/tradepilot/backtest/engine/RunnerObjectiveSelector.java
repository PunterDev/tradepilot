package com.tradepilot.backtest.engine;

import com.tradepilot.backtest.model.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

/** Selects a causally available Model B runner objective in the documented priority order. */
public class RunnerObjectiveSelector {
    private static final ZoneId NEW_YORK = ZoneId.of("America/New_York");

    public Objective select(PlaybookV1Strategy.Signal signal, BigDecimal entry, BigDecimal tp1, BigDecimal initialRisk,
                            Objectives objectives, List<SwingDetector.Swing> swings, BacktestConfig config) {
        for (ObjectiveType type : List.of(ObjectiveType.FVG_5M, ObjectiveType.FVG_1H, ObjectiveType.FVG_4H)) {
            Optional<Objective> fvg = objectives.zones(type).stream()
                    .filter(zone -> zone.instrumentId() == signal.instrumentId() && !zone.confirmedAt().isAfter(signal.time()))
                    .filter(zone -> withinTradingDayLookback(zone.confirmedAt(), signal.time(), config.effectiveRunnerObjectiveLookbackTradingDays()))
                    .map(zone -> new Objective(type, nearEdge(signal.direction(), zone), zone.confirmedAt(), timeframe(type)))
                    .filter(objective -> meaningfulRunner(signal.direction(), entry, tp1, initialRisk, objective.price(), config))
                    .min(Comparator.comparing(objective -> objective.price().subtract(tp1).abs()));
            if (fvg.isPresent()) return fvg.get();
        }
        return swings.stream().filter(swing -> swing.instrumentId() == signal.instrumentId() && !swing.confirmedAt().isAfter(signal.time()))
                .filter(swing -> withinTradingDayLookback(swing.confirmedAt(), signal.time(), config.effectiveRunnerObjectiveLookbackTradingDays()))
                .map(swing -> new Objective(ObjectiveType.CONFIRMED_SWING, swing.price(), swing.confirmedAt(), "1m"))
                .filter(objective -> meaningfulRunner(signal.direction(), entry, tp1, initialRisk, objective.price(), config))
                .min(Comparator.comparing(objective -> objective.price().subtract(tp1).abs()))
                .orElse(new Objective(ObjectiveType.NONE, tp1, null, null));
    }

    private BigDecimal nearEdge(BacktestTradeResult.Direction direction, PriceZone zone) {
        return direction == BacktestTradeResult.Direction.LONG ? zone.low() : zone.high();
    }

    private boolean meaningfulRunner(BacktestTradeResult.Direction direction, BigDecimal entry, BigDecimal tp1,
                                     BigDecimal risk, BigDecimal candidate, BacktestConfig config) {
        if (!beyond(direction, candidate, tp1)) return false;
        BigDecimal additionalR = candidate.subtract(tp1).abs().divide(risk, 6, java.math.RoundingMode.HALF_UP);
        return additionalR.compareTo(config.effectiveMinimumRunnerAdditionalR()) >= 0
                && (direction == BacktestTradeResult.Direction.LONG ? candidate.compareTo(entry) > 0 : candidate.compareTo(entry) < 0);
    }

    private boolean withinTradingDayLookback(Instant candidate, Instant entry, int lookbackTradingDays) {
        return tradingDaysBetween(candidate.atZone(NEW_YORK).toLocalDate(), entry.atZone(NEW_YORK).toLocalDate()) <= lookbackTradingDays;
    }

    private int tradingDaysBetween(LocalDate earlier, LocalDate later) {
        int days = 0;
        for (LocalDate date = earlier; date.isBefore(later); date = date.plusDays(1)) {
            if (date.getDayOfWeek().getValue() <= 5) days++;
        }
        return days;
    }

    private boolean beyond(BacktestTradeResult.Direction direction, BigDecimal candidate, BigDecimal reference) {
        return direction == BacktestTradeResult.Direction.LONG ? candidate.compareTo(reference) > 0 : candidate.compareTo(reference) < 0;
    }
    private String timeframe(ObjectiveType type) { return type == ObjectiveType.FVG_5M ? "5m" : type == ObjectiveType.FVG_1H ? "1h" : "4h"; }
    public enum ObjectiveType { FVG_5M, FVG_1H, FVG_4H, CONFIRMED_SWING, NONE }
    public record Objective(ObjectiveType type, BigDecimal price, Instant createdAt, String timeframe) { }
    public record Objectives(List<PriceZone> fiveMinute, List<PriceZone> oneHour, List<PriceZone> fourHour) {
        public List<PriceZone> zones(ObjectiveType type) { return type == ObjectiveType.FVG_5M ? fiveMinute : type == ObjectiveType.FVG_1H ? oneHour : type == ObjectiveType.FVG_4H ? fourHour : List.of(); }
        public static Objectives empty() { return new Objectives(List.of(), List.of(), List.of()); }
    }
}
