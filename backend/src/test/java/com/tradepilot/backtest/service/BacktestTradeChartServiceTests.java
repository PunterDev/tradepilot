package com.tradepilot.backtest.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradepilot.backtest.dto.BacktestTradeChartResponse;
import com.tradepilot.backtest.entity.BacktestRun;
import com.tradepilot.backtest.entity.BacktestTrade;
import com.tradepilot.backtest.repository.BacktestRunRepository;
import com.tradepilot.backtest.repository.BacktestTradeRepository;
import com.tradepilot.entity.User;
import com.tradepilot.entitlement.FeatureEntitlementService;
import com.tradepilot.marketdata.entity.MarketBar;
import com.tradepilot.marketdata.model.MarketInstrument;
import com.tradepilot.marketdata.repository.MarketBarRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class BacktestTradeChartServiceTests {
    @Test
    void chartUsesBoundedStoredBarsForTheTradeContractAndRetainsCausalOverlays() {
        BacktestRunRepository runs = mock(BacktestRunRepository.class);
        BacktestTradeRepository trades = mock(BacktestTradeRepository.class);
        MarketBarRepository bars = mock(MarketBarRepository.class);
        BacktestService service = new BacktestService(runs, trades, mock(BacktestDataService.class), bars, new ObjectMapper(), mock(BacktestWorker.class), mock(FeatureEntitlementService.class));
        User user = User.builder().id(7L).email("chart@example.com").name("Chart").passwordHash("hash").build();
        BacktestRun run = new BacktestRun(); run.setUserId(user.getId());
        BacktestTrade trade = trade();
        when(runs.findByIdAndUserId(4L, 7L)).thenReturn(Optional.of(run));
        when(trades.findByIdAndBacktestRunId(12L, 4L)).thenReturn(Optional.of(trade));
        Instant entry = Instant.parse("2026-09-09T14:12:00Z");
        when(bars.findBySymbolAndInstrumentIdAndTimeframeAndTsEventGreaterThanEqualAndTsEventLessThanOrderByTsEventAsc(
                eq(MarketInstrument.NQ), eq(42004177L), eq("1m"), eq(entry.minusSeconds(45 * 60)), eq(Instant.parse("2026-09-09T15:00:00Z"))))
                .thenReturn(List.of(bar(entry.minusSeconds(60)), bar(entry), bar(entry.plusSeconds(60))));

        BacktestTradeChartResponse response = service.chart(user, 4L, 12L, "1m");

        assertThat(response.timezone()).isEqualTo("America/New_York");
        assertThat(response.instrument()).isEqualTo("NQ");
        assertThat(response.instrumentId()).isEqualTo(42004177L);
        assertThat(response.bars()).hasSize(3).allMatch(candle -> !candle.timestamp().isBefore(entry.minusSeconds(60)));
        assertThat(response.overlays().setupZone().availableAt()).isEqualTo(Instant.parse("2026-09-09T14:05:00Z"));
        assertThat(response.overlays().setupZone().inversionConfirmedAt()).isEqualTo(Instant.parse("2026-09-09T14:10:00Z"));
        assertThat(response.overlays().confirmationZone().availableAt()).isEqualTo(Instant.parse("2026-09-09T14:11:00Z"));
        assertThat(response.overlays().entry().price()).isEqualByComparingTo("29547.25");
        assertThat(response.overlays().stopSwing().confirmedAt()).isEqualTo(Instant.parse("2026-09-09T14:00:00Z"));
        assertThat(response.overlays().targetSwing().timestamp()).isEqualTo(Instant.parse("2026-09-09T13:49:00Z"));
    }

    @Test
    void chartAggregatesOnlyReturnedStoredOneMinuteBarsForFiveMinuteView() {
        BacktestRunRepository runs = mock(BacktestRunRepository.class);
        BacktestTradeRepository trades = mock(BacktestTradeRepository.class);
        MarketBarRepository bars = mock(MarketBarRepository.class);
        BacktestService service = new BacktestService(runs, trades, mock(BacktestDataService.class), bars, new ObjectMapper(), mock(BacktestWorker.class), mock(FeatureEntitlementService.class));
        User user = User.builder().id(7L).email("chart@example.com").name("Chart").passwordHash("hash").build();
        BacktestRun run = new BacktestRun(); run.setUserId(user.getId());
        BacktestTrade trade = trade();
        when(runs.findByIdAndUserId(4L, 7L)).thenReturn(Optional.of(run));
        when(trades.findByIdAndBacktestRunId(12L, 4L)).thenReturn(Optional.of(trade));
        Instant start = Instant.parse("2026-09-09T14:10:00Z");
        when(bars.findBySymbolAndInstrumentIdAndTimeframeAndTsEventGreaterThanEqualAndTsEventLessThanOrderByTsEventAsc(any(), anyLong(), eq("1m"), any(), any()))
                .thenReturn(List.of(bar(start), bar(start.plusSeconds(60)), bar(start.plusSeconds(120)), bar(start.plusSeconds(180)), bar(start.plusSeconds(240))));

        BacktestTradeChartResponse response = service.chart(user, 4L, 12L, "5m");

        assertThat(response.timeframe()).isEqualTo("5m");
        assertThat(response.bars()).hasSize(1);
        verify(bars).findBySymbolAndInstrumentIdAndTimeframeAndTsEventGreaterThanEqualAndTsEventLessThanOrderByTsEventAsc(eq(MarketInstrument.NQ), eq(42004177L), eq("1m"), any(), any());
    }

    @Test
    void chartAggregatesBoundedStoredBarsIntoNewYorkAlignedHourlyCandles() {
        ChartFixture fixture = fixture();
        Instant hourStart = Instant.parse("2026-09-09T13:00:00Z"); // 09:00 ET
        when(fixture.bars.findBySymbolAndInstrumentIdAndTimeframeAndTsEventGreaterThanEqualAndTsEventLessThanOrderByTsEventAsc(any(), anyLong(), eq("1m"), any(), any()))
                .thenReturn(minuteBars(hourStart, 60, 42004177L));

        BacktestTradeChartResponse response = fixture.service.chart(fixture.user, 4L, 12L, "1h");

        assertThat(response.timeframe()).isEqualTo("1h");
        assertThat(response.bars()).singleElement().satisfies(candle -> {
            assertThat(candle.timestamp()).isEqualTo(hourStart);
            assertThat(candle.open()).isEqualByComparingTo("29547");
            assertThat(candle.high()).isEqualByComparingTo("29607");
            assertThat(candle.low()).isEqualByComparingTo("29546");
            assertThat(candle.close()).isEqualByComparingTo("29606.5");
            assertThat(candle.volume()).isEqualTo(6000L);
        });
        verify(fixture.bars).findBySymbolAndInstrumentIdAndTimeframeAndTsEventGreaterThanEqualAndTsEventLessThanOrderByTsEventAsc(
                eq(MarketInstrument.NQ), eq(42004177L), eq("1m"), eq(Instant.parse("2026-09-02T04:00:00Z")), eq(Instant.parse("2026-09-09T18:30:00Z")));
    }

    @Test
    void chartAggregatesBoundedStoredBarsIntoNewYorkAlignedFourHourCandles() {
        ChartFixture fixture = fixture();
        Instant fourHourStart = Instant.parse("2026-09-09T12:00:00Z"); // 08:00 ET
        when(fixture.bars.findBySymbolAndInstrumentIdAndTimeframeAndTsEventGreaterThanEqualAndTsEventLessThanOrderByTsEventAsc(any(), anyLong(), eq("1m"), any(), any()))
                .thenReturn(minuteBars(fourHourStart, 240, 42004177L));

        BacktestTradeChartResponse response = fixture.service.chart(fixture.user, 4L, 12L, "4h");

        assertThat(response.timeframe()).isEqualTo("4h");
        assertThat(response.bars()).singleElement().satisfies(candle -> {
            assertThat(candle.timestamp()).isEqualTo(fourHourStart);
            assertThat(candle.open()).isEqualByComparingTo("29547");
            assertThat(candle.high()).isEqualByComparingTo("29787");
            assertThat(candle.low()).isEqualByComparingTo("29546");
            assertThat(candle.close()).isEqualByComparingTo("29786.5");
            assertThat(candle.volume()).isEqualTo(24000L);
        });
        verify(fixture.bars).findBySymbolAndInstrumentIdAndTimeframeAndTsEventGreaterThanEqualAndTsEventLessThanOrderByTsEventAsc(
                eq(MarketInstrument.NQ), eq(42004177L), eq("1m"), eq(Instant.parse("2026-08-19T04:00:00Z")), eq(Instant.parse("2026-09-09T22:30:00Z")));
    }

    @Test
    void chartNeverBuildsAnHourlyCandleAcrossAnInstrumentRollover() {
        ChartFixture fixture = fixture();
        Instant hourStart = Instant.parse("2026-09-09T13:00:00Z");
        List<MarketBar> mixedInstrumentBars = new ArrayList<>(minuteBars(hourStart, 30, 42004177L));
        mixedInstrumentBars.addAll(minuteBars(hourStart.plus(Duration.ofMinutes(30)), 30, 42004178L));
        when(fixture.bars.findBySymbolAndInstrumentIdAndTimeframeAndTsEventGreaterThanEqualAndTsEventLessThanOrderByTsEventAsc(any(), anyLong(), eq("1m"), any(), any()))
                .thenReturn(mixedInstrumentBars);

        assertThat(fixture.service.chart(fixture.user, 4L, 12L, "1h").bars()).isEmpty();
    }

    @Test
    void chartRejectsUnsupportedTimeframes() {
        ChartFixture fixture = fixture();

        assertThatThrownBy(() -> fixture.service.chart(fixture.user, 4L, 12L, "15m"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Chart timeframe must be 1m, 5m, 1h, or 4h.");
    }

    private ChartFixture fixture() {
        BacktestRunRepository runs = mock(BacktestRunRepository.class);
        BacktestTradeRepository trades = mock(BacktestTradeRepository.class);
        MarketBarRepository bars = mock(MarketBarRepository.class);
        BacktestService service = new BacktestService(runs, trades, mock(BacktestDataService.class), bars, new ObjectMapper(), mock(BacktestWorker.class), mock(FeatureEntitlementService.class));
        User user = User.builder().id(7L).email("chart@example.com").name("Chart").passwordHash("hash").build();
        BacktestRun run = new BacktestRun(); run.setUserId(user.getId());
        when(runs.findByIdAndUserId(4L, 7L)).thenReturn(Optional.of(run));
        when(trades.findByIdAndBacktestRunId(12L, 4L)).thenReturn(Optional.of(trade()));
        return new ChartFixture(service, bars, user);
    }

    private List<MarketBar> minuteBars(Instant start, int count, long instrumentId) {
        List<MarketBar> bars = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            Instant timestamp = start.plus(Duration.ofMinutes(index));
            BigDecimal open = new BigDecimal("29547").add(BigDecimal.valueOf(index));
            bars.add(new MarketBar(MarketInstrument.NQ, "NQ.v.0", "DATABENTO", instrumentId, "1m", timestamp,
                    open, open.add(BigDecimal.ONE), open.subtract(BigDecimal.ONE), open.add(new BigDecimal("0.5")), 100L, timestamp));
        }
        return bars;
    }

    private record ChartFixture(BacktestService service, MarketBarRepository bars, User user) { }

    private BacktestTrade trade() {
        BacktestTrade trade = new BacktestTrade();
        ReflectionTestUtils.setField(trade, "id", 12L);
        trade.fill(4L, java.time.LocalDate.parse("2026-09-09"), "NQ", "NQ.v.0", "LONG", Instant.parse("2026-09-09T14:05:00Z"),
                Instant.parse("2026-09-09T14:12:00Z"), new BigDecimal("29547.250"), new BigDecimal("29530.750"), new BigDecimal("29555.750"), new BigDecimal("29560.500"),
                Instant.parse("2026-09-09T14:30:00Z"), new BigDecimal("29558.000"), new BigDecimal("0.515152"), new BigDecimal("0.651515"), new BigDecimal("0.515152"), new BigDecimal("0.651515"), BigDecimal.ONE, 42004177L,
                "{\"setupZone\":\"29540.000-29544.000\",\"setupZoneAvailableAt\":\"2026-09-09T14:05:00Z\",\"ifvgConfirmationTime\":\"2026-09-09T14:10:00Z\",\"confirmationZone\":\"29543.750-29550.500\",\"confirmationZoneAvailableAt\":\"2026-09-09T14:11:00Z\",\"stopSwingTimestamp\":\"2026-09-09T13:55:00Z\",\"stopSwingConfirmedAt\":\"2026-09-09T14:00:00Z\",\"targetSwingTimestamp\":\"2026-09-09T13:49:00Z\",\"targetSwingConfirmedAt\":\"2026-09-09T13:54:00Z\",\"targetR\":0.515152,\"runnerR\":0.803030,\"runnerObjectiveType\":\"FVG_5M\",\"runnerObjectiveTimeframe\":\"5m\",\"ambiguousIntrabar\":false}");
        return trade;
    }

    private MarketBar bar(Instant timestamp) {
        return new MarketBar(MarketInstrument.NQ, "NQ.v.0", "DATABENTO", 42004177L, "1m", timestamp,
                new BigDecimal("29547"), new BigDecimal("29548"), new BigDecimal("29546"), new BigDecimal("29547.5"), 100L, timestamp);
    }
}
