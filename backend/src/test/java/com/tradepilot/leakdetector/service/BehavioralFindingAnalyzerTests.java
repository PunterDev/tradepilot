package com.tradepilot.leakdetector.service;

import com.tradepilot.entity.Trade;
import com.tradepilot.enums.OutcomeType;
import com.tradepilot.leakdetector.dto.EvidenceStrength;
import com.tradepilot.leakdetector.dto.LeakType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BehavioralFindingAnalyzerTests {
    @Test
    void postLossPauseGoldenFixtureCountsBothWinnersAndLosers() {
        var result = BehavioralFindingAnalyzer.postLoss(List.of(
                trade("09:45", "100"), trade("09:52", "-100"), trade("10:00", "-120"),
                trade("10:07", "-200"), trade("10:15", "150"), trade("10:24", "80")
        ));

        var simulation = result.finding().counterfactual();
        assertThat(result.finding().occurrenceCount()).isEqualTo(1);
        assertThat(result.finding().evidenceStrength()).isEqualTo(EvidenceStrength.INSUFFICIENT);
        assertThat(simulation.skippedTradeCount()).isEqualTo(2);
        assertThat(simulation.skippedLoserCount()).isEqualTo(1);
        assertThat(simulation.skippedWinnerCount()).isEqualTo(1);
        assertThat(simulation.lossesAvoided()).isEqualByComparingTo("200.00");
        assertThat(simulation.winnersForgone()).isEqualByComparingTo("150.00");
        assertThat(simulation.netHistoricalImpact()).isEqualByComparingTo("50.00");
    }

    @Test
    void continuousLossStreakCreatesOnePostLossTrigger() {
        var result = BehavioralFindingAnalyzer.postLoss(List.of(
                trade("09:00", "-10"), trade("09:05", "-10"), trade("09:10", "-10"), trade("09:15", "-50")
        ));
        assertThat(result.finding().occurrenceCount()).isEqualTo(1);
    }

    @Test
    void evidenceStrengthUsesStableOccurrenceBoundaries() {
        assertThat(BehavioralFindingAnalyzer.evidenceStrength(0)).isEqualTo(EvidenceStrength.INSUFFICIENT);
        assertThat(BehavioralFindingAnalyzer.evidenceStrength(1)).isEqualTo(EvidenceStrength.INSUFFICIENT);
        assertThat(BehavioralFindingAnalyzer.evidenceStrength(4)).isEqualTo(EvidenceStrength.INSUFFICIENT);
        assertThat(BehavioralFindingAnalyzer.evidenceStrength(5)).isEqualTo(EvidenceStrength.WEAK);
        assertThat(BehavioralFindingAnalyzer.evidenceStrength(9)).isEqualTo(EvidenceStrength.WEAK);
        assertThat(BehavioralFindingAnalyzer.evidenceStrength(10)).isEqualTo(EvidenceStrength.MODERATE);
        assertThat(BehavioralFindingAnalyzer.evidenceStrength(19)).isEqualTo(EvidenceStrength.MODERATE);
        assertThat(BehavioralFindingAnalyzer.evidenceStrength(20)).isEqualTo(EvidenceStrength.STRONG);
        assertThat(BehavioralFindingAnalyzer.evidenceStrength(1000)).isEqualTo(EvidenceStrength.STRONG);
    }

    @Test
    void negativeCounterfactualRemainsUnsupported() {
        var result = BehavioralFindingAnalyzer.postLoss(List.of(
                trade("09:45", "100"), trade("09:52", "-100"), trade("10:00", "-120"),
                trade("10:07", "-200"), trade("10:15", "350"), trade("10:24", "80")
        ));
        var simulation = result.finding().counterfactual();
        assertThat(simulation.lossesAvoided()).isEqualByComparingTo("200.00");
        assertThat(simulation.winnersForgone()).isEqualByComparingTo("350.00");
        assertThat(simulation.netHistoricalImpact()).isEqualByComparingTo("-150.00");
        assertThat(simulation.supported()).isFalse();
    }

    @Test
    void sizeEscalationRequiresLargerComparableInstrumentAfterLoss() {
        Trade loss = trade("09:00", "-10");
        loss.setQuantity(1);
        Trade larger = trade("09:05", "-50");
        larger.setQuantity(2);
        Trade differentProduct = trade("09:10", "-50");
        differentProduct.setSymbol("MNQ");
        differentProduct.setQuantity(10);
        var result = BehavioralFindingAnalyzer.sizeEscalation(List.of(loss, larger, differentProduct));
        assertThat(result.finding().occurrenceCount()).isEqualTo(1);
        assertThat(result.relatedTrades()).containsExactly(larger);
    }

    @Test
    void overtradingExcludesTheEvaluatedSessionAndUsesOnlyTradesBeyondTheHistoricalThreshold() {
        List<Trade> trades = List.of(
                trade(LocalDate.of(2026, 1, 5), "09:00", "10"), trade(LocalDate.of(2026, 1, 5), "09:05", "10"),
                trade(LocalDate.of(2026, 1, 6), "09:00", "10"), trade(LocalDate.of(2026, 1, 6), "09:05", "10"),
                trade(LocalDate.of(2026, 1, 7), "09:00", "10"), trade(LocalDate.of(2026, 1, 7), "09:05", "10"),
                trade(LocalDate.of(2026, 1, 8), "09:00", "10"), trade(LocalDate.of(2026, 1, 8), "09:05", "10"),
                trade(LocalDate.of(2026, 1, 9), "09:00", "10"), trade(LocalDate.of(2026, 1, 9), "09:05", "10"),
                trade(LocalDate.of(2026, 1, 10), "09:00", "10"), trade(LocalDate.of(2026, 1, 10), "09:05", "10"),
                trade(LocalDate.of(2026, 1, 10), "09:10", "10"), trade(LocalDate.of(2026, 1, 10), "09:15", "-100")
        );
        var result = BehavioralFindingAnalyzer.overtrading(trades);
        assertThat(result.type()).isEqualTo(LeakType.OVERTRADING);
        assertThat(result.finding().threshold()).isEqualTo(3);
        assertThat(result.relatedTrades()).hasSize(1);
    }

    @Test
    void overtradingRequiresFivePriorReferenceSessions() {
        var result = BehavioralFindingAnalyzer.overtrading(List.of(
                trade(LocalDate.of(2026, 1, 5), "09:00", "10"),
                trade(LocalDate.of(2026, 1, 6), "09:00", "10"),
                trade(LocalDate.of(2026, 1, 7), "09:00", "10"),
                trade(LocalDate.of(2026, 1, 8), "09:00", "10"),
                trade(LocalDate.of(2026, 1, 9), "09:00", "10"),
                trade(LocalDate.of(2026, 1, 9), "09:05", "-100")
        ));
        assertThat(result.finding().qualified()).isFalse();
        assertThat(result.relatedTrades()).isEmpty();
        assertThat(result.finding().threshold()).isNull();
    }

    @Test
    void overtradingUsesMathematicalMedianAndCeilingForEvenReferenceHistory() {
        List<Trade> trades = new java.util.ArrayList<>();
        int[] historicalCounts = {2, 3, 4, 5, 6, 7};
        for (int day = 0; day < historicalCounts.length; day++) {
            for (int index = 0; index < historicalCounts[day]; index++) {
                trades.add(trade(LocalDate.of(2026, 2, 2).plusDays(day), String.format("09:%02d", index), "10"));
            }
        }
        for (int index = 0; index < 8; index++) trades.add(trade(LocalDate.of(2026, 2, 8), String.format("09:%02d", index), index < 7 ? "10" : "-100"));

        var result = BehavioralFindingAnalyzer.overtrading(trades);

        assertThat(result.finding().threshold()).isEqualTo(6);
        assertThat(result.relatedTrades().stream().filter(trade -> trade.getTradeDate().equals(LocalDate.of(2026, 2, 8))).toList())
                .hasSize(2)
                .extracting(Trade::getEntryTime)
                .containsExactly(LocalDate.of(2026, 2, 8).atTime(9, 6), LocalDate.of(2026, 2, 8).atTime(9, 7));
    }

    private Trade trade(String time, String pnl) { return trade(LocalDate.of(2026, 1, 5), time, pnl); }

    private Trade trade(LocalDate date, String time, String pnl) {
        LocalDateTime entry = LocalDateTime.of(date, java.time.LocalTime.parse(time));
        return Trade.builder().symbol("NQ").market("FUTURES").tradeDate(date).entryTime(entry).exitTime(entry).side("LONG").session(com.tradepilot.enums.SessionType.NY_AM).quantity(1).pnlAmount(new BigDecimal(pnl)).outcome(new BigDecimal(pnl).signum() < 0 ? OutcomeType.LOSS : OutcomeType.WIN).build();
    }
}
