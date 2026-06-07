package com.tradepilot.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradepilot.config.OpenAiProperties;
import com.tradepilot.dto.session.AiSessionReviewResponse;
import com.tradepilot.dto.trade.TradeResponse;
import com.tradepilot.observability.ObservabilityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiSessionReviewGeneratorService {
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final OpenAiProperties openAiProperties;
    private final ObservabilityService observabilityService;

    public AiSessionReviewResponse generate(
            Long userId,
            LocalDate sessionDate,
            List<TradeResponse> trades,
            BigDecimal totalPnl,
            BigDecimal winRate,
            Map<String, Long> mistakeSummary,
            Integer disciplineScore
    ) {
        if (openAiProperties.getApiKey() == null || openAiProperties.getApiKey().isBlank()) {
            log.debug("Session review OpenAI call skipped; API key missing. userId={}, sessionDate={}", userId, sessionDate);
            return fallback(sessionDate, trades, totalPnl, winRate, mistakeSummary, disciplineScore);
        }

        try {
            String content = callOpenAi(userId, sessionDate, trades, totalPnl, winRate, mistakeSummary, disciplineScore);
            return objectMapper.readValue(content, AiSessionReviewResponse.class);
        } catch (Exception error) {
            log.warn("AI session review failed. sessionDate={}", sessionDate, error);
            return fallback(sessionDate, trades, totalPnl, winRate, mistakeSummary, disciplineScore);
        }
    }

    private String callOpenAi(
            Long userId,
            LocalDate sessionDate,
            List<TradeResponse> trades,
            BigDecimal totalPnl,
            BigDecimal winRate,
            Map<String, Long> mistakeSummary,
            Integer disciplineScore
    ) throws Exception {
        Instant start = Instant.now();
        log.debug("Session review OpenAI call start. userId={}, sessionDate={}, model={}, tradeCount={}",
                userId, sessionDate, openAiProperties.getModel(), trades.size());
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(openAiProperties.getApiKey());

        Map<String, Object> body = Map.of(
                "model", openAiProperties.getModel(),
                "temperature", 0.25,
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt()),
                        Map.of("role", "user", "content", prompt(sessionDate, trades, totalPnl, winRate, mistakeSummary, disciplineScore))
                )
        );

        ResponseEntity<String> response;
        try {
            response = restTemplate.exchange(
                    "https://api.openai.com/v1/chat/completions",
                    HttpMethod.POST,
                    new HttpEntity<>(body, headers),
                    String.class
            );
        } catch (Exception ex) {
            long elapsedMs = Duration.between(start, Instant.now()).toMillis();
            log.warn("Session review OpenAI call failed. userId={}, sessionDate={}, elapsedMs={}",
                    userId, sessionDate, elapsedMs, ex);
            observabilityService.recordAiCall(userId, "SESSION_REVIEW", openAiProperties.getModel(), 0, 0, 0,
                    elapsedMs, false);
            throw ex;
        }

        long elapsedMs = Duration.between(start, Instant.now()).toMillis();
        JsonNode root = objectMapper.readTree(response.getBody());
        JsonNode usage = root.path("usage");
        log.debug("Session review OpenAI call end. userId={}, sessionDate={}, status={}, elapsedMs={}, totalTokens={}",
                userId, sessionDate, response.getStatusCode().value(), elapsedMs, usage.path("total_tokens").asInt(0));
        observabilityService.recordAiCall(
                userId,
                "SESSION_REVIEW",
                openAiProperties.getModel(),
                usage.path("prompt_tokens").asInt(0),
                usage.path("completion_tokens").asInt(0),
                usage.path("total_tokens").asInt(0),
                elapsedMs,
                true
        );
        return root.path("choices").get(0).path("message").path("content").asText();
    }

    private String systemPrompt() {
        return """
                You are TradePilot AI, a strict but practical futures trading session coach.
                Return ONLY valid JSON. Do not use markdown. Do not include extra fields.

                JSON shape:
                {
                  "summary": "string",
                  "whatWentWell": "string",
                  "whatHurtPerformance": "string",
                  "biggestMistake": "string",
                  "bestSetup": "string",
                  "worstDecision": "string",
                  "emotionalPattern": "string",
                  "executionPattern": "string",
                  "riskManagementPattern": "string",
                  "oneRuleForTomorrow": "string"
                }
                """;
    }

    private String prompt(
            LocalDate sessionDate,
            List<TradeResponse> trades,
            BigDecimal totalPnl,
            BigDecimal winRate,
            Map<String, Long> mistakeSummary,
            Integer disciplineScore
    ) {
        return """
                Review this full trading session/day.

                Date: %s
                Total P&L: %s
                Win rate: %s%%
                Discipline score: %s/100
                Mistake summary: %s

                Trades in timeline order:
                %s

	                Identify:
	                - session summary
	                - what went well
	                - what hurt performance
	                - Setup Analysis if setup metadata exists
	                - Emotion Analysis if emotion metadata exists
	                - Confidence Analysis if confidence metadata exists
	                - best trade
                - worst trade
                - biggest mistake
                - emotional pattern
                - execution pattern
                - risk management pattern
                - worst decision
                - one clear rule for tomorrow

                Be concise, direct, and specific to the trades.
                """.formatted(sessionDate, totalPnl, winRate, disciplineScore, mistakeSummary, trades);
    }

    private AiSessionReviewResponse fallback(
            LocalDate sessionDate,
            List<TradeResponse> trades,
            BigDecimal totalPnl,
            BigDecimal winRate,
            Map<String, Long> mistakeSummary,
            Integer disciplineScore
    ) {
        String dominantMistake = mistakeSummary.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("No repeated mistake tagged");

        AiSessionReviewResponse response = new AiSessionReviewResponse();
        response.setSummary("Session review for " + sessionDate + ": " + trades.size() + " trades, " + totalPnl + " total P&L, " + winRate + "% win rate.");
        response.setWhatWentWell(totalPnl.compareTo(BigDecimal.ZERO) >= 0
                ? "You finished the session green and created enough positive expectancy to offset mistakes."
                : "You captured useful data from the session; focus on reducing avoidable losses before increasing size.");
        response.setWhatHurtPerformance(dominantMistake);
        response.setBiggestMistake(dominantMistake);
        response.setBestSetup(trades.stream().filter(t -> t.getPnlAmount() != null && t.getPnlAmount().compareTo(BigDecimal.ZERO) > 0).findFirst().map(t -> String.valueOf(t.getSetup())).orElse("No winning setup stood out."));
        response.setWorstDecision(disciplineScore < 80 ? "Discipline leaks lowered the session quality." : "No major discipline decision was detected from the available data.");
        response.setEmotionalPattern(trades.size() >= 4
                ? "Watch for decision quality fading later in the session as trade count increases."
                : "The available trades do not show a clear emotional pattern yet.");
        response.setExecutionPattern("Review whether each entry waited for confirmation before continuation.");
        response.setRiskManagementPattern("Keep contract size consistent until the session proves stable.");
        response.setOneRuleForTomorrow("Take the first A setup only after confirmation, then pause for two minutes before the next trade.");
        return response;
    }
}
