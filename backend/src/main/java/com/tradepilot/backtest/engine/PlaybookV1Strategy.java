package com.tradepilot.backtest.engine;

import com.tradepilot.backtest.model.*;
import java.math.*;
import java.time.*;
import java.util.*;

/** Conservative causal workflow: completed 5m FVG/IFVG context, 1m displacement, then a later retrace. */
public class PlaybookV1Strategy {
    private static final ZoneId NY = ZoneId.of("America/New_York");
    private final FvgDetector fvg = new FvgDetector();
    private final IfvgDetector ifvg = new IfvgDetector();
    private final DisplacementDetector displacement = new DisplacementDetector();

    public List<Signal> signals(List<BacktestBar> minuteBars, BacktestConfig config) {
        return evaluate(minuteBars, config).signals();
    }

    public Evaluation evaluate(List<BacktestBar> minuteBars, BacktestConfig config) {
        List<BacktestBar> five = new TimeframeAggregationService().aggregate(minuteBars, Duration.ofMinutes(5));
        List<PriceZone> detectedContexts = regularFiveMinuteContexts(five, config);
        List<PriceZone> contexts = resolvedFiveMinuteContexts(five, detectedContexts, config);
        List<Signal> signals = new ArrayList<>();
        Map<String, Integer> rejections = new TreeMap<>();
        Map<String, Integer> stages = new TreeMap<>();
        stages.put("FIVE_MINUTE_ZONES_DETECTED", detectedContexts.size());
        stages.put("FIVE_MINUTE_IFVGS_CONFIRMED", Math.toIntExact(contexts.stream().filter(PriceZone::inversion).count()));
        List<PriceZone> oneMinute = new ArrayList<>();
        long currentContract = -1;
        boolean interacted = false;
        PriceZone context = null;
        for (int i = 2; i < minuteBars.size(); i++) {
            BacktestBar bar = minuteBars.get(i);
            if (currentContract != -1 && currentContract != bar.instrumentId()) {
                oneMinute.clear(); interacted = false; context = null;
                increment(stages, "ROLLOVER_RESETS");
            }
            currentContract = bar.instrumentId();
            ZonedDateTime ny = bar.time().atZone(NY);
            fvg.detect(minuteBars.get(i - 2), minuteBars.get(i - 1), bar, minimumSize(config))
                    .map(zone -> retime(zone, BarTiming.availableAt(bar), false)).ifPresent(oneMinute::add);
            if (ny.toLocalDate().isBefore(config.startDate()) || ny.toLocalDate().isAfter(config.endDate())) continue;
            if (ny.toLocalTime().isBefore(config.contextStart())) { reject(rejections, "OUTSIDE_SESSION"); continue; }
            context = latestContext(contexts, bar, config.requiresIfvgConfirmation());
            if (context == null) { reject(rejections, config.requiresIfvgConfirmation() ? "NO_IFVG_CONFIRMATION" : "NO_5M_CONTEXT"); continue; }
            if (touches(bar, context)) { interacted = true; increment(stages, "ZONE_TOUCHES"); }
            if (!interacted) { reject(rejections, "NO_ZONE_TOUCH"); continue; }
            if (!inDirection(context, bar, config.direction()) || !isDisplacement(minuteBars, i, config)) { reject(rejections, "NO_DISPLACEMENT"); continue; }
            increment(stages, "ONE_MINUTE_DISPLACEMENTS");
            PriceZone confirmation = latestDirection(oneMinute, context.direction(), bar.instrumentId());
            if (confirmation == null || !confirmation.confirmedAt().isBefore(bar.time())) { reject(rejections, "NO_CONFIRMATION_ZONE"); continue; }
            increment(stages, "CONFIRMATION_ZONES");
            if (!withinEntry(ny, config)) { reject(rejections, "OUTSIDE_SESSION"); continue; }
            if (exceedsChase(bar, confirmation, config)) { reject(rejections, "CHASE_FILTER"); continue; }
            // A later one-minute candle must supply the actual midpoint retrace/fill.
            signals.add(new Signal(BarTiming.availableAt(bar), direction(context), confirmation, context, bar.instrumentId()));
            increment(stages, "CANDIDATE_SETUPS");
            interacted = false;
        }
        return new Evaluation(signals, rejections, stages);
    }

    /** Produces only zones whose source and inversion bars are fully closed on the 5m clock. */
    List<PriceZone> fiveMinuteContexts(List<BacktestBar> five, BacktestConfig config) {
        return resolvedFiveMinuteContexts(five, regularFiveMinuteContexts(five, config), config);
    }
    private List<PriceZone> regularFiveMinuteContexts(List<BacktestBar> five, BacktestConfig config) {
        List<PriceZone> regular = new ArrayList<>();
        for (int i = 2; i < five.size(); i++) {
            BacktestBar completed = five.get(i);
            fvg.detect(five.get(i - 2), five.get(i - 1), completed, minimumSize(config))
                    .ifPresent(zone -> regular.add(retime(zone, BarTiming.availableAt(completed.time(), Duration.ofMinutes(5)), false)));
        }
        return regular;
    }
    private List<PriceZone> resolvedFiveMinuteContexts(List<BacktestBar> five, List<PriceZone> regular, BacktestConfig config) {
        if (!config.requiresIfvgConfirmation()) return regular;
        List<PriceZone> inversions = new ArrayList<>();
        for (PriceZone zone : regular) {
            for (BacktestBar bar : five) {
                Instant closeTime = BarTiming.availableAt(bar.time(), Duration.ofMinutes(5));
                if (bar.instrumentId() != zone.instrumentId() || !closeTime.isAfter(zone.confirmedAt())) continue;
                ifvg.invert(zone, bar, false, false)
                        .map(inverted -> retime(inverted, closeTime, true))
                        .ifPresent(inversions::add);
                if (inversions.stream().anyMatch(inverted -> inverted.instrumentId() == zone.instrumentId()
                        && inverted.low().compareTo(zone.low()) == 0 && inverted.high().compareTo(zone.high()) == 0)) break;
            }
        }
        return inversions;
    }

    private PriceZone retime(PriceZone zone, Instant availableAt, boolean inversion) {
        return new PriceZone(zone.direction(), zone.low(), zone.high(), availableAt, zone.instrumentId(), inversion);
    }
    private PriceZone latestContext(List<PriceZone> zones, BacktestBar bar, boolean requireInversion) {
        return zones.stream().filter(zone -> zone.instrumentId() == bar.instrumentId() && BarTiming.availableBy(zone.confirmedAt(), bar.time()))
                .filter(zone -> !requireInversion || zone.inversion()).reduce((first, second) -> second).orElse(null);
    }
    private PriceZone latestDirection(List<PriceZone> zones, PriceZone.Direction direction, long instrumentId) {
        return zones.stream().filter(zone -> zone.direction() == direction && zone.instrumentId() == instrumentId)
                .reduce((first, second) -> second).orElse(null);
    }
    private boolean touches(BacktestBar bar, PriceZone zone) { return bar.low().compareTo(zone.high()) <= 0 && bar.high().compareTo(zone.low()) >= 0; }
    private boolean inDirection(PriceZone zone, BacktestBar bar, BacktestConfig.Direction requested) {
        boolean directionAllowed = requested == BacktestConfig.Direction.BOTH || (requested == BacktestConfig.Direction.LONG) == (zone.direction() == PriceZone.Direction.BULLISH);
        return directionAllowed && ((zone.direction() == PriceZone.Direction.BULLISH && bar.close().compareTo(bar.open()) > 0)
                || (zone.direction() == PriceZone.Direction.BEARISH && bar.close().compareTo(bar.open()) < 0));
    }
    private BacktestTradeResult.Direction direction(PriceZone zone) { return zone.direction() == PriceZone.Direction.BULLISH ? BacktestTradeResult.Direction.LONG : BacktestTradeResult.Direction.SHORT; }
    private boolean withinEntry(ZonedDateTime time, BacktestConfig config) { return !time.toLocalTime().isBefore(config.entryStart()) && time.toLocalTime().isBefore(config.entryEnd()); }
    private boolean isDisplacement(List<BacktestBar> bars, int index, BacktestConfig config) { return displacement.measure(bars, index, config.displacementLookback(), config.bodyMultiplier(), config.rangeMultiplier(), config.closeLocationThreshold()).map(DisplacementDetector.Measurement::qualifies).orElse(false); }
    private boolean exceedsChase(BacktestBar bar, PriceZone zone, BacktestConfig config) { return bar.close().subtract(zone.midpoint()).abs().compareTo(minimumSize(config).multiply(BigDecimal.valueOf(config.maxChaseTicks()))) > 0; }
    private BigDecimal minimumSize(BacktestConfig config) { return new BigDecimal("0.25").multiply(BigDecimal.valueOf(config.minFvgTicks())); }
    private void reject(Map<String, Integer> counts, String reason) { counts.merge(reason, 1, Integer::sum); }
    private void increment(Map<String, Integer> counts, String stage) { counts.merge(stage, 1, Integer::sum); }
    public record Signal(Instant time, BacktestTradeResult.Direction direction, PriceZone confirmation, PriceZone context, long instrumentId) { }
    public record Evaluation(List<Signal> signals, Map<String, Integer> rejectionCounts, Map<String, Integer> stageCounts) { }
}
