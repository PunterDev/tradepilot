package com.tradepilot.backtest.service;

import com.tradepilot.backtest.engine.*;
import com.tradepilot.backtest.model.*;
import com.tradepilot.marketdata.model.MarketInstrument;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import java.math.BigDecimal;
import java.nio.file.*;
import java.sql.*;
import java.time.*;
import java.util.*;
import static org.assertj.core.api.Assertions.assertThat;

/** Explicit, read-only JDBC validation runner. It deliberately does not bootstrap Spring or persist a run. */
@EnabledIfSystemProperty(named = "backtest.storedDataValidation", matches = "true")
class StoredDataBacktestValidationHarnessTest {
    private static final ZoneId NEW_YORK = ZoneId.of("America/New_York");
    private static final LocalDate START = LocalDate.of(2026, 9, 7);
    private static final LocalDate END = LocalDate.of(2026, 9, 10);

    @Test void runsTheConservativeShortNqSampleAgainstStoredBarsOnly() throws Exception {
        BacktestConfig config = BacktestConfig.conservative(MarketInstrument.NQ, START, END);
        Instant started = Instant.now();
        List<BacktestBar> bars = loadStoredBars();
        assertThat(bars).as("stored NQ 1m bars").isNotEmpty();
        System.out.printf("DB_CONNECTION=SUCCESS instrument=NQ rows=%d first=%s last=%s instrumentIds=%d requested=%s..%s warmupStart=%s%n",
                bars.size(), bars.get(0).time(), bars.get(bars.size() - 1).time(), bars.stream().map(BacktestBar::instrumentId).distinct().count(), START, END, bars.get(0).time());

        PlaybookV1Strategy.Evaluation strategy = new PlaybookV1Strategy().evaluate(bars, config);
        List<SwingDetector.Swing> swings = new SwingDetector().confirmed(bars, config.swingLeftBars(), config.swingRightBars());
        RunnerObjectiveSelector.Objectives objectives = new RunnerObjectiveSelector.Objectives(
                BacktestService.fvgObjectives(bars, Duration.ofMinutes(5), config),
                BacktestService.fvgObjectives(bars, Duration.ofHours(1), config),
                BacktestService.fvgObjectives(bars, Duration.ofHours(4), config));
        TradeSimulator.Evaluation simulation = new TradeSimulator().evaluate(bars, strategy.signals(), swings, objectives, config);
        Map<String,Integer> rejections = new TreeMap<>(strategy.rejectionCounts());
        simulation.rejectionCounts().forEach((reason,count) -> rejections.merge(reason,count,Integer::sum));
        simulation.trades().forEach(this::assertCausal);

        System.out.println("=== STORED DATA BACKTEST VALIDATION ===");
        System.out.printf("Instrument=NQ Range=%s..%s Bars=%d RuntimeMs=%d Sessions=4 Candidates=%d Accepted=%d Rejected=%d%n",
                START, END, bars.size(), Duration.between(started, Instant.now()).toMillis(), strategy.signals().size(), simulation.trades().size(), rejections.values().stream().mapToInt(Integer::intValue).sum());
        System.out.println("REJECTIONS=" + rejections);
        for (int index = 0; index < simulation.trades().size(); index++) printTrade(index + 1, simulation.trades().get(index));
    }

    private List<BacktestBar> loadStoredBars() throws Exception {
        DataSourceSettings settings = DataSourceSettings.resolve();
        Instant warmupStart = START.minusDays(28).atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant endExclusive = END.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        List<BacktestBar> bars = new ArrayList<>();
        try (Connection connection = DriverManager.getConnection(settings.url(), settings.username(), settings.password())) {
            connection.setReadOnly(true);
            try (PreparedStatement statement = connection.prepareStatement("select instrument_id, ts_event, open, high, low, close, volume from market_bars where symbol = ? and timeframe = ? and ts_event >= ? and ts_event < ? order by ts_event asc")) {
                statement.setString(1, "NQ"); statement.setString(2, "1m");
                statement.setTimestamp(3, Timestamp.from(warmupStart)); statement.setTimestamp(4, Timestamp.from(endExclusive));
                try (ResultSet rows = statement.executeQuery()) {
                    while (rows.next()) bars.add(new BacktestBar(rows.getTimestamp("ts_event").toInstant(), rows.getLong("instrument_id"), rows.getBigDecimal("open"), rows.getBigDecimal("high"), rows.getBigDecimal("low"), rows.getBigDecimal("close"), rows.getLong("volume")));
                }
            }
        }
        return bars;
    }

    private void assertCausal(CanonicalBacktestTrade trade) {
        assertThat(trade.setupTime()).isBeforeOrEqualTo(trade.entryTime());
        assertThat(Instant.parse(String.valueOf(trade.metadata().get("setupZoneAvailableAt")))).isBeforeOrEqualTo(trade.entryTime());
        assertThat(Instant.parse(String.valueOf(trade.metadata().get("ifvgConfirmationTime")))).isBeforeOrEqualTo(trade.entryTime());
        assertThat(Instant.parse(String.valueOf(trade.metadata().get("confirmationZoneAvailableAt")))).isBefore(trade.entryTime());
        assertThat(Instant.parse(String.valueOf(trade.metadata().get("stopSwingConfirmedAt")))).isBeforeOrEqualTo(trade.entryTime());
        assertThat(Instant.parse(String.valueOf(trade.metadata().get("targetSwingConfirmedAt")))).isBeforeOrEqualTo(trade.entryTime());
        String runnerCreated = String.valueOf(trade.metadata().get("runnerObjectiveCreatedAt"));
        if (!"N/A".equals(runnerCreated)) assertThat(Instant.parse(runnerCreated)).isBeforeOrEqualTo(trade.entryTime());
        LocalTime entryTime = trade.entryTime().atZone(NEW_YORK).toLocalTime();
        assertThat(!entryTime.isBefore(LocalTime.of(9,45)) && entryTime.isBefore(LocalTime.of(11,0))).isTrue();
        BigDecimal tick = MarketInstrument.NQ.tickSize();
        assertThat(tickAligned(trade.entryPrice(), tick) && tickAligned(trade.stopPrice(), tick)
                && tickAligned(trade.tp1Price(), tick) && tickAligned(trade.runnerTargetPrice(), tick)
                && tickAligned(trade.modelA().exitPrice(), tick) && tickAligned(trade.modelB().exitPrice(), tick))
                .as("all modeled prices must be executable: entry=%s stop=%s tp1=%s runner=%s modelAExit=%s modelBExit=%s",
                        trade.entryPrice(), trade.stopPrice(), trade.tp1Price(), trade.runnerTargetPrice(),
                        trade.modelA().exitPrice(), trade.modelB().exitPrice())
                .isTrue();
    }

    private boolean tickAligned(BigDecimal price, BigDecimal tick) {
        return price.remainder(tick).stripTrailingZeros().signum() == 0;
    }

    private void printTrade(int number, CanonicalBacktestTrade trade) {
        Map<String,Object> m = trade.metadata();
        System.out.printf("TRADE #%d date=%s entryET=%s entryUTC=%s direction=%s entry=%s stop=%s risk=%s target=%s targetR=%s targetSwing=%s targetConfirmed=%s targetAgeTradingDays=%s minimumTargetR=%s runner=%s runnerR=%s runnerType=%s runnerTf=%s runnerCreated=%s runnerAgeTradingDays=%s minimumRunnerAdditionalR=%s runnerBoundary=%s modelAExit=%s@%s rawA=%s weightedA=%s outcomeA=%s modelBExit=%s@%s rawB=%s weightedB=%s outcomeB=%s ambiguous=%s instrumentId=%d%n",
                number, trade.tradeDate(), trade.entryTime().atZone(NEW_YORK), trade.entryTime(), trade.direction(), trade.entryPrice(), trade.stopPrice(), trade.entryPrice().subtract(trade.stopPrice()).abs(),
                trade.tp1Price(), m.get("targetR"), m.get("targetSwingTimestamp"), m.get("targetSwingConfirmedAt"), m.get("targetSwingAgeTradingDays"), m.get("minimumTargetR"), trade.runnerTargetPrice(), m.get("runnerR"), m.get("runnerObjectiveType"), m.get("runnerObjectiveTimeframe"), m.get("runnerObjectiveCreatedAt"), m.get("runnerObjectiveAgeTradingDays"), m.get("minimumRunnerAdditionalR"), m.get("runnerFvgBoundary"),
                trade.modelA().exitPrice(), trade.modelA().exitTime(), trade.modelA().rawR(), trade.modelA().weightedR(), trade.modelA().outcome(), trade.modelB().exitPrice(), trade.modelB().exitTime(), trade.modelB().rawR(), trade.modelB().weightedR(), trade.modelB().outcome(), trade.ambiguousIntrabar(), trade.instrumentId());
    }

    private record DataSourceSettings(String url, String username, String password) {
        static DataSourceSettings resolve() throws Exception {
            String yaml = Files.readString(Path.of("src/main/resources/application-dev.yml"));
            return new DataSourceSettings(value("DB_URL", "DATABASE_URL", fallback(yaml, "url")), value("DB_USERNAME", "DATABASE_USERNAME", fallback(yaml, "username")), value("DB_PASSWORD", "DATABASE_PASSWORD", fallback(yaml, "password")));
        }
        private static String value(String primary, String secondary, String fallback) { String first=System.getenv(primary); if(first!=null&&!first.isBlank())return first; String second=System.getenv(secondary); return second!=null&&!second.isBlank()?second:fallback; }
        private static String fallback(String yaml, String name) { java.util.regex.Matcher matcher=java.util.regex.Pattern.compile("(?m)^\\s*"+name+":\\s*\\$\\{[^:]+:\\$\\{[^:]+:(.*)}}\\s*$").matcher(yaml); if(!matcher.find())throw new IllegalStateException("Missing local datasource "+name); return matcher.group(1).trim(); }
    }
}
