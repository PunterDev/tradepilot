package com.tradepilot.manualbacktest.service;

import com.tradepilot.backtest.model.BacktestBar;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import static org.assertj.core.api.Assertions.assertThat;

class ReplayAggregationServiceTests {
    private final ReplayAggregationService aggregation = new ReplayAggregationService();

    @Test void buildsOnlyTheRevealedPartialFiveMinuteCandle() {
        List<ReplayAggregationService.ReplayBar> result = aggregation.aggregate(bars("2026-09-11T14:10:00Z", 3, 42L), Duration.ofMinutes(5));
        assertThat(result).hasSize(1);
        assertThat(result.get(0).complete()).isFalse();
        assertThat(result.get(0).bar().time()).isEqualTo(Instant.parse("2026-09-11T14:10:00Z"));
        assertThat(result.get(0).bar().close()).isEqualByComparingTo("103");
        assertThat(result.get(0).bar().high()).isEqualByComparingTo("102.5");
    }

    @Test void respectsNewYorkHourAndFourHourBoundaries() {
        List<BacktestBar> bars = bars("2026-09-11T13:58:00Z", 3, 42L); // 09:58-10:00 ET
        assertThat(aggregation.aggregate(bars, Duration.ofHours(1)).get(0).bar().time()).isEqualTo(Instant.parse("2026-09-11T13:00:00Z"));
        assertThat(aggregation.aggregate(bars, Duration.ofHours(4)).get(0).bar().time()).isEqualTo(Instant.parse("2026-09-11T12:00:00Z"));
    }

    @Test void neverJoinsDifferentInstrumentIdsIntoOneReplayCandle() {
        List<BacktestBar> bars = new ArrayList<>(bars("2026-09-11T14:10:00Z", 2, 42L));
        bars.addAll(bars("2026-09-11T14:12:00Z", 1, 99L));
        assertThat(aggregation.aggregate(bars, Duration.ofMinutes(5))).isEmpty();
    }

    private List<BacktestBar> bars(String start, int count, Long instrumentId) {
        Instant first = Instant.parse(start); List<BacktestBar> result = new ArrayList<>();
        for (int index=0; index<count; index++) { BigDecimal open=BigDecimal.valueOf(100L+index); result.add(new BacktestBar(first.plus(Duration.ofMinutes(index)),instrumentId,open,open.add(new BigDecimal(".5")),open.subtract(new BigDecimal(".5")),open.add(BigDecimal.ONE),100L)); }
        return result;
    }
}
