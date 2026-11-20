package com.tradepilot.coach.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradepilot.ai.openai.OpenAiResponsesClient;
import com.tradepilot.coach.dto.CoachAiResponse;
import com.tradepilot.coach.dto.CoachDeterministicSummary;
import com.tradepilot.coach.entity.CoachSnapshotType;
import com.tradepilot.leakdetector.dto.LeakResultDto;
import org.springframework.stereotype.Service;

@Service
public class CoachAiGeneratorService {
    @java.lang.SuppressWarnings("all")
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(CoachAiGeneratorService.class);
    private final ObjectMapper objectMapper;
    private final OpenAiResponsesClient openAiResponsesClient;
    private final CoachAiPromptBuilder promptBuilder;

    public CoachAiResponse generate(CoachSnapshotType type, CoachDeterministicSummary summary, Long userId) throws Exception {
		return generate(type, summary, userId, null);
	}

    public CoachAiResponse generate(CoachSnapshotType type, CoachDeterministicSummary summary, Long userId, LeakResultDto behavioralLeak) throws Exception {
        if (!openAiResponsesClient.isConfigured()) {
            return fallback(summary);
        }
        String content = openAiResponsesClient.generateJson(userId, type.name() + "_COACH", promptBuilder.systemPrompt(), promptBuilder.buildPrompt(type, summary, behavioralLeak)).content();
        content = cleanJson(content);
        return objectMapper.readValue(content, CoachAiResponse.class);
    }

    private String cleanJson(String content) {
        if (content == null) {
            return "{}";
        }
        return content.replace("```json", "").replace("```", "").trim();
    }

    private CoachAiResponse fallback(CoachDeterministicSummary summary) {
        return CoachAiResponse.builder().headline(summary.getSelectedDaySummary() == null ? "Deterministic performance summary available." : summary.getSelectedDaySummary()).emotionalPattern(summary.getDataQualityReason() == null ? "No AI emotional conclusion was generated." : summary.getDataQualityReason()).bestBehavior(summary.getBestSetup() == null ? "No best setup conclusion is available." : "Recorded best setup: " + summary.getBestSetup()).biggestMistake(summary.getBiggestMistake()).bestSetup(summary.getBestSetup()).avoidNextSession(summary.getPrimaryIssueToday() == null ? "No AI avoidance recommendation was generated." : summary.getPrimaryIssueToday()).nextSessionFocus(summary.getSetupDirectionReview() == null ? "Review the deterministic summary before choosing the next-session focus." : summary.getSetupDirectionReview()).riskNote("Use the risk limits in your documented plan.").disciplineNote("No AI discipline inference was generated.").oneRuleForNextSession(summary.getNextSessionRule() == null ? "Choose one documented rule after reviewing the available evidence." : summary.getNextSessionRule()).build();
    }

    @java.lang.SuppressWarnings("all")
    public CoachAiGeneratorService(final ObjectMapper objectMapper, final OpenAiResponsesClient openAiResponsesClient, final CoachAiPromptBuilder promptBuilder) {
        this.objectMapper = objectMapper;
        this.openAiResponsesClient = openAiResponsesClient;
        this.promptBuilder = promptBuilder;
    }
}
