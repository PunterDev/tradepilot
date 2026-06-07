package com.tradepilot.tradingdna.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradepilot.tradingdna.dto.TradingDnaContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TradingDnaPromptBuilder {
    private final ObjectMapper objectMapper;

    public String systemPrompt() {
        return """
                You are TradePilot Trading DNA Coach.

                You analyze long-term futures trading behavior using only the provided JSON.
                Identify recurring strengths, recurring mistakes, emotional tendencies,
                risk management behavior, best setup archetypes, worst environments,
                revenge trading, overtrading, time-of-day weakness, Friday/OPEX weakness,
                sizing inconsistency, and consistency trends.

                Rules:
                - Use only provided analytics.
                - Never invent trades, performance, or scores.
                - Return strict JSON only.
                - Keep every list item concise and coaching-oriented.
                - Scores must be whole numbers from 0 to 100.

                Required JSON shape:
                {
                  "strengths": [],
                  "weaknesses": [],
                  "emotionalPatterns": [],
                  "riskPatterns": [],
                  "bestConditions": [],
                  "worstConditions": [],
                  "recurringMistakes": [],
                  "improvementTrend": "",
                  "consistencyScore": 0,
                  "disciplineScore": 0,
                  "executionScore": 0,
                  "patienceScore": 0,
                  "confidenceSummary": "",
                  "coachingSummary": ""
                }
                """;
    }

    public String buildPrompt(TradingDnaContext context) throws JsonProcessingException {
        String contextJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(context);

        return """
                Generate a Trading DNA analysis for this trader.

	                Emphasize:
	                - patterns repeated across the whole timeframe
	                - winning conditions versus losing conditions
	                - setup analysis when setup metadata exists
	                - emotion analysis when emotion metadata exists
	                - confidence analysis when confidence metadata exists
	                - revenge trading and overtrading clues
                - time-of-day/session weaknesses
                - Friday/OPEX weakness if present
                - sizing or risk inconsistency
                - best setup archetype
                - realistic next coaching focus

                Trading DNA context JSON:
                %s
                """.formatted(contextJson);
    }
}
