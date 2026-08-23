package com.tradepilot;

import com.tradepilot.entity.Trade;
import com.tradepilot.entity.TradingAccount;
import com.tradepilot.enums.OutcomeType;
import com.tradepilot.enums.SessionType;
import com.tradepilot.performancetimeline.dto.PerformanceCategoryScores;
import com.tradepilot.performancetimeline.dto.PerformanceTimelineMetrics;
import com.tradepilot.performancetimeline.entity.ScoreConfidence;
import com.tradepilot.performancetimeline.service.PerformanceEvolutionScoreService;
import com.tradepilot.performancetimeline.service.PerformanceTimelineMetricsService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PerformanceTimelineCalculationTests {
    private final PerformanceTimelineMetricsService metricsService = new PerformanceTimelineMetricsService();
    private final PerformanceEvolutionScoreService scoreService = new PerformanceEvolutionScoreService();

    @Test
    void calculatesRMultiplesFromAccountDefaultRisk() {
        TradingAccount account = accountRisk("200.00");

        PerformanceTimelineMetrics metrics = metricsService.calculate(List.of(
                trade(1L, "LONG", "400.00", null, account),
                trade(2L, "SHORT", "-100.00", null, account)
        ), account);

        assertThat(metrics.getTotalR()).isEqualByComparingTo("1.50");
        assertThat(metrics.getAverageR()).isEqualByComparingTo("0.75");
        assertThat(metrics.getNetPnl()).isEqualByComparingTo("300.00");
    }

    @Test
    void tradeRiskOverridesAccountDefaultRisk() {
        TradingAccount account = accountRisk("200.00");

        PerformanceTimelineMetrics metrics = metricsService.calculate(List.of(
                trade(1L, "LONG", "400.00", "100.00", account)
        ), account);

        assertThat(metrics.getTotalR()).isEqualByComparingTo("4.00");
        assertThat(metrics.getAverageR()).isEqualByComparingTo("4.00");
    }

    @Test
    void missingRiskLeavesRMetricsNullInsteadOfZero() {
        TradingAccount account = accountRisk(null);

        PerformanceTimelineMetrics metrics = metricsService.calculate(List.of(
                trade(1L, "LONG", "400.00", null, account)
        ), account);

        assertThat(metrics.getTotalR()).isNull();
        assertThat(metrics.getAverageR()).isNull();
    }

    @Test
    void scoreConfidenceUsesMonthlySampleSize() {
        assertThat(scoreService.confidence(5)).isEqualTo(ScoreConfidence.LOW);
        assertThat(scoreService.confidence(10)).isEqualTo(ScoreConfidence.MEDIUM);
        assertThat(scoreService.confidence(30)).isEqualTo(ScoreConfidence.HIGH);
    }

    @Test
    void evolutionScoreStaysInRange() {
        PerformanceTimelineMetrics metrics = metricsService.calculate(List.of(
                trade(1L, "LONG", "400.00", null, accountRisk("200.00")),
                trade(2L, "SHORT", "-100.00", null, accountRisk("200.00")),
                trade(3L, "LONG", "80.00", null, accountRisk("200.00")),
                trade(4L, "SHORT", "-50.00", null, accountRisk("200.00")),
                trade(5L, "LONG", "125.00", null, accountRisk("200.00"))
        ), accountRisk("200.00"));

        PerformanceCategoryScores scores = scoreService.categoryScores(metrics);
        int evolutionScore = scoreService.evolutionScore(scores);

        assertThat(evolutionScore).isBetween(0, 100);
    }

    private TradingAccount accountRisk(String risk) {
        TradingAccount account = new TradingAccount();
        account.setDefaultRiskAmount(risk == null ? null : new BigDecimal(risk));
        return account;
    }

    private Trade trade(Long id, String side, String pnl, String risk, TradingAccount account) {
        Trade trade = new Trade();
        trade.setId(id);
        trade.setSymbol("MESU6");
        trade.setMarket("FUTURES");
        trade.setTradeDate(LocalDate.of(2026, 6, Math.toIntExact(id)));
        trade.setEntryTime(LocalDateTime.of(2026, 6, Math.toIntExact(id), 9, 30));
        trade.setExitTime(LocalDateTime.of(2026, 6, Math.toIntExact(id), 9, 45));
        trade.setSide(side);
        trade.setSession(SessionType.NY_AM);
        trade.setQuantity(1);
        trade.setPnlAmount(new BigDecimal(pnl));
        trade.setRiskAmount(risk == null ? null : new BigDecimal(risk));
        trade.setOutcome(new BigDecimal(pnl).signum() >= 0 ? OutcomeType.WIN : OutcomeType.LOSS);
        trade.setAccount(account);
        trade.setConfidence(4);
        trade.setEmotion("CALM");
        trade.setNotes("Test trade");
        trade.setCreatedAt(LocalDateTime.now());
        trade.setUpdatedAt(LocalDateTime.now());
        return trade;
    }
}
