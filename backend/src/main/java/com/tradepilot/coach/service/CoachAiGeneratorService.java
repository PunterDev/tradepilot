package com.tradepilot.coach.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradepilot.config.OpenAiProperties;
import com.tradepilot.coach.dto.CoachAiResponse;
import com.tradepilot.coach.dto.CoachDeterministicSummary;
import com.tradepilot.coach.entity.CoachSnapshotType;
import com.tradepilot.observability.ObservabilityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class CoachAiGeneratorService {

    private static final int MAX_RETRIES = 2;
    private static final long RETRY_DELAY_MS = 700;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final OpenAiProperties openAiProperties;
    private final CoachAiPromptBuilder promptBuilder;
    private final ObservabilityService observabilityService;

    public CoachAiResponse generate(
            CoachSnapshotType type,
            CoachDeterministicSummary summary,
            Long userId
    ) throws Exception {

        if (openAiProperties.getApiKey() == null
                || openAiProperties.getApiKey().isBlank()) {

            return fallback(summary);
        }

        return callOpenAiWithRetry(type, summary, userId);
    }

    private CoachAiResponse callOpenAiWithRetry(
            CoachSnapshotType type,
            CoachDeterministicSummary summary,
            Long userId
    ) throws Exception {

        Exception lastException = null;

        for (int attempt = 1; attempt <= MAX_RETRIES + 1; attempt++) {

            try {
                return callOpenAi(type, summary, attempt, userId);

            } catch (Exception e) {

                lastException = e;

                log.warn(
                        "Coach AI generation failed. attempt={}, maxAttempts={}",
                        attempt,
                        MAX_RETRIES + 1,
                        e
                );

                if (attempt <= MAX_RETRIES) {
                    Thread.sleep(RETRY_DELAY_MS * attempt);
                }
            }
        }

        throw lastException;
    }

    private CoachAiResponse callOpenAi(
            CoachSnapshotType type,
            CoachDeterministicSummary summary,
            int attempt,
            Long userId
    ) throws Exception {

        Instant start = Instant.now();

        String prompt = promptBuilder.buildPrompt(type, summary);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(openAiProperties.getApiKey());

        Map<String, Object> body = Map.of(
                "model", openAiProperties.getModel(),
                "messages", List.of(
                        Map.of(
                                "role",
                                "system",
                                "content",
                                promptBuilder.systemPrompt()
                        ),
                        Map.of(
                                "role",
                                "user",
                                "content",
                                prompt
                        )
                ),
                "temperature", 0.3
        );

        HttpEntity<Map<String, Object>> entity =
                new HttpEntity<>(body, headers);

        ResponseEntity<String> response;
        try {
            response =
                    restTemplate.exchange(
                            "https://api.openai.com/v1/chat/completions",
                            HttpMethod.POST,
                            entity,
                            String.class
                    );
        } catch (Exception ex) {
            observabilityService.recordAiCall(userId, type.name() + "_COACH", openAiProperties.getModel(), 0, 0, 0,
                    Duration.between(start, Instant.now()).toMillis(), false);
            throw ex;
        }

        long elapsedMs =
                Duration.between(start, Instant.now()).toMillis();

        JsonNode root =
                objectMapper.readTree(response.getBody());

        JsonNode usage = root.path("usage");
        int promptTokens = usage.path("prompt_tokens").asInt(0);
        int completionTokens = usage.path("completion_tokens").asInt(0);
        int totalTokens = usage.path("total_tokens").asInt(0);

        log.info(
                "Coach AI response received. attempt={}, elapsedMs={}, totalTokens={}",
                attempt,
                elapsedMs,
                totalTokens
        );
        observabilityService.recordAiCall(userId, type.name() + "_COACH", openAiProperties.getModel(), promptTokens, completionTokens, totalTokens, elapsedMs, true);

        String content = root
                .path("choices")
                .get(0)
                .path("message")
                .path("content")
                .asText();

        content = cleanJson(content);

        return objectMapper.readValue(
                content,
                CoachAiResponse.class
        );
    }

    private String cleanJson(String content) {

        if (content == null) {
            return "{}";
        }

        return content
                .replace("```json", "")
                .replace("```", "")
                .trim();
    }

    private CoachAiResponse fallback(
            CoachDeterministicSummary summary
    ) {

        return CoachAiResponse.builder()
                .headline(summary.getSelectedDaySummary() == null
                        ? "Trading performance reviewed."
                        : summary.getSelectedDaySummary())
                .emotionalPattern(
                        summary.getDataQualityReason() == null
                                ? "No emotion or setup metadata was recorded, so coaching is based only on execution data."
                                : summary.getDataQualityReason()
                )
                .bestBehavior(
                        "Best setup: " + summary.getBestSetup()
                )
                .biggestMistake(
                        summary.getBiggestMistake()
                )
                .bestSetup(
                        summary.getBestSetup()
                )
                .avoidNextSession(
                        summary.getPrimaryIssueToday() == null
                                ? "Avoid repeating weak setups and emotional entries."
                                : summary.getPrimaryIssueToday()
                )
                .nextSessionFocus(
                        summary.getSetupDirectionReview() == null
                                ? "Focus on your highest quality setup only."
                                : summary.getSetupDirectionReview()
                )
                .riskNote(
                        "Respect risk limits and avoid overtrading."
                )
                .disciplineNote(
                        "Wait for confirmation before entering."
                )
                .oneRuleForNextSession(
                        summary.getNextSessionRule() == null
                                ? "After a loss, pause before the next trade."
                                : summary.getNextSessionRule()
                )
                .build();
    }
}
