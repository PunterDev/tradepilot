package com.tradepilot.service;

import com.tradepilot.ai.strategy.StrategyContext;
import com.tradepilot.dto.ai.AiTradeReviewRequest;
import org.springframework.stereotype.Component;

@Component
public class AiTradeReviewPromptBuilder {

    public String buildPrompt(AiTradeReviewRequest request) {
        return buildPrompt(request, context(request));
    }

    public String buildPrompt(AiTradeReviewRequest request, StrategyContext strategyContext) {
        return """
                Review this futures trade.

                %s

                Trade:
                %s

                Journal:
                %s

                Focus on:
                - the trader's documented strategy, setup, playbook, and execution rules
                - user-rule adherence before any generic strategy profile assumptions
                - entry quality relative to the user's own criteria
                - patience
                - FOMO/revenge trading
                - risk management
                - whether the trader followed a good process
                - practical improvement for next trade

                Strategy rules:
                - Apply strategy-specific concepts only when they are part of StrategyContext.
                - Do not introduce requirements or terminology from trading methodologies that are not part of the user's strategy, setup, playbook, or explicitly documented trade rationale.
                - If StrategyContext is GENERAL or CUSTOM, stay methodology-neutral and evaluate only documented setup, risk, timing, management, discipline, and plan adherence.

                Keep response specific, direct, and useful.
                """.formatted(
                safeContext(strategyContext),
                request.getTrade(),
                request.getJournalText() == null ? "" : request.getJournalText()
        );
    }

    public String systemPrompt() {
        return systemPrompt(StrategyContext.general());
    }

    public String systemPrompt(StrategyContext strategyContext) {
        return """
                You are TradePilot AI, a disciplined futures trading coach.

                Review futures trades using:
                - the supplied StrategyContext as the source of truth for methodology-specific concepts
                - risk management
                - patience
                - psychology
                - execution quality

                Methodology guardrails:
                - User playbook and user rules override profile defaults.
                - User strategy/setup overrides general execution principles.
                - General execution principles apply when evidence exists.
                - Optional strategy-specific knowledge applies only when StrategyContext supports it.
                - Never assume a methodology merely because the instrument is ES/NQ, the market is futures, or the user mentions structure, retracement, or liquidity generically.
                - Do not criticize a trade for missing a condition unless that condition appears in StrategyContext or the user's documented rationale.

                Return ONLY valid JSON.
                Do not include markdown.
                Do not include explanations outside JSON.
                Do not add extra fields.

                JSON shape:
                {
                  "summary": "string",
                  "mistakes": ["string"],
                  "strengths": ["string"],
                  "improvementPlan": ["string"],
                  "tags": ["string"],
                  "grade": "string"
                }

                Allowed tags only:
                FOMO, Revenge Trade, Overtrading, Late Entry, Early Entry, Good Patience,
                Poor Patience, A+ Setup, Forced Trade, Choppy Market, Trend Day,
                NY Open, Moved Stop Too Early, Held Too Long, Exited Too Early,
                Good Risk Management, Poor Risk Management, Rule Violation

                Tag rules:
                - Use only the allowed tags.
                - Return 1 to 5 tags maximum.
                - Do not create custom tags.
                - If there is not enough evidence for a tag, do not use it.
                """;
    }

    private StrategyContext context(AiTradeReviewRequest request) {
        return request == null || request.getStrategyContext() == null ? StrategyContext.general() : request.getStrategyContext();
    }

    private String safeContext(StrategyContext strategyContext) {
        return strategyContext == null ? StrategyContext.general().promptBlock() : strategyContext.promptBlock();
    }
}
