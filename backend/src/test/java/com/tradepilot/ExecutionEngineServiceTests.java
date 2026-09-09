package com.tradepilot;

import com.tradepilot.billing.Feature;
import com.tradepilot.billing.FeatureAccessService;
import com.tradepilot.config.FeatureFlagService;
import com.tradepilot.entity.Trade;
import com.tradepilot.entity.TradeRuleViolation;
import com.tradepilot.entity.TradingAccount;
import com.tradepilot.entity.User;
import com.tradepilot.enums.OutcomeType;
import com.tradepilot.enums.SessionType;
import com.tradepilot.enums.SetupType;
import com.tradepilot.exception.ResourceNotFoundException;
import com.tradepilot.execution.config.ExecutionEngineProperties;
import com.tradepilot.execution.dto.ExecutionClassification;
import com.tradepilot.execution.dto.LossResponseClassification;
import com.tradepilot.execution.dto.SampleConfidenceLabel;
import com.tradepilot.execution.service.ExecutionEngineService;
import com.tradepilot.execution.service.PreAnalysisExecutionComparisonService;
import com.tradepilot.repository.TradeRepository;
import com.tradepilot.repository.TradeRuleViolationRepository;
import com.tradepilot.repository.TradingAccountRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class ExecutionEngineServiceTests {
    private final ExecutionEngineProperties properties = new ExecutionEngineProperties();
    private final FeatureFlagService featureFlagService = mock(FeatureFlagService.class);
    private final FeatureAccessService featureAccessService = mock(FeatureAccessService.class);
    private final TradeRepository tradeRepository = mock(TradeRepository.class);
    private final TradeRuleViolationRepository violationRepository = mock(TradeRuleViolationRepository.class);
    private final TradingAccountRepository accountRepository = mock(TradingAccountRepository.class);
    private final PreAnalysisExecutionComparisonService preAnalysisComparisonService = mock(PreAnalysisExecutionComparisonService.class);
    private final ExecutionEngineService service = new ExecutionEngineService(properties, featureFlagService, featureAccessService, tradeRepository, violationRepository, accountRepository, preAnalysisComparisonService);

    @Test
    void profitableHighExecutionClassifiesCleanWin() {
        var score = service.scoreTrade(trade(1L, BigDecimal.valueOf(2), 1, 100, 5, 5, true), List.of());

        assertThat(score.executionScore()).isGreaterThanOrEqualTo(90);
        assertThat(score.evidenceCoveragePercent()).isEqualTo(100);
        assertThat(score.evidenceConfidence().name()).isEqualTo("HIGH");
        assertThat(score.classification()).isEqualTo(ExecutionClassification.CLEAN_WIN);
    }

    @Test
    void losingHighExecutionClassifiesCleanLoss() {
        var score = service.scoreTrade(trade(1L, BigDecimal.valueOf(-1), 1, 100, 5, 5, true), List.of());

        assertThat(score.classification()).isEqualTo(ExecutionClassification.CLEAN_LOSS);
        assertThat(score.processOutcomeMessage()).contains("normal losing outcome");
    }

    @Test
    void profitablePoorExecutionClassifiesUndisciplinedWin() {
        var score = service.scoreTrade(trade(1L, BigDecimal.valueOf(2), 1, 100, 1, 1, false), List.of(violation(1L, "RISK", "HIGH")));

        assertThat(score.classification()).isEqualTo(ExecutionClassification.UNDISCIPLINED_WIN);
        assertThat(score.processOutcomeMessage()).contains("profitable");
        assertThat(score.processOutcomeMessage()).contains("violated your process");
    }

    @Test
    void losingPoorExecutionClassifiesUndisciplinedLoss() {
        var score = service.scoreTrade(trade(1L, BigDecimal.valueOf(-1), 1, 100, 1, 1, false), List.of(violation(1L, "ENTRY", "HIGH")));

        assertThat(score.classification()).isEqualTo(ExecutionClassification.UNDISCIPLINED_LOSS);
    }

    @Test
    void unavailableCriteriaDoNotReduceNormalizedScore() {
        Trade trade = trade(1L, BigDecimal.valueOf(-1), null, null, null, null, null);
        trade.setSetup(null);
        trade.setSession(null);

        var score = service.scoreTrade(trade, List.of(violation(1L, "RISK", "LOW")));

        assertThat(score.availablePoints()).isEqualTo(25);
        assertThat(score.evidenceCoveragePercent()).isEqualTo(25);
        assertThat(score.executionScore()).isEqualTo(80);
        assertThat(score.components().get("setupAdherence").evaluated()).isFalse();
    }

    @Test
    void rapidReentryAndIncreasedSizeAfterLossAreDetected() {
        Trade first = trade(1L, BigDecimal.valueOf(-1), 1, 100, 5, 5, true);
        first.setEntryTime(LocalDateTime.parse("2026-06-22T09:30:00"));
        first.setExitTime(LocalDateTime.parse("2026-06-22T09:35:00"));
        Trade next = trade(2L, BigDecimal.valueOf(-1), 2, 150, 2, 2, false);
        next.setEntryTime(LocalDateTime.parse("2026-06-22T09:38:00"));
        next.setExitTime(LocalDateTime.parse("2026-06-22T09:45:00"));

        var response = service.lossResponse(first, List.of(first, next), java.util.Map.of(
                1L, List.of(),
                2L, List.of(violation(2L, "ENTRY", "HIGH"))
        ));

        assertThat(response.classification()).isIn(LossResponseClassification.ELEVATED_RISK, LossResponseClassification.UNSTABLE);
        assertThat(response.flags()).contains("FAST_REENTRY", "RISK_INCREASE", "SIZE_INCREASE", "LOWER_EXECUTION_SCORE", "NEW_RULE_VIOLATION");
    }

    @Test
    void stablePostLossBehaviorIsNotCalledRevengeTrading() {
        Trade first = trade(1L, BigDecimal.valueOf(-1), 1, 100, 5, 5, true);
        first.setEntryTime(LocalDateTime.parse("2026-06-22T09:30:00"));
        first.setExitTime(LocalDateTime.parse("2026-06-22T09:35:00"));
        Trade next = trade(2L, BigDecimal.valueOf(1), 1, 100, 5, 5, true);
        next.setEntryTime(LocalDateTime.parse("2026-06-22T10:05:00"));
        next.setExitTime(LocalDateTime.parse("2026-06-22T10:20:00"));

        var response = service.lossResponse(first, List.of(first, next), java.util.Map.of(1L, List.of(), 2L, List.of()));

        assertThat(response.classification()).isEqualTo(LossResponseClassification.EXCELLENT);
        assertThat(response.flags()).isEmpty();
        assertThat(response.message()).doesNotContainIgnoringCase("revenge");
    }

    @Test
    void pressurePointIsNotShownBelowMinimumSample() {
        User user = user(1L);
        TradingAccount account = account(10L, user);
        when(featureFlagService.isEnabled(Feature.PERFORMANCE_TIMELINE)).thenReturn(true);
        when(accountRepository.findByIdAndUser_Id(10L, 1L)).thenReturn(Optional.of(account));
        when(tradeRepository.findByUser_IdAndAccount_IdAndTradeDateBetweenOrderByTradeDateAscEntryTimeAscIdAsc(eq(1L), eq(10L), any(), any()))
                .thenReturn(List.of(trade(1L, BigDecimal.valueOf(-1), 1, 100, 5, 5, true)));

        var pressure = service.pressurePoint(user, 10L);

        assertThat(pressure.detected()).isFalse();
        assertThat(pressure.message()).contains("Insufficient sample");
    }

    @Test
    void setupStatisticsCalculateSampleConfidence() {
        User user = user(1L);
        TradingAccount account = account(10L, user);
        List<Trade> trades = java.util.stream.LongStream.rangeClosed(1, 12)
                .mapToObj(id -> trade(id, id % 2 == 0 ? BigDecimal.ONE : BigDecimal.valueOf(-1), 1, 100, 5, 5, true))
                .toList();
        when(featureFlagService.isEnabled(Feature.PERFORMANCE_TIMELINE)).thenReturn(true);
        when(accountRepository.findByIdAndUser_Id(10L, 1L)).thenReturn(Optional.of(account));
        when(tradeRepository.findByUser_IdAndAccount_IdAndTradeDateBetweenOrderByTradeDateAscEntryTimeAscIdAsc(eq(1L), eq(10L), any(), any()))
                .thenReturn(trades);
        when(violationRepository.findForTradingDna(eq(1L), eq(10L), any(), any())).thenReturn(List.of());

        var stats = service.setupStats(user, 10L);

        assertThat(stats).hasSize(1);
        assertThat(stats.get(0).sampleConfidence()).isEqualTo(SampleConfidenceLabel.EARLY);
        assertThat(stats.get(0).averageExecutionScore()).isEqualByComparingTo("100.00");
    }

    @Test
    void weeklyFocusPrioritizesUndisciplinedWinsWhenNoPatternMeetsThreshold() {
        User user = user(1L);
        TradingAccount account = account(10L, user);
        Trade win = trade(1L, BigDecimal.valueOf(2), 1, 100, 1, 1, false);
        when(featureFlagService.isEnabled(Feature.PERFORMANCE_TIMELINE)).thenReturn(true);
        when(accountRepository.findByIdAndUser_Id(10L, 1L)).thenReturn(Optional.of(account));
        when(tradeRepository.findByUser_IdAndAccount_IdAndTradeDateBetweenOrderByTradeDateAscEntryTimeAscIdAsc(eq(1L), eq(10L), any(), any()))
                .thenReturn(List.of(win));
        when(violationRepository.findForTradingDna(eq(1L), eq(10L), any(), any())).thenReturn(List.of(violation(1L, "RISK", "HIGH")));

        var focus = service.weeklyFocus(user, 10L);

        assertThat(focus.title()).contains("undisciplined wins");
        assertThat(focus.currentFrequency()).isEqualTo(1);
    }

    @Test
    void tradeExecutionRequiresOwnedTrade() {
        User user = user(1L);
        when(featureFlagService.isEnabled(Feature.PERFORMANCE_TIMELINE)).thenReturn(true);
        when(tradeRepository.findByIdAndUser_Id(99L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.tradeExecution(user, 99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    private Trade trade(Long id, BigDecimal pnlR, Integer quantity, Integer risk, Integer confidence, Integer executionRating, Boolean followedPlan) {
        User user = user(1L);
        TradingAccount account = account(10L, user);
        return Trade.builder()
                .id(id)
                .user(user)
                .account(account)
                .symbol("NQ")
                .market("FUTURES")
                .tradeDate(LocalDate.parse("2026-06-22"))
                .entryTime(LocalDateTime.parse("2026-06-22T09:30:00").plusMinutes(id))
                .exitTime(LocalDateTime.parse("2026-06-22T09:40:00").plusMinutes(id))
                .side("LONG")
                .setup(SetupType.IFVG)
                .session(SessionType.NY_AM)
                .quantity(quantity)
                .riskAmount(risk == null ? null : BigDecimal.valueOf(risk))
                .pnlR(pnlR)
                .pnlAmount(pnlR.multiply(BigDecimal.valueOf(100)))
                .outcome(pnlR.compareTo(BigDecimal.ZERO) > 0 ? OutcomeType.WIN : OutcomeType.LOSS)
                .confidence(confidence)
                .executionRating(executionRating)
                .followedPlan(followedPlan)
                .build();
    }

    private TradeRuleViolation violation(Long tradeId, String category, String severity) {
        return TradeRuleViolation.builder()
                .userId(1L)
                .tradeId(tradeId)
                .ruleCode(category + "_RULE")
                .ruleTitle(category + " rule")
                .category(category)
                .severity(severity)
                .source("SYSTEM")
                .tradeDate(LocalDate.parse("2026-06-22"))
                .build();
    }

    private User user(Long id) {
        User user = new User();
        user.setId(id);
        user.setEmail("exec-" + id + "@example.com");
        return user;
    }

    private TradingAccount account(Long id, User user) {
        TradingAccount account = new TradingAccount();
        account.setId(id);
        account.setUser(user);
        account.setName("Account " + id);
        return account;
    }
}
