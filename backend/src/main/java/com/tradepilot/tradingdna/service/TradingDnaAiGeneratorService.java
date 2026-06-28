package com.tradepilot.tradingdna.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradepilot.config.OpenAiProperties;
import com.tradepilot.observability.ObservabilityService;
import com.tradepilot.tradingdna.dto.TradingDnaAiResult;
import com.tradepilot.tradingdna.dto.TradingDnaContext;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
public class TradingDnaAiGeneratorService {
    @java.lang.SuppressWarnings("all")
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(TradingDnaAiGeneratorService.class);
    private static final int MAX_RETRIES = 2;
    private static final long RETRY_DELAY_MS = 700;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final OpenAiProperties openAiProperties;
    private final TradingDnaPromptBuilder promptBuilder;
    private final ObservabilityService observabilityService;

    public TradingDnaAiResult generate(TradingDnaContext context) throws Exception {
        if (openAiProperties.getApiKey() == null || openAiProperties.getApiKey().isBlank()) {
            log.debug("Trading DNA OpenAI call skipped; API key missing. userId={}, accountId={}, totalTrades={}", context.getUserId(), context.getAccountId(), context.getTotalTrades());
            return fallback(context);
        }
        Exception lastException = null;
        for (int attempt = 1; attempt <= MAX_RETRIES + 1; attempt++) {
            try {
                return callOpenAi(context, attempt);
            } catch (Exception e) {
                lastException = e;
                log.warn("Trading DNA AI generation failed. attempt={}, maxAttempts={}", attempt, MAX_RETRIES + 1, e);
                if (attempt <= MAX_RETRIES) {
                    Thread.sleep(RETRY_DELAY_MS * attempt);
                }
            }
        }
        throw lastException;
    }

    private TradingDnaAiResult callOpenAi(TradingDnaContext context, int attempt) throws Exception {
        Instant start = Instant.now();
        log.debug("Trading DNA OpenAI call start. userId={}, accountId={}, attempt={}, model={}, totalTrades={}", context.getUserId(), context.getAccountId(), attempt, openAiProperties.getModel(), context.getTotalTrades());
        String prompt = promptBuilder.buildPrompt(context);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(openAiProperties.getApiKey());
        Map<String, Object> body = Map.of("model", openAiProperties.getModel(), "messages", List.of(Map.of("role", "system", "content", promptBuilder.systemPrompt()), Map.of("role", "user", "content", prompt)), "temperature", 0.25);
        ResponseEntity<String> response;
        try {
            response = restTemplate.exchange("https://api.openai.com/v1/chat/completions", HttpMethod.POST, new HttpEntity<>(body, headers), String.class);
        } catch (Exception ex) {
            long elapsedMs = Duration.between(start, Instant.now()).toMillis();
            log.warn("Trading DNA OpenAI call failed. userId={}, accountId={}, attempt={}, elapsedMs={}", context.getUserId(), context.getAccountId(), attempt, elapsedMs, ex);
            observabilityService.recordAiCall(context.getUserId(), "TRADING_DNA", openAiProperties.getModel(), 0, 0, 0, elapsedMs, false);
            throw ex;
        }
        long elapsedMs = Duration.between(start, Instant.now()).toMillis();
        JsonNode root = objectMapper.readTree(response.getBody());
        JsonNode usage = root.path("usage");
        int promptTokens = usage.path("prompt_tokens").asInt(0);
        int completionTokens = usage.path("completion_tokens").asInt(0);
        int totalTokens = usage.path("total_tokens").asInt(0);
        log.info("Trading DNA AI response received. userId={}, accountId={}, attempt={}, elapsedMs={}, totalTokens={}", context.getUserId(), context.getAccountId(), attempt, elapsedMs, totalTokens);
        observabilityService.recordAiCall(context.getUserId(), "TRADING_DNA", openAiProperties.getModel(), promptTokens, completionTokens, totalTokens, elapsedMs, true);
        String content = root.path("choices").get(0).path("message").path("content").asText();
        return objectMapper.readValue(cleanJson(content), TradingDnaAiResult.class);
    }

    private String cleanJson(String content) {
        if (content == null) {
            return "{}";
        }
        return content.replace("```json", "").replace("```", "").trim();
    }

    private TradingDnaAiResult fallback(TradingDnaContext context) {
        return TradingDnaAiResult.of(
                defaultList(context.getBestSetup(), "Best setup archetype: "),
                context.getDeterministicSignals(),
                List.of("Watch for impulsive trades after losses and clustered entries."),
                List.of("Keep position size and risk per trade consistent across sessions."),
                defaultList(context.getBestSession(), "Best session: "),
                defaultList(context.getWorstSession(), "Weakest session: "),
                context.getMistakeCounts().entrySet().stream().limit(5).map(entry -> entry.getKey() + " (" + entry.getValue() + "x)").toList(),
                buildTrend(context),
                context.getDeterministicConsistencyScore(),
                context.getDeterministicDisciplineScore(),
                context.getDeterministicExecutionScore(),
                context.getDeterministicPatienceScore(),
                "Confidence is healthiest when it follows your strongest setup and session data.",
                "Focus the next block on one high-quality setup, fixed risk, and a pause after every loss."
        );
    }

    private List<String> defaultList(String value, String prefix) {
        if (value == null || value.isBlank() || "Not enough data".equals(value)) {
            return List.of();
        }
        return List.of(prefix + value);
    }

    private String buildTrend(TradingDnaContext context) {
        if (context.getTotalTrades() < 5) {
            return "Not enough trades yet to measure a reliable improvement trend.";
        }
        return "Consistency trend is based on discipline, win-rate, and emotional stability over the selected timeframe.";
    }

    @java.lang.SuppressWarnings("all")
    public TradingDnaAiGeneratorService(final RestTemplate restTemplate, final ObjectMapper objectMapper, final OpenAiProperties openAiProperties, final TradingDnaPromptBuilder promptBuilder, final ObservabilityService observabilityService) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.openAiProperties = openAiProperties;
        this.promptBuilder = promptBuilder;
        this.observabilityService = observabilityService;
    }
}
