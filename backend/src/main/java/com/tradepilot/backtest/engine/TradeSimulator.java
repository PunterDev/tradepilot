package com.tradepilot.backtest.engine;

import com.tradepilot.backtest.model.*;
import java.math.*;
import java.time.*;
import java.util.*;

/** Simulates both exit models from one entry using only structure confirmed before that entry. */
public class TradeSimulator {
    private static final ZoneId NY = ZoneId.of("America/New_York");

    public List<CanonicalBacktestTrade> simulate(List<BacktestBar> bars, List<PlaybookV1Strategy.Signal> signals,
                                                  List<SwingDetector.Swing> swings, RunnerObjectiveSelector.Objectives objectives,
                                                  BacktestConfig config) {
        return evaluate(bars, signals, swings, objectives, config).trades();
    }

    public Evaluation evaluate(List<BacktestBar> bars, List<PlaybookV1Strategy.Signal> signals,
                               List<SwingDetector.Swing> swings, RunnerObjectiveSelector.Objectives objectives,
                               BacktestConfig config) {
        List<CanonicalBacktestTrade> output = new ArrayList<>();
        Map<LocalDate, Integer> acceptedPerSession = new HashMap<>();
        Map<String, Integer> rejections = new TreeMap<>();
        Map<String, Integer> stages = new TreeMap<>();
        for (PlaybookV1Strategy.Signal signal : signals) {
            Optional<EntryFill> fill = nextMidpointFill(bars, signal, config);
            if (fill.isEmpty()) { reject(rejections, "NO_RETRACE"); continue; }
            increment(stages, "RETRACEMENTS");
            PlaybookV1Strategy.Signal execution = new PlaybookV1Strategy.Signal(fill.get().availableAt(), signal.direction(), signal.confirmation(), signal.context(), signal.instrumentId());
            int start = fill.get().index() + 1;
            LocalDate day = execution.time().atZone(NY).toLocalDate();
            if (acceptedPerSession.getOrDefault(day, 0) >= config.maxTradesPerSession()) { reject(rejections, "MAX_TRADES_REACHED"); continue; }

            // The raw midpoint remains the causal retracement trigger. Only the fill is normalized afterward.
            BigDecimal midpointReference = execution.confirmation().midpoint();
            BigDecimal tick = config.instrument().tickSize();
            BigDecimal entry = ExecutablePriceNormalizer.roundEntryToExecutableTick(execution.direction(), midpointReference, tick);
            Optional<SwingDetector.Swing> stopSwing = latestStopSwing(swings, execution, entry);
            if (stopSwing.isEmpty()) { reject(rejections, "NO_CAUSAL_STOP"); continue; }

            BigDecimal stop = execution.direction() == BacktestTradeResult.Direction.LONG
                    ? stopSwing.get().price().subtract(tick.multiply(BigDecimal.valueOf(config.stopBufferTicks())))
                    : stopSwing.get().price().add(tick.multiply(BigDecimal.valueOf(config.stopBufferTicks())));
            BigDecimal risk = entry.subtract(stop).abs();
            if (risk.signum() <= 0) { reject(rejections, "NO_CAUSAL_STOP"); continue; }
            Optional<SwingDetector.Swing> targetSwing = nearestTargetSwing(swings, execution, entry, risk, config);
            if (targetSwing.isEmpty()) { reject(rejections, "NO_VALID_SWING_TARGET"); continue; }

            BigDecimal tp1 = targetSwing.get().price();
            RunnerObjectiveSelector.Objective runnerObjective = new RunnerObjectiveSelector().select(execution, entry, tp1, risk, objectives, swings, config);
            BigDecimal runner = runnerObjective.price();
            if (!ExecutablePriceNormalizer.isTickAligned(entry, tick)
                    || !ExecutablePriceNormalizer.isTickAligned(stop, tick)
                    || !ExecutablePriceNormalizer.isTickAligned(tp1, tick)
                    || !ExecutablePriceNormalizer.isTickAligned(runner, tick)) {
                reject(rejections, "NON_EXECUTABLE_PRICE");
                continue;
            }
            Simulation modelA = closeModelA(bars, start, execution, entry, stop, tp1, config);
            Simulation modelB = closeModelB(bars, start, execution, entry, stop, tp1, runner, config);
            BigDecimal multiplier = weekdayWeight(day.getDayOfWeek(), config);
            Map<String, Object> metadata = new LinkedHashMap<>();
            metadata.put("entryReason", "Post-confirmation midpoint retrace into 1m FVG");
            metadata.put("midpointReferencePrice", midpointReference);
            metadata.put("signalAvailableAt", signal.time().toString());
            metadata.put("entryFillBarStart", fill.get().barStart().toString());
            metadata.put("setupZone", execution.context().low() + "-" + execution.context().high());
            metadata.put("setupZoneDirection", execution.context().direction().name());
            metadata.put("ifvgConfirmationTime", execution.context().inversion() ? execution.context().confirmedAt().toString() : "N/A");
            metadata.put("setupZoneAvailableAt", execution.context().confirmedAt().toString());
            metadata.put("confirmationZone", execution.confirmation().low() + "-" + execution.confirmation().high());
            metadata.put("confirmationZoneAvailableAt", execution.confirmation().confirmedAt().toString());
            metadata.put("stopSwingTimestamp", stopSwing.get().timestamp().toString());
            metadata.put("stopSwingConfirmedAt", stopSwing.get().confirmedAt().toString());
            metadata.put("targetSwingTimestamp", targetSwing.get().timestamp().toString());
            metadata.put("targetSwingConfirmedAt", targetSwing.get().confirmedAt().toString());
            metadata.put("targetR", tp1.subtract(entry).abs().divide(risk, 6, RoundingMode.HALF_UP));
            metadata.put("targetSwingAgeTradingDays", tradingDaysBetween(targetSwing.get().confirmedAt(), execution.time()));
            metadata.put("minimumTargetR", config.effectiveMinimumTargetR());
            metadata.put("runnerObjectiveType", runnerObjective.type().name());
            metadata.put("runnerObjectivePrice", runnerObjective.price());
            metadata.put("runnerObjectiveCreatedAt", runnerObjective.createdAt() == null ? "N/A" : runnerObjective.createdAt().toString());
            metadata.put("runnerObjectiveTimeframe", runnerObjective.timeframe());
            metadata.put("runnerR", runner.subtract(entry).abs().divide(risk, 6, RoundingMode.HALF_UP));
            metadata.put("runnerObjectiveAgeTradingDays", runnerObjective.createdAt() == null ? "N/A" : tradingDaysBetween(runnerObjective.createdAt(), execution.time()));
            metadata.put("minimumRunnerAdditionalR", config.effectiveMinimumRunnerAdditionalR());
            metadata.put("runnerFvgBoundary", "LONG=LOW_NEAR_EDGE;SHORT=HIGH_NEAR_EDGE");
            metadata.put("rolloverSafe", true);

            BacktestTradeResult a = result(day, execution, entry, stop, tp1, runner, modelA, multiplier,
                    BacktestTradeResult.ExitModel.MODEL_A, metadata);
            BacktestTradeResult b = result(day, execution, entry, stop, tp1, runner, modelB, multiplier,
                    BacktestTradeResult.ExitModel.MODEL_B, metadata);
            output.add(new CanonicalBacktestTrade(day, execution.direction(), signal.time(), execution.time(), entry, stop,
                    tp1, runner, execution.instrumentId(), modelA.ambiguous || modelB.ambiguous, a, b, metadata));
            acceptedPerSession.merge(day, 1, Integer::sum);
            increment(stages, "ACCEPTED_TRADES");
        }
        return new Evaluation(output, rejections, stages);
    }

    private Optional<SwingDetector.Swing> latestStopSwing(List<SwingDetector.Swing> swings, PlaybookV1Strategy.Signal signal,
                                                           BigDecimal entry) {
        SwingDetector.Type type = signal.direction() == BacktestTradeResult.Direction.LONG ? SwingDetector.Type.LOW : SwingDetector.Type.HIGH;
        return swings.stream().filter(s -> s.instrumentId() == signal.instrumentId() && s.type() == type)
                .filter(s -> !s.confirmedAt().isAfter(signal.time()))
                .filter(s -> signal.direction() == BacktestTradeResult.Direction.LONG ? s.price().compareTo(entry) < 0 : s.price().compareTo(entry) > 0)
                .max(Comparator.comparing(SwingDetector.Swing::confirmedAt));
    }

    private Optional<SwingDetector.Swing> nearestTargetSwing(List<SwingDetector.Swing> swings, PlaybookV1Strategy.Signal signal,
                                                              BigDecimal entry, BigDecimal risk, BacktestConfig config) {
        SwingDetector.Type type = signal.direction() == BacktestTradeResult.Direction.LONG ? SwingDetector.Type.HIGH : SwingDetector.Type.LOW;
        return swings.stream().filter(s -> s.instrumentId() == signal.instrumentId() && s.type() == type)
                .filter(s -> !s.confirmedAt().isAfter(signal.time()))
                .filter(s -> profitable(signal.direction(), entry, s.price()))
                .filter(s -> tradingDaysBetween(s.confirmedAt(), signal.time()) <= config.effectiveTargetSwingLookbackTradingDays())
                .filter(s -> s.price().subtract(entry).abs().divide(risk, 6, RoundingMode.HALF_UP).compareTo(config.effectiveMinimumTargetR()) >= 0)
                .min(Comparator.comparing(s -> s.price().subtract(entry).abs()));
    }

    private Simulation closeModelA(List<BacktestBar> bars, int start, PlaybookV1Strategy.Signal signal, BigDecimal entry,
                                   BigDecimal stop, BigDecimal target, BacktestConfig config) {
        for (int i = start; i < bars.size() && bars.get(i).instrumentId() == signal.instrumentId(); i++) {
            BacktestBar bar = bars.get(i);
            boolean stopHit = hit(signal.direction(), bar, stop, false);
            boolean targetHit = hit(signal.direction(), bar, target, true);
            if (stopHit && targetHit && config.ambiguityPolicy() == BacktestConfig.AmbiguityPolicy.CONSERVATIVE_STOP_FIRST)
                return simulation(bar, stop, "STOP", true);
            if (targetHit) return simulation(bar, target, "TP1", stopHit);
            if (stopHit) return simulation(bar, stop, "STOP", false);
        }
        return sessionEnd(bars, start);
    }

    private Simulation closeModelB(List<BacktestBar> bars, int start, PlaybookV1Strategy.Signal signal, BigDecimal entry,
                                   BigDecimal stop, BigDecimal tp1, BigDecimal runner, BacktestConfig config) {
        boolean tp1Taken = false;
        for (int i = start; i < bars.size() && bars.get(i).instrumentId() == signal.instrumentId(); i++) {
            BacktestBar bar = bars.get(i);
            BigDecimal activeStop = tp1Taken && config.runnerStopAfterTp1() == BacktestConfig.RunnerStop.BREAK_EVEN ? entry : stop;
            boolean stopHit = hit(signal.direction(), bar, activeStop, false);
            boolean tpHit = !tp1Taken && hit(signal.direction(), bar, tp1, true);
            boolean runnerHit = tp1Taken && hit(signal.direction(), bar, runner, true);
            if (!tp1Taken && stopHit && tpHit && config.ambiguityPolicy() == BacktestConfig.AmbiguityPolicy.CONSERVATIVE_STOP_FIRST)
                return simulation(bar, stop, "STOP", true);
            if (!tp1Taken && tpHit) {
                tp1Taken = true;
                if (runner.compareTo(tp1) == 0) return simulation(bar, tp1, "TP1_AND_RUNNER", false);
                continue;
            }
            if (stopHit) return simulation(bar, tp1Taken ? blendedExecutableExit(signal.direction(), tp1, activeStop, config) : activeStop,
                    tp1Taken ? "RUNNER_STOP" : "STOP", false);
            if (runnerHit) return simulation(bar, blendedExecutableExit(signal.direction(), tp1, runner, config), "RUNNER", false);
        }
        Simulation end = sessionEnd(bars, start);
        return simulation(end.time, tp1Taken ? blendedExecutableExit(signal.direction(), tp1, end.exitPrice, config) : end.exitPrice, "SESSION_END", false);
    }

    private boolean hit(BacktestTradeResult.Direction direction, BacktestBar bar, BigDecimal price, boolean profit) {
        return profit == (direction == BacktestTradeResult.Direction.LONG)
                ? bar.high().compareTo(price) >= 0 : bar.low().compareTo(price) <= 0;
    }

    private Simulation sessionEnd(List<BacktestBar> bars, int start) {
        BacktestBar last = bars.get(Math.min(bars.size() - 1, start));
        return simulation(last, last.close(), "SESSION_END", false);
    }

    private BacktestTradeResult result(LocalDate day, PlaybookV1Strategy.Signal signal, BigDecimal entry, BigDecimal stop,
                                       BigDecimal tp1, BigDecimal runner, Simulation simulation, BigDecimal multiplier,
                                       BacktestTradeResult.ExitModel model, Map<String, Object> metadata) {
        BigDecimal raw = signal.direction() == BacktestTradeResult.Direction.LONG
                ? simulation.exitPrice.subtract(entry).divide(entry.subtract(stop).abs(), 6, RoundingMode.HALF_UP)
                : entry.subtract(simulation.exitPrice).divide(entry.subtract(stop).abs(), 6, RoundingMode.HALF_UP);
        Map<String, Object> details = new LinkedHashMap<>(metadata);
        details.put("exitReason", simulation.reason);
        details.put("ambiguousIntrabar", simulation.ambiguous);
        return new BacktestTradeResult(day, signal.direction(), signal.time(), entry, stop, tp1, runner, simulation.time,
                simulation.exitPrice, raw, raw.multiply(multiplier), outcome(raw), model, signal.instrumentId(), details);
    }

    private boolean profitable(BacktestTradeResult.Direction d, BigDecimal entry, BigDecimal target) { return d == BacktestTradeResult.Direction.LONG ? target.compareTo(entry) > 0 : target.compareTo(entry) < 0; }
    private Optional<EntryFill> nextMidpointFill(List<BacktestBar> bars, PlaybookV1Strategy.Signal signal, BacktestConfig config) {
        for (int index = 0; index < bars.size(); index++) {
            BacktestBar bar = bars.get(index);
            if (bar.instrumentId() != signal.instrumentId() || bar.time().isBefore(signal.time())) continue;
            if (bar.time().atZone(NY).toLocalDate().isAfter(config.endDate())) return Optional.empty();
            LocalTime time = bar.time().atZone(NY).toLocalTime();
            if (time.isBefore(config.entryStart())) continue;
            if (!time.isBefore(config.entryEnd())) return Optional.empty();
            if (touches(bar, signal.confirmation().midpoint())) return Optional.of(new EntryFill(index, bar.time(), BarTiming.availableAt(bar)));
        }
        return Optional.empty();
    }
    private boolean touches(BacktestBar bar, BigDecimal price) { return bar.low().compareTo(price) <= 0 && bar.high().compareTo(price) >= 0; }
    private int tradingDaysBetween(Instant earlier, Instant later) {
        int days = 0;
        for (LocalDate date = earlier.atZone(NY).toLocalDate(); date.isBefore(later.atZone(NY).toLocalDate()); date = date.plusDays(1))
            if (date.getDayOfWeek().getValue() <= 5) days++;
        return days;
    }
    private BacktestTradeResult.Outcome outcome(BigDecimal r) { return r.signum() > 0 ? BacktestTradeResult.Outcome.WIN : r.signum() < 0 ? BacktestTradeResult.Outcome.LOSS : BacktestTradeResult.Outcome.BREAK_EVEN; }
    private Simulation simulation(BacktestBar bar, BigDecimal price, String reason, boolean ambiguous) { return simulation(BarTiming.availableAt(bar), price, reason, ambiguous); }
    private Simulation simulation(Instant time, BigDecimal price, String reason, boolean ambiguous) { return new Simulation(time, price, reason, ambiguous); }
    private BigDecimal blendedExecutableExit(BacktestTradeResult.Direction direction, BigDecimal first, BigDecimal second, BacktestConfig config) {
        return ExecutablePriceNormalizer.roundBlendedExitToExecutableTick(direction, average(first, second), config.instrument().tickSize());
    }
    private BigDecimal average(BigDecimal first, BigDecimal second) { return first.add(second).divide(BigDecimal.valueOf(2), 6, RoundingMode.HALF_UP); }
    private BigDecimal weekdayWeight(DayOfWeek day, BacktestConfig config) { return day == DayOfWeek.MONDAY ? config.mondayRisk() : day == DayOfWeek.FRIDAY ? config.fridayRisk() : BigDecimal.ONE; }
    private void reject(Map<String, Integer> counts, String reason) { counts.merge(reason, 1, Integer::sum); }
    private void increment(Map<String, Integer> counts, String stage) { counts.merge(stage, 1, Integer::sum); }
    private record Simulation(Instant time, BigDecimal exitPrice, String reason, boolean ambiguous) { }
    private record EntryFill(int index, Instant barStart, Instant availableAt) { }
    public record Evaluation(List<CanonicalBacktestTrade> trades, Map<String, Integer> rejectionCounts, Map<String, Integer> stageCounts) { }
}
