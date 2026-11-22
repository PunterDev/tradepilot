package com.tradepilot;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradepilot.ai.strategy.StrategyContext;
import com.tradepilot.ai.strategy.StrategyContextService;
import com.tradepilot.ai.strategy.StrategyType;
import com.tradepilot.dto.ai.AiTradeAnalysisRequest;
import com.tradepilot.dto.ai.AiTradeReviewRequest;
import com.tradepilot.dto.ai.TradeSnapshot;
import com.tradepilot.service.AiChartReviewService;
import com.tradepilot.service.AiTradeAnalysisPromptBuilder;
import com.tradepilot.service.AiTradeReviewPromptBuilder;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

class PublicAiPromptStrategyContextTests {
    private final AiTradeReviewPromptBuilder reviewPromptBuilder = new AiTradeReviewPromptBuilder();
    private final AiTradeAnalysisPromptBuilder analysisPromptBuilder = new AiTradeAnalysisPromptBuilder();
    private final AiChartReviewService chartReviewService = new AiChartReviewService(null, null, null, null, null, null);

    @Test
    void ictContextAllowsIctConcepts() {
        StrategyContext context = context(StrategyType.ICT_SMC, "ICT London model", "FVG", List.of("Wait for FVG retracement before entry."));
        String reviewPrompt = reviewPromptBuilder.buildPrompt(reviewRequest("FVG", context, "Waited for displacement into fair value gap."), context);
        String chartPrompt = chartReviewService.buildPrompt(10L, "ICT setup", context);

        assertThat(reviewPrompt).contains("fair value gap", "displacement", "liquidity sweep", "SMT");
        assertThat(chartPrompt).contains("FVG", "IFVG", "Liquidity Sweep");
    }

    @Test
    void openingRangeContextDoesNotLeakIctRequirements() {
        StrategyContext context = context(
                StrategyType.OPENING_RANGE,
                "Opening Range Breakout",
                "ORB",
                List.of("Break first 15-minute range.", "Wait for close outside range.", "No retest required.", "Use 1R stop and 2R target.")
        );

        String reviewPrompt = reviewPromptBuilder.buildPrompt(reviewRequest("ORB", context, "Opening range breakout through the high."), context);
        String analysisPrompt = analysisPromptBuilder.build(analysisRequest("ORB", context, "Opening range breakout through the high."), true);
        String chartPrompt = chartReviewService.buildPrompt(11L, "Opening range breakout through the high.", context);

        assertThat(reviewPrompt).contains("Opening Range Breakout", "No retest required.");
        assertNoIctLeakage(reviewPrompt);
        assertNoIctLeakage(analysisPrompt);
        assertNoIctLeakage(chartPrompt);
    }

    @Test
    void trendFollowingContextFocusesOnTrendWithoutMandatoryIct() {
        StrategyContext context = context(StrategyType.TREND_FOLLOWING, "Trend Following", "TREND_CONTINUATION", List.of("Trade only with higher-timeframe trend.", "Enter on pullback continuation."));

        String prompt = analysisPromptBuilder.build(analysisRequest("TREND_CONTINUATION", context, "Pullback continuation with higher timeframe trend."), false);

        assertThat(prompt).contains("Trend Following", "pullback", "continuation");
        assertNoIctLeakage(prompt);
    }

    @Test
    void meanReversionContextFocusesOnReversionWithoutMandatoryIct() {
        StrategyContext context = context(StrategyType.MEAN_REVERSION, "VWAP Mean Reversion", "RANGE_REVERSION", List.of("Enter only after extension and exhaustion.", "Target VWAP or session mean.", "Invalidate on continuation through the extreme."));

        String prompt = analysisPromptBuilder.build(analysisRequest("RANGE_REVERSION", context, "Short after extension away from VWAP."), true);

        assertThat(prompt).contains("VWAP Mean Reversion", "extension", "exhaustion");
        assertThat(prompt.toLowerCase(Locale.ROOT)).contains("mean reversion");
        assertNoIctLeakage(prompt);
    }

    @Test
    void customContextKeepsUserRulesDominantWithoutUnrelatedMethodology() {
        StrategyContext context = context(StrategyType.CUSTOM, "My custom scalp", "OTHER", List.of("Only trade after two failed pushes.", "Stop after one loss.", "Exit at fixed 1.5R."));

        String prompt = reviewPromptBuilder.buildPrompt(reviewRequest("OTHER", context, "Two failed pushes into resistance."), context);

        assertThat(prompt).contains("My custom scalp", "Only trade after two failed pushes.", "Stop after one loss.", "Exit at fixed 1.5R.");
        assertNoIctLeakage(prompt);
    }

    @Test
    void unknownContextDefaultsToGeneralWithoutIct() {
        StrategyContext context = StrategyContext.general();

        String reviewPrompt = reviewPromptBuilder.buildPrompt(reviewRequest(null, context, "Managed risk well but entered early."), context);
        String analysisPrompt = analysisPromptBuilder.build(analysisRequest(null, context, "Managed risk well but entered early."), false);
        String chartPrompt = chartReviewService.buildPrompt(12L, "Managed risk well but entered early.", context);

        assertThat(reviewPrompt).contains("GENERAL");
        assertNoIctLeakage(reviewPrompt);
        assertNoIctLeakage(analysisPrompt);
        assertNoIctLeakage(chartPrompt);
    }

    @Test
    void genericLiquidityTextDoesNotResolveToIct() {
        StrategyContextService service = new StrategyContextService(null, new ObjectMapper());

        StrategyContext context = service.resolveFromText("Watching liquidity near the prior high, but no setup type is defined.");

        assertThat(context.strategyType()).isEqualTo(StrategyType.GENERAL);
    }

    private AiTradeReviewRequest reviewRequest(String setup, StrategyContext context, String journalText) {
        AiTradeReviewRequest request = new AiTradeReviewRequest();
        request.setTradeId(100L);
        request.setJournalText(journalText);
        request.setTrade(trade(setup));
        request.setStrategyContext(context);
        return request;
    }

    private AiTradeAnalysisRequest analysisRequest(String setup, StrategyContext context, String journalText) {
        AiTradeAnalysisRequest request = new AiTradeAnalysisRequest();
        request.setTradeId(100L);
        request.setJournalText(journalText);
        request.setTrade(trade(setup));
        request.setStrategyContext(context);
        return request;
    }

    private TradeSnapshot trade(String setup) {
        TradeSnapshot trade = new TradeSnapshot();
        trade.setId(100L);
        trade.setSymbol("MES");
        trade.setSide("LONG");
        trade.setPnl(125.0);
        trade.setQuantity(2);
        trade.setEntryPrice(5000.0);
        trade.setExitPrice(5008.0);
        trade.setSetup(setup);
        trade.setNotes("Reviewed against the selected strategy.");
        return trade;
    }

    private StrategyContext context(StrategyType type, String name, String setup, List<String> rules) {
        return new StrategyContext(
                type,
                name,
                setup,
                setup,
                type.name(),
                name,
                rules,
                rules,
                List.of(),
                List.of(),
                List.of(),
                concepts(type),
                "TEST",
                1.0
        );
    }

    private List<String> concepts(StrategyType type) {
        return switch (type) {
            case ICT_SMC -> List.of("fair value gap", "inversion fair value gap", "displacement", "liquidity sweep", "SMT", "premium/discount", "order block");
            case OPENING_RANGE -> List.of("opening range", "opening range high/low", "breakout", "acceptance", "failed breakout");
            case TREND_FOLLOWING -> List.of("higher-timeframe trend", "pullback", "continuation", "momentum confirmation");
            case MEAN_REVERSION -> List.of("extension", "exhaustion", "reversion", "VWAP", "invalidation");
            default -> List.of("user-defined rules", "plan adherence", "risk management");
        };
    }

    private void assertNoIctLeakage(String prompt) {
        String lower = prompt.toLowerCase(Locale.ROOT);
        assertThat(lower).doesNotContain(" ict ", "ict/", "ict_", "ict-");
        assertThat(lower).doesNotContain("fvg");
        assertThat(lower).doesNotContain("ifvg");
        assertThat(lower).doesNotContain("fair value gap");
        assertThat(lower).doesNotContain("smt");
        assertThat(lower).doesNotContain("premium/discount");
        assertThat(lower).doesNotContain("premium discount");
        assertThat(lower).doesNotContain("order block");
        assertThat(lower).doesNotContain("liquidity sweep");
    }
}
