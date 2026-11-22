package com.tradepilot.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradepilot.ai.openai.OpenAiGeneratedResponse;
import com.tradepilot.ai.openai.OpenAiResponsesClient;
import com.tradepilot.ai.openai.OpenAiUnavailableException;
import com.tradepilot.ai.strategy.StrategyContext;
import com.tradepilot.ai.strategy.StrategyContextService;
import com.tradepilot.billing.UsageLimitService;
import com.tradepilot.billing.UsageType;
import com.tradepilot.dto.ai.AiChartReviewResponse;
import com.tradepilot.entity.Trade;
import com.tradepilot.entity.User;
import com.tradepilot.security.OwnershipGuard;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

@Service
public class AiChartReviewService {
    @java.lang.SuppressWarnings("all")
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AiChartReviewService.class);
    private final ObjectMapper objectMapper;
    private final OwnershipGuard ownershipGuard;
    private final TradeService tradeService;
    private final StrategyContextService strategyContextService;
    private final OpenAiResponsesClient openAiResponsesClient;
    private final UsageLimitService usageLimitService;

    public AiChartReviewResponse reviewChart(User user, MultipartFile file, Long tradeId, String journalText) {
        ownershipGuard.requireCurrentUserId(user);
        Trade trade = null;
        if (tradeId != null) {
            ownershipGuard.requireTradeOwner(user, tradeId);
            trade = tradeService.getOwnedTrade(user, tradeId);
        }
        StrategyContext strategyContext = strategyContextService.resolve(user == null ? null : user.getId(), trade, journalText);
        String prompt = buildPrompt(tradeId, journalText, strategyContext);
        usageLimitService.enforceAiUsage(user, UsageType.AI_TRADE_REVIEW);

        try {
            OpenAiGeneratedResponse response = openAiResponsesClient.generateJson(
                    user.getId(), "AI_CHART_REVIEW", chartSystemPrompt(), prompt, file.getBytes(), file.getContentType());
            AiChartReviewResponse parsed = objectMapper.readValue(cleanJson(response.content()), AiChartReviewResponse.class);
            usageLimitService.recordAiUsage(user, UsageType.AI_TRADE_REVIEW, tradeId);
            return parsed;
        } catch (OpenAiUnavailableException e) {
            throw e;
        } catch (Exception e) {
            log.warn("AI chart review response could not be safely interpreted. failureCategory={}", e.getClass().getSimpleName());
            throw new OpenAiUnavailableException("AI chart response could not be safely interpreted", e);
        }
    }

    public String buildPrompt(Long tradeId, String journalText, StrategyContext strategyContext) {
        StrategyContext context = strategyContext == null ? StrategyContext.general() : strategyContext;
        return """
            You are an expert futures trading coach reviewing an NQ/ES chart screenshot.
            
            Analyze the chart visually and return ONLY valid JSON.

            %s
            
            Focus on:
            - observable chart evidence relevant to StrategyContext
            - general execution context
            - entry timing relative to the user's own rules
            - entry timing
            - stop loss placement
            - take profit logic
            - risk/reward
            - plan-adherence evidence only when a matching rule or journal fact is supplied

            Strategy rules:
            - User playbook and user rules override generic strategy-profile assumptions.
            - Apply strategy-specific concepts only when StrategyContext supports them.
            - Do not introduce requirements or terminology from trading methodologies that are not part of the user's strategy, setup, playbook, or explicitly documented trade rationale.
            - General market-structure, retracement, or liquidity language does not by itself imply any specific methodology.
            - If StrategyContext is GENERAL or CUSTOM, stay methodology-neutral and evaluate only visible price action, documented setup, risk, timing, management, discipline, and plan adherence.
            - Treat the screenshot as visual evidence only. Treat journal text and the selected trade as supplied facts only.
            - Do not infer emotions, hidden orders, fills, rule violations, profitability, or a setup that is not visible or supplied.
            - When evidence is absent, say that it cannot be determined from the available chart and notes.
            
            Trade ID: %s
            
            Trader journal note:
            %s
            
            Return this JSON shape:
            {
              \"summary\": \"...\",
              \"bias\": \"...\",
              \"entryQuality\": \"...\",
              \"stopLossQuality\": \"...\",
              \"takeProfitQuality\": \"...\",
              \"riskManagement\": \"...\",
              \"grade\": \"A/B/C/D/F\",
              \"mistakes\": [\"...\"],
              \"strengths\": [\"...\"],
              \"improvementPlan\": [\"...\"],
              \"tags\": %s
            }
            """.formatted(context.promptBlock(), tradeId == null ? "N/A" : tradeId, journalText == null ? "" : journalText, suggestedTags(context));
    }

    private String chartSystemPrompt() {
        return "You are TradePilot's chart-review assistant. Return only the requested JSON. Ground every conclusion in visible chart evidence or explicitly supplied trade and journal facts.";
    }

    private String cleanJson(String content) {
        return content == null ? "{}" : content.replace("```json", "").replace("```", "").trim();
    }

    private String suggestedTags(StrategyContext context) {
        if (context != null && context.isIct()) {
            return "[\"FVG\", \"IFVG\", \"Liquidity Sweep\", \"Displacement\", \"SMT\"]";
        }
        return "[\"Good Patience\", \"Late Entry\", \"Early Entry\", \"Poor Risk Management\", \"Good Risk Management\", \"Rule Violation\", \"Choppy Market\"]";
    }

    @java.lang.SuppressWarnings("all")
    public AiChartReviewService(final ObjectMapper objectMapper, final OwnershipGuard ownershipGuard, final TradeService tradeService, final StrategyContextService strategyContextService, final OpenAiResponsesClient openAiResponsesClient, final UsageLimitService usageLimitService) {
        this.objectMapper = objectMapper;
        this.ownershipGuard = ownershipGuard;
        this.tradeService = tradeService;
        this.strategyContextService = strategyContextService;
        this.openAiResponsesClient = openAiResponsesClient;
        this.usageLimitService = usageLimitService;
    }
}
