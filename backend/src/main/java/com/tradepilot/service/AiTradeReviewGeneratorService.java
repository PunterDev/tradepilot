package com.tradepilot.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradepilot.config.OpenAiProperties;
import com.tradepilot.dto.ai.AiTradeReviewRequest;
import com.tradepilot.dto.ai.AiTradeReviewResponse;
import com.tradepilot.observability.ObservabilityService;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
public class AiTradeReviewGeneratorService {
    @java.lang.SuppressWarnings("all")
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AiTradeReviewGeneratorService.class);
    private static final int MAX_RETRIES = 2;
    private static final long RETRY_DELAY_MS = 700;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final OpenAiProperties openAiProperties;
    private final AiTradeReviewPromptBuilder promptBuilder;
    private final ObservabilityService observabilityService;

    public AiTradeReviewResponse generateFreshReview(AiTradeReviewRequest request, Long userId) throws Exception {
        if (openAiProperties.getApiKey() == null || openAiProperties.getApiKey().isBlank()) {
            return fallbackReview("OpenAI API key is missing. Using fallback review.");
        }
        return callOpenAiWithRetry(request, userId);
    }

    private AiTradeReviewResponse callOpenAiWithRetry(AiTradeReviewRequest request, Long userId) throws Exception {
        Exception lastException = null;
        for (int attempt = 1; attempt <= MAX_RETRIES + 1; attempt++) {
            try {
                return callOpenAi(request, attempt, userId);
            } catch (Exception e) {
                lastException = e;
                log.warn("OpenAI trade review attempt failed. attempt={}, maxAttempts={}, tradeId={}", attempt, MAX_RETRIES + 1, request.getTradeId(), e);
                if (attempt <= MAX_RETRIES) {
                    Thread.sleep(RETRY_DELAY_MS * attempt);
                }
            }
        }
        throw lastException;
    }

    private AiTradeReviewResponse callOpenAi(AiTradeReviewRequest request, int attempt, Long userId) throws Exception {
        Instant start = Instant.now();
        String prompt = promptBuilder.buildPrompt(request);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(openAiProperties.getApiKey());
        Map<String, Object> body = Map.of("model", openAiProperties.getModel(), "messages", List.of(Map.of("role", "system", "content", promptBuilder.systemPrompt()), Map.of("role", "user", "content", prompt)), "temperature", 0.3);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
        ResponseEntity<String> response;
        try {
            response = restTemplate.exchange("https://api.openai.com/v1/chat/completions", HttpMethod.POST, entity, String.class);
        } catch (Exception ex) {
            observabilityService.recordAiCall(userId, "AI_REVIEW", openAiProperties.getModel(), 0, 0, 0, Duration.between(start, Instant.now()).toMillis(), false);
            throw ex;
        }
        long elapsedMs = Duration.between(start, Instant.now()).toMillis();
        JsonNode root = objectMapper.readTree(response.getBody());
        JsonNode usage = root.path("usage");
        int promptTokens = usage.path("prompt_tokens").asInt(0);
        int completionTokens = usage.path("completion_tokens").asInt(0);
        int totalTokens = usage.path("total_tokens").asInt(0);
        log.info("OpenAI trade review response received. tradeId={}, attempt={}, status={}, elapsedMs={}, promptTokens={}, completionTokens={}, totalTokens={}", request.getTradeId(), attempt, response.getStatusCode(), elapsedMs, promptTokens, completionTokens, totalTokens);
        observabilityService.recordAiCall(userId, "AI_REVIEW", openAiProperties.getModel(), promptTokens, completionTokens, totalTokens, elapsedMs, true);
        String content = root.path("choices").get(0).path("message").path("content").asText();
        return objectMapper.readValue(content, AiTradeReviewResponse.class);
    }

    private AiTradeReviewResponse fallbackReview(String reason) {
        return AiTradeReviewResponse.builder().grade("B-").summary(reason).mistakes(List.of("Possible impatience or FOMO based on journal notes.", "Setup quality may not have been strong enough.")).strengths(List.of("You documented the trade clearly.", "You identified the emotional issue after the trade.")).improvementPlan(List.of("Wait for clear confirmation before entering.", "Avoid trading when mood is anxious.", "Only take A or A+ setups during discipline reset.")).tags(List.of("Poor Patience", "FOMO")).build();
    }

    @java.lang.SuppressWarnings("all")
    public AiTradeReviewGeneratorService(final RestTemplate restTemplate, final ObjectMapper objectMapper, final OpenAiProperties openAiProperties, final AiTradeReviewPromptBuilder promptBuilder, final ObservabilityService observabilityService) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.openAiProperties = openAiProperties;
        this.promptBuilder = promptBuilder;
        this.observabilityService = observabilityService;
    }
}
