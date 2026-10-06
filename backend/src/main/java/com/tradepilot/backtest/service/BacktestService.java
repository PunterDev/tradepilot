package com.tradepilot.backtest.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.tradepilot.backtest.dto.BacktestTradeChartResponse;
import com.tradepilot.backtest.dto.BacktestAccessResponse;
import com.tradepilot.backtest.engine.*;
import com.tradepilot.backtest.entity.*;
import com.tradepilot.backtest.model.*;
import com.tradepilot.backtest.repository.*;
import com.tradepilot.entity.User;
import com.tradepilot.entitlement.FeatureEntitlementKey;
import com.tradepilot.entitlement.FeatureEntitlementService;
import com.tradepilot.exception.ResourceNotFoundException;
import com.tradepilot.marketdata.entity.MarketBar;
import com.tradepilot.marketdata.model.MarketInstrument;
import com.tradepilot.marketdata.repository.MarketBarRepository;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import java.math.*;
import java.time.*;
import java.util.*;

@Service
public class BacktestService {
    private final BacktestRunRepository runs; private final BacktestTradeRepository trades;
    private final BacktestDataService data; private final MarketBarRepository marketBars; private final ObjectMapper json; private final BacktestWorker worker; private final FeatureEntitlementService entitlementService;
    public BacktestService(BacktestRunRepository runs, BacktestTradeRepository trades, BacktestDataService data,
                           MarketBarRepository marketBars, ObjectMapper json, @Lazy BacktestWorker worker, FeatureEntitlementService entitlementService) {
        this.runs = runs; this.trades = trades; this.data = data; this.marketBars = marketBars; this.json = json; this.worker = worker; this.entitlementService = entitlementService;
    }
    public BacktestAccessResponse access(User user) { return BacktestAccessResponse.privateBeta(entitlementService.hasEntitlement(user, FeatureEntitlementKey.PLAYBOOK_BACKTEST_BETA)); }
    public BacktestRun start(User user, BacktestConfig config) {
        requirePrivateAccess(user);
        try {
            BacktestRun run = new BacktestRun(); run.setUserId(user.getId()); run.setInstrument(config.instrument().name());
            run.setStrategyVersion("PLAYBOOK_V1"); run.setStartDate(config.startDate()); run.setEndDate(config.endDate());
            run.setConfigJson(json.writeValueAsString(config)); run.setStatus("QUEUED"); run.setCreatedAt(Instant.now());
            run = runs.save(run); worker.run(run.getId(), config); return run;
        } catch (Exception exception) { throw new IllegalArgumentException("Backtest request is invalid.", exception); }
    }
    public BacktestRun get(User user, Long id) { requirePrivateAccess(user); return runs.findByIdAndUserId(id, user.getId()).orElseThrow(() -> new ResourceNotFoundException("Backtest run not found")); }
    public List<BacktestRun> list(User user) { requirePrivateAccess(user); return runs.findTop20ByUserIdOrderByCreatedAtDesc(user.getId()); }
    public List<BacktestTrade> trades(User user, Long id) { requirePrivateAccess(user); get(user, id); return trades.findByBacktestRunIdOrderByEntryTimeAsc(id); }

    public BacktestTradeChartResponse chart(User user, Long runId, Long tradeId, String timeframe) {
        requirePrivateAccess(user);
        get(user, runId);
        if (!Set.of("1m", "5m", "1h", "4h").contains(timeframe)) throw new IllegalArgumentException("Chart timeframe must be 1m, 5m, 1h, or 4h.");
        BacktestTrade trade = trades.findByIdAndBacktestRunId(tradeId, runId)
                .orElseThrow(() -> new ResourceNotFoundException("Backtest trade not found"));
        Instant exit = trade.getFinalExitTime() == null ? trade.getEntryTime() : trade.getFinalExitTime();
        Instant start = chartStart(trade.getEntryTime(), timeframe);
        Instant end = chartEnd(exit, timeframe);
        MarketInstrument instrument = MarketInstrument.from(trade.getInstrument());
        List<MarketBar> stored = marketBars.findBySymbolAndInstrumentIdAndTimeframeAndTsEventGreaterThanEqualAndTsEventLessThanOrderByTsEventAsc(
                instrument, trade.getInstrumentId(), "1m", start, end);
        List<BacktestBar> oneMinute = stored.stream().map(bar -> new BacktestBar(
                bar.getTsEvent(), bar.getInstrumentId(), bar.getOpen(), bar.getHigh(), bar.getLow(), bar.getClose(), bar.getVolume())).toList();
        List<BacktestBar> display = "1m".equals(timeframe)
                ? oneMinute : new TimeframeAggregationService().aggregate(oneMinute, chartInterval(timeframe));
        JsonNode metadata = metadata(trade);
        return new BacktestTradeChartResponse(trade.getInstrument(), trade.getContinuousSymbol(), trade.getInstrumentId(), "America/New_York", timeframe,
                display.stream().map(bar -> new BacktestTradeChartResponse.Candle(bar.time(), bar.open(), bar.high(), bar.low(), bar.close(), bar.volume())).toList(),
                overlays(trade, metadata), facts(trade, metadata));
    }
    private Instant chartStart(Instant entry, String timeframe) {
        return switch (timeframe) {
            case "1m" -> entry.minus(Duration.ofMinutes(45));
            case "5m" -> entry.minus(Duration.ofHours(3));
            case "1h" -> tradingDayStart(entry, 5);
            case "4h" -> tradingDayStart(entry, 15);
            default -> throw new IllegalArgumentException("Unsupported chart timeframe.");
        };
    }
    private Instant chartEnd(Instant exit, String timeframe) {
        return exit.plus(switch (timeframe) {
            case "1m" -> Duration.ofMinutes(30);
            case "5m" -> Duration.ofHours(1);
            case "1h" -> Duration.ofHours(4);
            case "4h" -> Duration.ofHours(8);
            default -> throw new IllegalArgumentException("Unsupported chart timeframe.");
        });
    }
    private Instant tradingDayStart(Instant entry, int tradingDaysBefore) {
        ZoneId exchangeZone = ZoneId.of("America/New_York");
        LocalDate date = entry.atZone(exchangeZone).toLocalDate();
        int found = 0;
        while (found < tradingDaysBefore) {
            date = date.minusDays(1);
            if (date.getDayOfWeek() != DayOfWeek.SATURDAY && date.getDayOfWeek() != DayOfWeek.SUNDAY) found++;
        }
        return date.atStartOfDay(exchangeZone).toInstant();
    }
    private Duration chartInterval(String timeframe) {
        return switch (timeframe) {
            case "5m" -> Duration.ofMinutes(5);
            case "1h" -> Duration.ofHours(1);
            case "4h" -> Duration.ofHours(4);
            default -> throw new IllegalArgumentException("Unsupported chart timeframe.");
        };
    }
    private void requirePrivateAccess(User user) { entitlementService.requireEntitlement(user, FeatureEntitlementKey.PLAYBOOK_BACKTEST_BETA); }

    void execute(Long id, BacktestConfig config) {
        BacktestRun run = runs.findById(id).orElseThrow();
        try {
            run.setStatus("RUNNING"); run.setStartedAt(Instant.now()); runs.save(run);
            List<BacktestBar> bars = data.load(config.instrument(), config.startDate(), config.endDate());
            PlaybookV1Strategy.Evaluation evaluation = new PlaybookV1Strategy().evaluate(bars, config);
            List<PlaybookV1Strategy.Signal> signals = evaluation.signals();
            List<SwingDetector.Swing> swings = new SwingDetector().confirmed(bars, config.swingLeftBars(), config.swingRightBars());
            RunnerObjectiveSelector.Objectives objectives = new RunnerObjectiveSelector.Objectives(
                    fvgObjectives(bars, Duration.ofMinutes(5), config),
                    fvgObjectives(bars, Duration.ofHours(1), config),
                    fvgObjectives(bars, Duration.ofHours(4), config));
            TradeSimulator.Evaluation simulation = new TradeSimulator().evaluate(bars, signals, swings, objectives, config);
            List<CanonicalBacktestTrade> simulated = simulation.trades();
            persistCanonicalTrades(id, config, simulated);
            BacktestMetrics aMetrics = new BacktestMetricsCalculator().calculate(simulated.stream().map(CanonicalBacktestTrade::modelA).toList());
            BacktestMetrics bMetrics = new BacktestMetricsCalculator().calculate(simulated.stream().map(CanonicalBacktestTrade::modelB).toList());
            run.metrics(bMetrics.totalTrades(), bMetrics.winRate(), bMetrics.weightedTotalR(), bMetrics.expectancy(), bMetrics.maxDrawdownR());
            Map<String,Object> result = new LinkedHashMap<>();
            result.put("modelA", aMetrics); result.put("modelB", bMetrics); result.put("breakdowns", new BacktestBreakdownCalculator().calculate(simulated.stream().map(CanonicalBacktestTrade::modelA).toList(), simulated.stream().map(CanonicalBacktestTrade::modelB).toList()));
            Map<String, Integer> rejectionCounts = new TreeMap<>(evaluation.rejectionCounts());
            simulation.rejectionCounts().forEach((reason, count) -> rejectionCounts.merge(reason, count, Integer::sum));
            Map<String, Integer> stageCounts = new TreeMap<>(evaluation.stageCounts());
            simulation.stageCounts().forEach((stage, count) -> stageCounts.merge(stage, count, Integer::sum));
            result.put("candidateSetups", signals.size()); result.put("acceptedCanonicalTrades", simulated.size()); result.put("rejectionCounts", rejectionCounts); result.put("trades", simulated);
            result.put("stageCounts", stageCounts);
            run.setResultJson(json.writeValueAsString(result));
            run.setStatus("COMPLETED"); run.setCompletedAt(Instant.now()); runs.save(run);
        } catch (Exception exception) {
            run.setStatus("FAILED"); run.setErrorMessage("Backtest could not complete. Review stored market-data coverage and server logs.");
            run.setCompletedAt(Instant.now()); runs.save(run);
        }
    }
    private void persistCanonicalTrades(Long runId, BacktestConfig config, List<CanonicalBacktestTrade> simulated) throws Exception {
        trades.deleteByBacktestRunId(runId);
        for (CanonicalBacktestTrade trade : simulated) {
            if (trade.modelA().entryPrice().compareTo(trade.modelB().entryPrice()) != 0 || trade.modelA().stopPrice().compareTo(trade.modelB().stopPrice()) != 0)
                throw new IllegalStateException("Exit models must share a canonical entry and stop");
            BacktestTrade saved = new BacktestTrade();
            Map<String, Object> metadata = new LinkedHashMap<>(trade.metadata());
            metadata.put("modelAExitTime", trade.modelA().exitTime().toString());
            metadata.put("modelAExitPrice", trade.modelA().exitPrice());
            metadata.put("modelBExitTime", trade.modelB().exitTime().toString());
            metadata.put("modelBExitPrice", trade.modelB().exitPrice());
            metadata.put("modelAOutcome", trade.modelA().outcome().name());
            metadata.put("modelBOutcome", trade.modelB().outcome().name());
            metadata.put("ambiguousIntrabar", trade.ambiguousIntrabar());
            saved.fill(runId, trade.tradeDate(), config.instrument().name(), config.instrument().continuousSymbol(), trade.direction().name(),
                    trade.setupTime(), trade.entryTime(), trade.entryPrice(), trade.stopPrice(), trade.tp1Price(), trade.runnerTargetPrice(),
                    trade.modelB().exitTime(), trade.modelB().exitPrice(),
                    trade.modelA().rawR(), trade.modelB().rawR(), trade.modelA().weightedR(), trade.modelB().weightedR(),
                    multiplier(trade.modelA()), trade.instrumentId(), json.writeValueAsString(metadata));
            trades.save(saved);
        }
    }
    private BigDecimal multiplier(BacktestTradeResult trade) { return trade.rawR().signum() == 0 ? BigDecimal.ONE : trade.weightedR().divide(trade.rawR(), 4, RoundingMode.HALF_UP); }

    private JsonNode metadata(BacktestTrade trade) {
        try { return json.readTree(trade.getSetupMetadataJson()); }
        catch (Exception exception) { throw new IllegalStateException("Backtest trade metadata is invalid", exception); }
    }

    private BacktestTradeChartResponse.Overlays overlays(BacktestTrade trade, JsonNode metadata) {
        return new BacktestTradeChartResponse.Overlays(
                zone("5m IFVG", metadata, "setupZone", "setupZoneAvailableAt", "ifvgConfirmationTime"),
                zone("1m confirmation", metadata, "confirmationZone", "confirmationZoneAvailableAt", null),
                new BacktestTradeChartResponse.Level("Entry", trade.getEntryPrice(), trade.getEntryTime(), "Entry fill"),
                new BacktestTradeChartResponse.Level("Stop", trade.getStopPrice(), trade.getEntryTime(), "Initial stop"),
                new BacktestTradeChartResponse.Level("TP1", trade.getTp1Price(), trade.getEntryTime(), detail("targetR", metadata, "R")),
                new BacktestTradeChartResponse.Level("Runner", trade.getRunnerTargetPrice(), trade.getEntryTime(), runnerDetail(metadata)),
                swing("Stop swing", trade.getStopPrice(), metadata, "stopSwingTimestamp", "stopSwingConfirmedAt"),
                swing("TP1 target swing", trade.getTp1Price(), metadata, "targetSwingTimestamp", "targetSwingConfirmedAt"));
    }

    private BacktestTradeChartResponse.Facts facts(BacktestTrade trade, JsonNode metadata) {
        BigDecimal risk = trade.getEntryPrice().subtract(trade.getStopPrice()).abs();
        return new BacktestTradeChartResponse.Facts(trade.getDirection(), trade.getEntryTime(), trade.getEntryPrice(), trade.getStopPrice(), risk,
                trade.getTp1Price(), decimal(metadata, "targetR"), trade.getRunnerTargetPrice(), decimal(metadata, "runnerR"),
                text(metadata, "runnerObjectiveType"), trade.getModelAR(), trade.getModelBR(), trade.getInstrumentId(), bool(metadata, "ambiguousIntrabar"));
    }

    private BacktestTradeChartResponse.Zone zone(String label, JsonNode metadata, String rangeField, String availableField, String confirmedField) {
        String value = text(metadata, rangeField);
        if (value == null || !value.contains("-")) return null;
        String[] values = value.split("-", 2);
        try { return new BacktestTradeChartResponse.Zone(label, new BigDecimal(values[0]), new BigDecimal(values[1]), instant(metadata, availableField), confirmedField == null ? null : instant(metadata, confirmedField)); }
        catch (NumberFormatException ignored) { return null; }
    }

    private BacktestTradeChartResponse.Swing swing(String label, BigDecimal price, JsonNode metadata, String timestampField, String confirmedField) {
        Instant timestamp = instant(metadata, timestampField);
        return timestamp == null || price == null ? null : new BacktestTradeChartResponse.Swing(label, price, timestamp, instant(metadata, confirmedField));
    }

    private String runnerDetail(JsonNode metadata) {
        String type = text(metadata, "runnerObjectiveType"), timeframe = text(metadata, "runnerObjectiveTimeframe");
        return type == null ? null : timeframe == null ? type : type + " " + timeframe;
    }

    private String detail(String field, JsonNode metadata, String suffix) {
        BigDecimal value = decimal(metadata, field); return value == null ? null : value.stripTrailingZeros().toPlainString() + suffix;
    }
    private String text(JsonNode metadata, String field) { JsonNode value = metadata.path(field); return value.isMissingNode() || value.isNull() ? null : value.asText(); }
    private BigDecimal decimal(JsonNode metadata, String field) { String value = text(metadata, field); try { return value == null ? null : new BigDecimal(value); } catch (NumberFormatException ignored) { return null; } }
    private Instant instant(JsonNode metadata, String field) { String value = text(metadata, field); try { return value == null || "N/A".equalsIgnoreCase(value) ? null : Instant.parse(value); } catch (Exception ignored) { return null; } }
    private boolean bool(JsonNode metadata, String field) { return metadata.path(field).asBoolean(false); }

    static List<PriceZone> fvgObjectives(List<BacktestBar> bars, Duration timeframe, BacktestConfig config) {
        List<BacktestBar> aggregated = new TimeframeAggregationService().aggregate(bars, timeframe);
        List<PriceZone> objectives = new ArrayList<>();
        BigDecimal minimum = new BigDecimal("0.25").multiply(BigDecimal.valueOf(config.minFvgTicks()));
        for (int index = 2; index < aggregated.size(); index++) {
            BacktestBar completed = aggregated.get(index);
            new FvgDetector().detect(aggregated.get(index - 2), aggregated.get(index - 1), completed, minimum)
                    .map(zone -> new PriceZone(zone.direction(), zone.low(), zone.high(), completed.time().plus(timeframe), zone.instrumentId(), false))
                    .ifPresent(objectives::add);
        }
        return objectives;
    }
}
