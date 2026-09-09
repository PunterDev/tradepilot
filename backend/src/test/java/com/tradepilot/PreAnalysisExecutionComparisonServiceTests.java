package com.tradepilot;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradepilot.entity.Trade;
import com.tradepilot.entity.User;
import com.tradepilot.enums.OutcomeType;
import com.tradepilot.enums.SessionType;
import com.tradepilot.execution.dto.NoTradeEvaluationStatus;
import com.tradepilot.execution.dto.PlanAlignmentStatus;
import com.tradepilot.execution.dto.PlanComparisonStatus;
import com.tradepilot.execution.service.PreAnalysisExecutionComparisonService;
import com.tradepilot.preanalysis.entity.AiPreAnalysis;
import com.tradepilot.preanalysis.entity.Bias;
import com.tradepilot.preanalysis.entity.PreAnalysisMarket;
import com.tradepilot.preanalysis.entity.PreAnalysisTimeframe;
import com.tradepilot.preanalysis.entity.SetupStatus;
import com.tradepilot.preanalysis.repository.AiPreAnalysisRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class PreAnalysisExecutionComparisonServiceTests {
    private final AiPreAnalysisRepository repository = mock(AiPreAnalysisRepository.class);
    private final PreAnalysisExecutionComparisonService service = new PreAnalysisExecutionComparisonService(repository, new ObjectMapper());
    private final User user = user(1L);

    @Test
    void preAnalysisCreatedBeforeTradeIsSelected() {
        Trade trade = trade(10L, "LONG", "MESU6", "2026-06-22T10:00:00");
        AiPreAnalysis before = plan(1L, "2026-06-22T09:45:00", PreAnalysisMarket.ES, SetupStatus.ENTRY_VALID, Bias.BULLISH, entryValidJson("BULLISH", "SATISFIED"));
        when(repository.findByUserAndCreatedAtBetweenOrderByCreatedAtDesc(eq(user), eq(LocalDateTime.parse("2026-06-22T00:00:00")), eq(LocalDateTime.parse("2026-06-22T10:00:00"))))
                .thenReturn(List.of(before));

        var comparison = service.compareTrade(user, trade);

        assertThat(comparison.preAnalysisId()).isEqualTo(1L);
        assertThat(comparison.status()).isEqualTo(PlanAlignmentStatus.FULL_ALIGNMENT);
    }

    @Test
    void preAnalysisCreatedAfterTradeIsNotUsedAsOriginalPlan() {
        Trade trade = trade(10L, "LONG", "MESU6", "2026-06-22T10:00:00");
        when(repository.findByUserAndCreatedAtBetweenOrderByCreatedAtDesc(eq(user), eq(LocalDateTime.parse("2026-06-22T00:00:00")), eq(LocalDateTime.parse("2026-06-22T10:00:00"))))
                .thenReturn(List.of());

        var comparison = service.compareTrade(user, trade);

        assertThat(comparison.planAvailable()).isFalse();
        assertThat(comparison.status()).isEqualTo(PlanAlignmentStatus.NOT_EVALUABLE);
    }

    @Test
    void biasAlignmentDetectedCorrectly() {
        Trade trade = trade(10L, "SHORT", "MNQU6", "2026-06-22T10:00:00");
        AiPreAnalysis before = plan(1L, "2026-06-22T09:45:00", PreAnalysisMarket.NQ, SetupStatus.ENTRY_VALID, Bias.BEARISH, entryValidJson("BEARISH", "SATISFIED"));
        when(repository.findByUserAndCreatedAtBetweenOrderByCreatedAtDesc(eq(user), any(), any())).thenReturn(List.of(before));

        var bias = service.compareTrade(user, trade).comparisons().stream()
                .filter(item -> item.type().equals("BIAS_ALIGNMENT"))
                .findFirst()
                .orElseThrow();

        assertThat(bias.status()).isEqualTo(PlanComparisonStatus.ALIGNED);
    }

    @Test
    void confirmationViolationDetectedOnlyWhenEvidenceExists() {
        Trade trade = trade(10L, "LONG", "MESU6", "2026-06-22T10:00:00");
        AiPreAnalysis before = plan(1L, "2026-06-22T09:45:00", PreAnalysisMarket.ES, SetupStatus.ENTRY_VALID, Bias.BULLISH, entryValidJson("BULLISH", "FAILED"));
        when(repository.findByUserAndCreatedAtBetweenOrderByCreatedAtDesc(eq(user), any(), any())).thenReturn(List.of(before));

        var confirmation = service.compareTrade(user, trade).comparisons().stream()
                .filter(item -> item.type().equals("CONFIRMATION_ADHERENCE"))
                .findFirst()
                .orElseThrow();

        assertThat(confirmation.status()).isEqualTo(PlanComparisonStatus.VIOLATED);
    }

    @Test
    void missingConfirmationEvidenceReturnsNotEvaluable() {
        Trade trade = trade(10L, "LONG", "MESU6", "2026-06-22T10:00:00");
        AiPreAnalysis before = plan(1L, "2026-06-22T09:45:00", PreAnalysisMarket.ES, SetupStatus.ENTRY_VALID, Bias.BULLISH, "{\"directionalContext\":{\"bias\":\"BULLISH\"},\"entryAssessment\":{\"status\":\"VALID\"}}");
        when(repository.findByUserAndCreatedAtBetweenOrderByCreatedAtDesc(eq(user), any(), any())).thenReturn(List.of(before));

        var confirmation = service.compareTrade(user, trade).comparisons().stream()
                .filter(item -> item.type().equals("CONFIRMATION_ADHERENCE"))
                .findFirst()
                .orElseThrow();

        assertThat(confirmation.status()).isEqualTo(PlanComparisonStatus.NOT_EVALUABLE);
    }

    @Test
    void noTradesWithExplicitNoTradePlanIsSuccessfulNoTrade() {
        AiPreAnalysis plan = plan(1L, "2026-06-22T09:45:00", PreAnalysisMarket.ES, SetupStatus.NO_TRADE, Bias.NEUTRAL, noTradeJson());
        when(repository.findByUserAndCreatedAtBetweenOrderByCreatedAtDesc(eq(user), any(), any())).thenReturn(List.of(plan));

        var evaluation = service.evaluateNoTrade(user, LocalDate.parse("2026-06-22"), List.of());

        assertThat(evaluation.status()).isEqualTo(NoTradeEvaluationStatus.SUCCESSFUL_NO_TRADE);
    }

    @Test
    void noTradesWithNoPlanIsInsufficientEvidence() {
        when(repository.findByUserAndCreatedAtBetweenOrderByCreatedAtDesc(eq(user), any(), any())).thenReturn(List.of());

        var evaluation = service.evaluateNoTrade(user, LocalDate.parse("2026-06-22"), List.of());

        assertThat(evaluation.status()).isEqualTo(NoTradeEvaluationStatus.INSUFFICIENT_EVIDENCE);
    }

    @Test
    void tradeTakenAgainstExplicitNoTradeConditionIsPlanViolation() {
        Trade trade = trade(10L, "LONG", "MESU6", "2026-06-22T10:00:00");
        AiPreAnalysis plan = plan(1L, "2026-06-22T09:45:00", PreAnalysisMarket.ES, SetupStatus.NO_TRADE, Bias.NEUTRAL, noTradeJson());
        when(repository.findByUserAndCreatedAtBetweenOrderByCreatedAtDesc(eq(user), any(), any())).thenReturn(List.of(plan));

        var evaluation = service.evaluateNoTrade(user, LocalDate.parse("2026-06-22"), List.of(trade));

        assertThat(evaluation.status()).isEqualTo(NoTradeEvaluationStatus.PLAN_VIOLATED_BY_TRADE);
    }

    @Test
    void ownershipProtectionUsesUserScopedRepositoryQuery() {
        Trade trade = trade(10L, "LONG", "MESU6", "2026-06-22T10:00:00");

        service.compareTrade(user, trade);

        verify(repository).findByUserAndCreatedAtBetweenOrderByCreatedAtDesc(eq(user), any(), any());
    }

    private static String entryValidJson(String bias, String confirmationStatus) {
        return "{\"directionalContext\":{\"bias\":\"" + bias + "\"},\"conditions\":{\"confirmation\":{\"status\":\"" + confirmationStatus + "\",\"evidence\":\"structured\"}},\"entryAssessment\":{\"status\":\"VALID\"},\"setupStatus\":\"ENTRY_VALID\",\"setupAlignmentScore\":90}";
    }

    private static String noTradeJson() {
        return "{\"directionalContext\":{\"bias\":\"NEUTRAL\"},\"entryAssessment\":{\"status\":\"NO_TRADE\"},\"setupStatus\":\"NO_TRADE\",\"noTradeScenario\":{\"condition\":\"Wait for full confirmation.\",\"reason\":\"Setup conditions were not satisfied.\"}}";
    }

    private static AiPreAnalysis plan(Long id, String createdAt, PreAnalysisMarket market, SetupStatus setupStatus, Bias bias, String json) {
        AiPreAnalysis analysis = new AiPreAnalysis();
        analysis.setId(id);
        analysis.setUser(user(1L));
        analysis.setMarket(market);
        analysis.setTimeframe(PreAnalysisTimeframe.M5);
        analysis.setSetupStatus(setupStatus);
        analysis.setBias(bias);
        analysis.setSetupAlignmentScore(setupStatus == SetupStatus.ENTRY_VALID ? 90 : 20);
        analysis.setAnalysisJson(json);
        analysis.setPromptVersion("test");
        analysis.setCreatedAt(LocalDateTime.parse(createdAt));
        return analysis;
    }

    private static Trade trade(Long id, String side, String symbol, String entryTime) {
        Trade trade = Trade.builder()
                .id(id)
                .user(user(1L))
                .symbol(symbol)
                .market("FUTURES")
                .tradeDate(LocalDate.parse("2026-06-22"))
                .entryTime(LocalDateTime.parse(entryTime))
                .exitTime(LocalDateTime.parse(entryTime).plusMinutes(10))
                .side(side)
                .session(SessionType.NY_AM)
                .quantity(1)
                .riskAmount(BigDecimal.valueOf(100))
                .pnlR(BigDecimal.ONE)
                .pnlAmount(BigDecimal.valueOf(100))
                .outcome(OutcomeType.WIN)
                .build();
        trade.setContractSymbol(symbol);
        return trade;
    }

    private static User user(Long id) {
        User user = new User();
        user.setId(id);
        user.setEmail("user" + id + "@example.com");
        user.setName("User " + id);
        user.setPasswordHash("hash");
        return user;
    }
}
