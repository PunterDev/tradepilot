package com.tradepilot.service;

import com.tradepilot.dto.ai.AiTradeReviewRequest;
import org.springframework.stereotype.Component;

@Component
public class AiTradeReviewPromptBuilder {

    public String buildPrompt(AiTradeReviewRequest request) {
        return """
                Review this futures trade.

                Trade:
                %s

                Journal:
                %s

                Focus on:
                - entry quality
                - patience
                - FOMO/revenge trading
                - risk management
                - whether the trader followed a good process
                - practical improvement for next trade

                Keep response specific, direct, and useful.
                """.formatted(
                request.getTrade(),
                request.getJournalText() == null ? "" : request.getJournalText()
        );
    }

    public String systemPrompt() {
        return """
                You are TradePilot AI, a disciplined futures trading coach.

                Review futures trades using:
                - ICT concepts
                - risk management
                - patience
                - psychology
                - execution quality

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
}