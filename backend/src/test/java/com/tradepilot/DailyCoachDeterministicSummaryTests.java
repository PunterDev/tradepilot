package com.tradepilot;

import com.tradepilot.coach.dto.CoachDeterministicSummary;
import com.tradepilot.coach.entity.CoachSnapshotType;
import com.tradepilot.coach.service.CoachAnalyticsAggregator;
import com.tradepilot.entity.Trade;
import com.tradepilot.enums.OutcomeType;
import com.tradepilot.enums.SessionType;
import com.tradepilot.repository.TradeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DailyCoachDeterministicSummaryTests {

    @Mock
    TradeRepository tradeRepository;

    @Test
    void oneLongLosingTradeProducesSelectedDayLongFeedbackWithoutShortClaims() {
        LocalDate date = LocalDate.of(2026, 6, 22);
        when(tradeRepository.findByUser_IdAndAccount_IdAndTradeDateBetween(1L, 6L, date, date))
                .thenReturn(List.of(trade("MESU6", "LONG", date, new BigDecimal("-76.25"))));

        CoachDeterministicSummary summary = aggregator().aggregate(1L, 6L, CoachSnapshotType.DAILY, date, date);

        assertThat(summary.getLongTradeCount()).isEqualTo(1);
        assertThat(summary.getShortTradeCount()).isZero();
        assertThat(summary.getLongPnl()).isEqualByComparingTo(new BigDecimal("-76.25"));
        assertThat(summary.getSelectedDaySummary()).contains("MESU6 LONG");
        assertThat(summary.getSelectedDaySummary()).contains("One trade, -$76.25 net");
        assertThat(summary.getPrimaryIssueToday()).contains("not an overtrading day");
        assertThat(summary.getSelectedDaySummary()).doesNotContain("SHORT");
        assertThat(summary.getPrimaryIssueToday()).doesNotContain("SHORT");
    }

    @Test
    void oneTradeDayFocusesOnExecutionReviewAndLimitedData() {
        LocalDate date = LocalDate.of(2026, 6, 22);
        when(tradeRepository.findByUser_IdAndAccount_IdAndTradeDateBetween(1L, 6L, date, date))
                .thenReturn(List.of(trade("MESU6", "LONG", date, new BigDecimal("-76.25"))));

        CoachDeterministicSummary summary = aggregator().aggregate(1L, 6L, CoachSnapshotType.DAILY, date, date);

        assertThat(summary.getPrimaryIssueToday()).contains("confirmation", "predefined stop", "realistic target");
        assertThat(summary.getSetupDirectionReview()).contains("The LONG idea failed", "valid bias", "clean location", "realistic target");
        assertThat(summary.getDataQuality()).isEqualTo("Limited Data");
        assertThat(summary.getDataQualityReason())
                .contains("only one trade was taken")
                .contains("setup/emotion notes were not available")
                .contains("execution data");
    }

    @Test
    void oneLongWinningTradeDoesNotSayLongIdeaFailed() {
        LocalDate date = LocalDate.of(2026, 6, 23);
        when(tradeRepository.findByUser_IdAndAccount_IdAndTradeDateBetween(1L, 6L, date, date))
                .thenReturn(List.of(trade("MESU6", "LONG", date, new BigDecimal("162.50"))));

        CoachDeterministicSummary summary = aggregator().aggregate(1L, 6L, CoachSnapshotType.DAILY, date, date);

        assertThat(summary.getSelectedDaySummary()).contains("MESU6 LONG");
        assertThat(summary.getSelectedDaySummary()).contains("One trade, $162.50 net");
        assertThat(summary.getSetupDirectionReview())
                .contains("The LONG trade was profitable")
                .contains("clear thesis")
                .doesNotContain("LONG idea failed")
                .doesNotContain("long failed");
        assertThat(summary.getDataQualityReason()).contains("Limited confidence: only one trade was taken");
    }

    @Test
    void oneShortWinningTradeDoesNotSayShortIdeaFailed() {
        LocalDate date = LocalDate.of(2026, 6, 24);
        when(tradeRepository.findByUser_IdAndAccount_IdAndTradeDateBetween(1L, 6L, date, date))
                .thenReturn(List.of(trade("MESU6", "SHORT", date, new BigDecimal("162.50"))));

        CoachDeterministicSummary summary = aggregator().aggregate(1L, 6L, CoachSnapshotType.DAILY, date, date);

        assertThat(summary.getSelectedDaySummary()).contains("MESU6 SHORT");
        assertThat(summary.getSetupDirectionReview())
                .contains("The SHORT trade was profitable")
                .doesNotContain("SHORT idea failed")
                .doesNotContain("short failed");
        assertThat(summary.getDataQualityReason()).contains("Limited confidence: only one trade was taken");
    }

    @Test
    void missingJournalMetadataIsReportedHonestly() {
        LocalDate date = LocalDate.of(2026, 6, 22);
        when(tradeRepository.findByUser_IdAndAccount_IdAndTradeDateBetween(1L, 6L, date, date))
                .thenReturn(List.of(trade("MESU6", "LONG", date, new BigDecimal("-76.25"))));

        CoachDeterministicSummary summary = aggregator().aggregate(1L, 6L, CoachSnapshotType.DAILY, date, date);

        assertThat(summary.getDataQualityReason())
                .contains("setup/emotion notes were not available")
                .doesNotContain("emotions were controlled");
    }

    @Test
    void unknownDirectionProducesDataQualityWarningWithoutGuessing() {
        LocalDate date = LocalDate.of(2026, 6, 22);
        when(tradeRepository.findByUser_IdAndAccount_IdAndTradeDateBetween(1L, 6L, date, date))
                .thenReturn(List.of(trade("MESU6", "UNKNOWN", date, new BigDecimal("-76.25"))));

        CoachDeterministicSummary summary = aggregator().aggregate(1L, 6L, CoachSnapshotType.DAILY, date, date);

        assertThat(summary.getUnknownTradeCount()).isEqualTo(1);
        assertThat(summary.getSelectedDaySummary()).contains("MESU6 UNKNOWN");
        assertThat(summary.getSetupDirectionReview()).contains("Direction was unknown");
        assertThat(summary.getDataQualityReason()).contains("direction was UNKNOWN");
        assertThat(summary.getLongTradeCount()).isZero();
        assertThat(summary.getShortTradeCount()).isZero();
    }

    private CoachAnalyticsAggregator aggregator() {
        return new CoachAnalyticsAggregator(tradeRepository);
    }

    private Trade trade(String symbol, String side, LocalDate date, BigDecimal pnl) {
        return Trade.builder()
                .symbol(symbol)
                .market("FUTURES")
                .tradeDate(date)
                .entryTime(LocalDateTime.of(date.getYear(), date.getMonth(), date.getDayOfMonth(), 9, 30))
                .exitTime(LocalDateTime.of(date.getYear(), date.getMonth(), date.getDayOfMonth(), 9, 45))
                .side(side)
                .session(SessionType.NY_AM)
                .entryPrice(new BigDecimal("5485.00"))
                .exitPrice(new BigDecimal("5477.50"))
                .quantity(1)
                .pnlAmount(pnl)
                .outcome(pnl.compareTo(BigDecimal.ZERO) < 0 ? OutcomeType.LOSS : OutcomeType.WIN)
                .build();
    }
}
