package com.tradepilot.coach.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradepilot.coach.dto.CoachDeterministicSummary;
import com.tradepilot.coach.entity.CoachSnapshotType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CoachAiPromptBuilder {

    private final ObjectMapper objectMapper;

    public String systemPrompt() {
        return """
                You are TradePilot Coach.

                You are an elite trading performance mentor for futures traders.

                Your job:
                - analyze deterministic trading analytics
                - identify behavioral patterns
                - detect emotional trading
                - reinforce strong habits
                - correct weak execution
                - improve discipline and risk management

                Rules:
                - Use ONLY the provided analytics JSON.
                - Never invent trades or statistics.
                - Be concise and actionable.
                - Speak like a professional trading mentor.
                - Focus on improvement for the next session/week.

                Return STRICT JSON ONLY.

                Required JSON format:
                {
                  "headline": "",
                  "emotionalPattern": "",
                  "bestBehavior": "",
                  "biggestMistake": "",
                  "bestSetup": "",
                  "avoidNextSession": "",
                  "nextSessionFocus": "",
                  "riskNote": "",
                  "disciplineNote": "",
                  "oneRuleForNextSession": ""
                }
                """;
    }

    public String buildPrompt(
            CoachSnapshotType type,
            CoachDeterministicSummary summary
    ) throws JsonProcessingException {

        String analyticsJson =
                objectMapper.writerWithDefaultPrettyPrinter()
                        .writeValueAsString(summary);

        String coachType =
                type == CoachSnapshotType.DAILY
                        ? "DAILY COACH"
                        : "WEEKLY COACH";

        return """
                Generate a %s report.

	                Focus areas:
	                - emotional trading
	                - emotion-tagged trade performance when present
	                - confidence-tagged trade performance when present
	                - revenge trading
	                - discipline
	                - risk management
	                - best setup
	                - setup-tagged trade performance when present
                - weakest setup
                - session performance
                - repeated mistakes
                - Friday/OPEX weakness if present

                Keep responses practical and actionable.

                Analytics JSON:
                %s
                """.formatted(coachType, analyticsJson);
    }
}
