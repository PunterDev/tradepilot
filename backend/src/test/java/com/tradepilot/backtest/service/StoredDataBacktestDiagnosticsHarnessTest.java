package com.tradepilot.backtest.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradepilot.backtest.engine.*;
import com.tradepilot.backtest.model.*;
import com.tradepilot.marketdata.model.MarketInstrument;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.time.*;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

/** Opt-in, read-only multi-window validation. It never persists a run or requests provider data. */
@EnabledIfSystemProperty(named = "backtest.storedDataDiagnostics", matches = "true")
class StoredDataBacktestDiagnosticsHarnessTest {
    private static final ZoneId NEW_YORK = ZoneId.of("America/New_York");
    private static final List<Window> WINDOWS = List.of(
            window("2025-10-06", "2025-10-10"), window("2025-11-10", "2025-11-14"),
            window("2026-01-12", "2026-01-16"), window("2026-03-09", "2026-03-13"),
            window("2026-05-11", "2026-05-15"), window("2026-07-13", "2026-07-17"),
            window("2026-08-10", "2026-08-14"), window("2026-09-07", "2026-09-11"));
    private static final BigDecimal TICK = new BigDecimal("0.25");

    @Test void validatesDeterministicChronologicalNqSamplesAndWritesReviewReport() throws Exception {
        DataSourceSettings settings = DataSourceSettings.resolve();
        List<Map<String, Object>> reports = new ArrayList<>(), diagnostics = new ArrayList<>();
        Map<String, Integer> rejections = new TreeMap<>(), stages = new TreeMap<>();
        List<String> violations = new ArrayList<>();
        long totalBars = 0, totalSessions = 0;
        for (Window window : WINDOWS) {
            long loadStart = System.nanoTime();
            List<BacktestBar> bars = loadStoredBars(settings, window);
            long loadDone = System.nanoTime();
            assertThat(bars).as("stored NQ data for %s", window).isNotEmpty();
            totalBars += bars.size(); totalSessions += sessions(bars, window);
            BacktestConfig config = BacktestConfig.conservative(MarketInstrument.NQ, window.start(), window.end());
            long aggregationStart = System.nanoTime();
            PlaybookV1Strategy.Evaluation strategy = new PlaybookV1Strategy().evaluate(bars, config);
            long strategyDone = System.nanoTime();
            List<SwingDetector.Swing> swings = new SwingDetector().confirmed(bars, config.swingLeftBars(), config.swingRightBars());
            RunnerObjectiveSelector.Objectives objectives = new RunnerObjectiveSelector.Objectives(
                    BacktestService.fvgObjectives(bars, Duration.ofMinutes(5), config), BacktestService.fvgObjectives(bars, Duration.ofHours(1), config), BacktestService.fvgObjectives(bars, Duration.ofHours(4), config));
            long objectivesDone = System.nanoTime();
            TradeSimulator.Evaluation simulation = new TradeSimulator().evaluate(bars, strategy.signals(), swings, objectives, config);
            long simulationDone = System.nanoTime();
            strategy.rejectionCounts().forEach((key, value) -> rejections.merge(key, value, Integer::sum));
            simulation.rejectionCounts().forEach((key, value) -> rejections.merge(key, value, Integer::sum));
            strategy.stageCounts().forEach((key, value) -> stages.merge(key, value, Integer::sum));
            simulation.stageCounts().forEach((key, value) -> stages.merge(key, value, Integer::sum));
            for (CanonicalBacktestTrade trade : simulation.trades()) {
                List<String> tradeViolations = assertCausality(trade, config, bars);
                violations.addAll(tradeViolations.stream().map(message -> window + " trade=" + trade.entryTime() + " " + message).toList());
                diagnostics.add(tradeReport(trade, tradeViolations));
            }
            reports.add(Map.of("window", window.toString(), "bars", bars.size(), "sessions", sessions(bars, window), "candidates", strategy.signals().size(), "accepted", simulation.trades().size(),
                    "loadMs", Duration.ofNanos(loadDone - loadStart).toMillis(), "strategyMs", Duration.ofNanos(strategyDone - aggregationStart).toMillis(), "objectiveMs", Duration.ofNanos(objectivesDone - strategyDone).toMillis(), "simulationMs", Duration.ofNanos(simulationDone - objectivesDone).toMillis()));
        }
        for (String reason : List.of("OUTSIDE_SESSION", "NO_5M_CONTEXT", "NO_ZONE_TOUCH", "NO_IFVG_CONFIRMATION", "NO_DISPLACEMENT", "NO_CONFIRMATION_ZONE", "NO_RETRACE", "CHASE_FILTER", "NO_CAUSAL_STOP", "NO_VALID_SWING_TARGET", "MAX_TRADES_REACHED", "ROLLOVER_RESET")) rejections.putIfAbsent(reason, 0);
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("label", "UNVALIDATED STORED-DATA ENGINEERING DIAGNOSTICS — pending manual chart review");
        report.put("instrument", "NQ"); report.put("windows", reports); report.put("barsProcessed", totalBars); report.put("sessionsScanned", totalSessions);
        report.put("stageConversions", stages); report.put("terminalRejections", rejections); report.put("trades", diagnostics); report.put("causalityViolations", violations);
        writeReport(report, diagnostics);
        System.out.printf("STORED_DIAGNOSTICS windows=%d bars=%d sessions=%d candidates=%d accepted=%d terminalRejections=%s stageConversions=%s violations=%d%n",
                WINDOWS.size(), totalBars, totalSessions, stages.getOrDefault("CANDIDATE_SETUPS", 0), stages.getOrDefault("ACCEPTED_TRADES", 0), rejections, stages, violations.size());
        assertThat(violations).as("all accepted trades must satisfy frozen causal invariants").isEmpty();
    }

    private List<String> assertCausality(CanonicalBacktestTrade trade, BacktestConfig config, List<BacktestBar> bars) {
        Map<String, Object> m = trade.metadata(); List<String> violations = new ArrayList<>();
        Instant entry = trade.entryTime();
        require(instant(m, "setupZoneAvailableAt").compareTo(entry) <= 0, "setup zone unavailable at entry", violations);
        require(instant(m, "ifvgConfirmationTime").compareTo(entry) <= 0, "IFVG unavailable at entry", violations);
        require(instant(m, "confirmationZoneAvailableAt").isBefore(entry), "confirmation zone unavailable before entry", violations);
        require(!instant(m, "entryFillBarStart").isBefore(instant(m, "signalAvailableAt")), "entry retrace precedes signal availability", violations);
        require(instant(m, "stopSwingConfirmedAt").compareTo(entry) <= 0, "stop swing unconfirmed at entry", violations);
        require(instant(m, "targetSwingConfirmedAt").compareTo(entry) <= 0, "target swing unconfirmed at entry", violations);
        require(decimal(m, "targetR").compareTo(config.effectiveMinimumTargetR()) >= 0, "target misses minimum R", violations);
        require(((Number) m.get("targetSwingAgeTradingDays")).intValue() <= config.effectiveTargetSwingLookbackTradingDays(), "target exceeds lookback", violations);
        String created = String.valueOf(m.get("runnerObjectiveCreatedAt"));
        if (!"N/A".equals(created)) require(!Instant.parse(created).isAfter(entry), "runner objective unavailable at selection", violations);
        require(decimal(m, "runnerR").subtract(decimal(m, "targetR")).compareTo(config.effectiveMinimumRunnerAdditionalR()) >= 0 || "NONE".equals(m.get("runnerObjectiveType")), "runner misses additional R", violations);
        Object runnerAge = m.get("runnerObjectiveAgeTradingDays"); if (!(runnerAge instanceof String)) require(((Number) runnerAge).intValue() <= config.effectiveRunnerObjectiveLookbackTradingDays(), "runner exceeds lookback", violations);
        require(Boolean.TRUE.equals(m.get("rolloverSafe")), "rollover-safe flag absent", violations);
        require(bars.stream().anyMatch(bar -> bar.instrumentId() == trade.instrumentId() && bar.time().equals(entry.minus(Duration.ofMinutes(1)))), "entry contract absent from stored bars", violations);
        LocalTime time = entry.atZone(NEW_YORK).toLocalTime(); require(!time.isBefore(config.entryStart()) && time.isBefore(config.entryEnd()), "entry outside configured session", violations);
        require(tickAligned(trade.entryPrice()) && tickAligned(trade.stopPrice()) && tickAligned(trade.tp1Price())
                && tickAligned(trade.runnerTargetPrice()) && tickAligned(trade.modelA().exitPrice())
                && tickAligned(trade.modelB().exitPrice()), "price is not aligned to NQ tick size", violations);
        return violations;
    }

    private Map<String, Object> tradeReport(CanonicalBacktestTrade trade, List<String> violations) {
        Map<String, Object> m = trade.metadata(), output = new LinkedHashMap<>();
        output.put("date", trade.tradeDate().toString()); output.put("entryTimeET", trade.entryTime().atZone(NEW_YORK).toString()); output.put("direction", trade.direction().name()); output.put("entry", trade.entryPrice()); output.put("stop", trade.stopPrice());
        output.put("riskPoints", trade.entryPrice().subtract(trade.stopPrice()).abs()); output.put("riskTicks", trade.entryPrice().subtract(trade.stopPrice()).abs().divide(TICK)); output.put("tp1", trade.tp1Price()); output.put("tp1R", m.get("targetR")); output.put("tp1AgeTradingDays", m.get("targetSwingAgeTradingDays"));
        output.put("runner", trade.runnerTargetPrice()); output.put("runnerR", m.get("runnerR")); output.put("runnerType", m.get("runnerObjectiveType")); output.put("runnerTimeframe", m.get("runnerObjectiveTimeframe")); output.put("runnerAgeTradingDays", m.get("runnerObjectiveAgeTradingDays")); output.put("modelAR", trade.modelA().rawR()); output.put("modelBR", trade.modelB().rawR()); output.put("instrumentId", trade.instrumentId());
        output.put("setupType", m.get("setupZoneDirection")); output.put("ifvgTimestamp", m.get("ifvgConfirmationTime")); output.put("displacementTimestamp", m.get("signalAvailableAt")); output.put("confirmationZoneTimestamp", m.get("confirmationZoneAvailableAt")); output.put("entryRetraceTimestamp", m.get("entryFillBarStart")); output.put("stopSwingTimestamp", m.get("stopSwingTimestamp")); output.put("stopSwingConfirmedAt", m.get("stopSwingConfirmedAt")); output.put("targetSwingTimestamp", m.get("targetSwingTimestamp")); output.put("targetSwingConfirmedAt", m.get("targetSwingConfirmedAt")); output.put("ambiguous", trade.ambiguousIntrabar()); output.put("setupMetadata", m); output.put("causalityStatus", violations.isEmpty() ? "PASS" : "FAIL"); output.put("violations", violations); return output;
    }

    private List<BacktestBar> loadStoredBars(DataSourceSettings settings, Window window) throws Exception { Instant start = window.start().minusDays(28).atStartOfDay(ZoneOffset.UTC).toInstant(), end = window.end().plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant(); List<BacktestBar> result = new ArrayList<>(); try (Connection connection = DriverManager.getConnection(settings.url(), settings.username(), settings.password())) { connection.setReadOnly(true); try (PreparedStatement statement = connection.prepareStatement("select instrument_id, ts_event, open, high, low, close, volume from market_bars where symbol = ? and timeframe = ? and ts_event >= ? and ts_event < ? order by ts_event asc")) { statement.setString(1, "NQ"); statement.setString(2, "1m"); statement.setTimestamp(3, Timestamp.from(start)); statement.setTimestamp(4, Timestamp.from(end)); try (ResultSet rows = statement.executeQuery()) { while (rows.next()) result.add(new BacktestBar(rows.getTimestamp("ts_event").toInstant(), rows.getLong("instrument_id"), rows.getBigDecimal("open"), rows.getBigDecimal("high"), rows.getBigDecimal("low"), rows.getBigDecimal("close"), rows.getLong("volume"))); } } } return result; }
    private long sessions(List<BacktestBar> bars, Window window) { return bars.stream().map(bar -> bar.time().atZone(NEW_YORK).toLocalDate()).filter(day -> !day.isBefore(window.start()) && !day.isAfter(window.end())).distinct().count(); }
    private void writeReport(Map<String, Object> report, List<Map<String, Object>> trades) throws Exception { Path directory = Path.of("target", "backtest-diagnostics"); Files.createDirectories(directory); ObjectMapper json = new ObjectMapper().findAndRegisterModules(); Files.writeString(directory.resolve("nq-sampled-validation.json"), json.writerWithDefaultPrettyPrinter().writeValueAsString(report)); StringBuilder csv = new StringBuilder("date,entryTimeET,direction,entry,stop,riskPoints,tp1,tp1R,runner,runnerR,runnerType,modelAR,modelBR,instrumentId,causalityStatus\n"); for (Map<String, Object> trade : trades) csv.append(String.join(",", List.of("date","entryTimeET","direction","entry","stop","riskPoints","tp1","tp1R","runner","runnerR","runnerType","modelAR","modelBR","instrumentId","causalityStatus").stream().map(key -> String.valueOf(trade.get(key))).toList())).append('\n'); Files.writeString(directory.resolve("nq-sampled-review-report.csv"), csv.toString()); }
    private static Window window(String start, String end) { return new Window(LocalDate.parse(start), LocalDate.parse(end)); }
    private static Instant instant(Map<String, Object> m, String key) { return Instant.parse(String.valueOf(m.get(key))); }
    private static BigDecimal decimal(Map<String, Object> m, String key) { return new BigDecimal(String.valueOf(m.get(key))); }
    private static boolean tickAligned(BigDecimal price) { return price.remainder(TICK).compareTo(BigDecimal.ZERO) == 0; }
    private static void require(boolean condition, String message, List<String> violations) { if (!condition) violations.add(message); }
    private record Window(LocalDate start, LocalDate end) { }
    private record DataSourceSettings(String url, String username, String password) { static DataSourceSettings resolve() throws Exception { String yaml = Files.readString(Path.of("src/main/resources/application-dev.yml")); return new DataSourceSettings(value("DB_URL", "DATABASE_URL", fallback(yaml, "url")), value("DB_USERNAME", "DATABASE_USERNAME", fallback(yaml, "username")), value("DB_PASSWORD", "DATABASE_PASSWORD", fallback(yaml, "password"))); } private static String value(String primary, String secondary, String fallback) { String first = System.getenv(primary); if (first != null && !first.isBlank()) return first; String second = System.getenv(secondary); return second != null && !second.isBlank() ? second : fallback; } private static String fallback(String yaml, String name) { java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("(?m)^\\s*" + name + ":\\s*\\$\\{[^:]+:\\$\\{[^:]+:(.*)}}\\s*$").matcher(yaml); if (!matcher.find()) throw new IllegalStateException("Missing local datasource " + name); return matcher.group(1).trim(); } }
}
