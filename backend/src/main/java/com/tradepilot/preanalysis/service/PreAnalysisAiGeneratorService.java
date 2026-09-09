package com.tradepilot.preanalysis.service;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.tradepilot.config.OpenAiProperties;
import com.tradepilot.observability.ObservabilityService;
import com.tradepilot.preanalysis.dto.PreAnalysisDtos.*;
import com.tradepilot.preanalysis.dto.TradingPlaybookDto;
import com.tradepilot.preanalysis.entity.*;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Service
public class PreAnalysisAiGeneratorService {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(PreAnalysisAiGeneratorService.class);
    private static final int MAX_RETRIES = 2;
    private static final long RETRY_DELAY_MS = 700;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final OpenAiProperties openAiProperties;
    private final PreAnalysisPromptBuilder promptBuilder;
    private final ObservabilityService observabilityService;
    private final PreAnalysisDecisionService decisionService;

    public PreAnalysisAiResult generate(Long userId, TradingPlaybookDto playbook, PreAnalysisMarket market, PreAnalysisTimeframe timeframe, String traderNote, byte[] imageBytes, String contentType) throws Exception {
        if (openAiProperties.getApiKey() == null || openAiProperties.getApiKey().isBlank()) {
            return fallback(playbook, market, timeframe);
        }
        Exception lastException = null;
        for (int attempt = 1; attempt <= MAX_RETRIES + 1; attempt++) {
            try {
                return callOpenAi(userId, playbook, market, timeframe, traderNote, imageBytes, contentType, attempt > 1);
            } catch (Exception ex) {
                lastException = ex;
                boolean retryable = isRetryable(ex, attempt);
                if (isContractFailure(ex)) {
                    log.warn("Pre-analysis response contract failure. userId={}, fieldPath={}, failureCategory={}, attempt={}, retryable=false", userId, fieldPath(ex), rootException(ex).getClass().getSimpleName(), attempt);
                } else {
                    log.warn("Pre-analysis AI generation failed. userId={}, attempt={}, maxAttempts={}, retryable={}", userId, attempt, MAX_RETRIES + 1, retryable, ex);
                }
                if (retryable && attempt <= MAX_RETRIES) {
                    Thread.sleep(RETRY_DELAY_MS * attempt);
                } else {
                    break;
                }
            }
        }
        if (isContractFailure(lastException)) {
            return fallback(playbook, market, timeframe, "AI response could not be safely interpreted, so TradePilot returned a no-trade fallback.");
        }
        throw lastException;
    }

    private PreAnalysisAiResult callOpenAi(Long userId, TradingPlaybookDto playbook, PreAnalysisMarket market, PreAnalysisTimeframe timeframe, String traderNote, byte[] imageBytes, String contentType, boolean retry) throws Exception {
        Instant start = Instant.now();
        long startedAt = System.nanoTime();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(openAiProperties.getApiKey());
        long headersReadyAt = System.nanoTime();
        String prompt = promptBuilder.userPrompt(playbook, market, timeframe, traderNote, retry);
        long promptReadyAt = System.nanoTime();
        String imageUrl = "data:" + contentType + ";base64," + Base64.getEncoder().encodeToString(imageBytes);
        long imageEncodedAt = System.nanoTime();
        Map<String, Object> userMessage = Map.of(
                "role", "user",
                "content", List.of(
                        Map.of("type", "text", "text", prompt),
                        Map.of("type", "image_url", "image_url", Map.of("url", imageUrl))
                )
        );
        Map<String, Object> body = Map.of(
                "model", openAiProperties.getModel(),
                "messages", List.of(Map.of("role", "system", "content", promptBuilder.systemPrompt()), userMessage),
                "temperature", 0.15,
                "response_format", Map.of("type", "json_object")
        );
        ResponseEntity<String> response;
        try {
            response = restTemplate.exchange("https://api.openai.com/v1/chat/completions", HttpMethod.POST, new HttpEntity<>(body, headers), String.class);
        } catch (Exception ex) {
            observabilityService.recordAiCall(userId, "AI_PRE_ANALYSIS", openAiProperties.getModel(), 0, 0, 0, Duration.between(start, Instant.now()).toMillis(), false);
            log.info("pre_analysis_openai_timing userId={} retry={} success=false headersMs={} promptMs={} imageEncodingMs={} openAiRequestMs={} totalMs={}", userId, retry, ms(startedAt, headersReadyAt), ms(headersReadyAt, promptReadyAt), ms(promptReadyAt, imageEncodedAt), Duration.between(start, Instant.now()).toMillis(), ms(startedAt, System.nanoTime()));
            throw ex;
        }
        long responseReadyAt = System.nanoTime();
        long elapsedMs = Duration.between(start, Instant.now()).toMillis();
        JsonNode root = objectMapper.readTree(response.getBody());
        JsonNode usage = root.path("usage");
        int promptTokens = usage.path("prompt_tokens").asInt(0);
        int completionTokens = usage.path("completion_tokens").asInt(0);
        int totalTokens = usage.path("total_tokens").asInt(0);
        String content = root.path("choices").get(0).path("message").path("content").asText();
        try {
            PreAnalysisAiResult result = objectMapper.readValue(cleanJson(content), PreAnalysisAiResult.class);
            long parsedAt = System.nanoTime();
            PreAnalysisAiResult normalized = decisionService.normalizeAndValidate(result);
            long normalizedAt = System.nanoTime();
            observabilityService.recordAiCall(userId, "AI_PRE_ANALYSIS", openAiProperties.getModel(), promptTokens, completionTokens, totalTokens, elapsedMs, true);
            log.info("pre_analysis_openai_timing userId={} retry={} success=true headersMs={} promptMs={} imageEncodingMs={} openAiRequestMs={} parseMs={} normalizeMs={} totalMs={} promptTokens={} completionTokens={} totalTokens={}", userId, retry, ms(startedAt, headersReadyAt), ms(headersReadyAt, promptReadyAt), ms(promptReadyAt, imageEncodedAt), ms(imageEncodedAt, responseReadyAt), ms(responseReadyAt, parsedAt), ms(parsedAt, normalizedAt), ms(startedAt, normalizedAt), promptTokens, completionTokens, totalTokens);
            return normalized;
        } catch (Exception ex) {
            observabilityService.recordAiCall(userId, "AI_PRE_ANALYSIS", openAiProperties.getModel(), promptTokens, completionTokens, totalTokens, elapsedMs, false);
            log.info("pre_analysis_openai_timing userId={} retry={} success=false headersMs={} promptMs={} imageEncodingMs={} openAiRequestMs={} parseAndNormalizeMs={} totalMs={} promptTokens={} completionTokens={} totalTokens={}", userId, retry, ms(startedAt, headersReadyAt), ms(headersReadyAt, promptReadyAt), ms(promptReadyAt, imageEncodedAt), ms(imageEncodedAt, responseReadyAt), ms(responseReadyAt, System.nanoTime()), ms(startedAt, System.nanoTime()), promptTokens, completionTokens, totalTokens);
            throw ex;
        }
    }

    public void validate(PreAnalysisAiResult result) {
        decisionService.validate(result);
    }

    private String cleanJson(String content) {
        if (content == null) {
            return "{}";
        }
        return content.replace("```json", "").replace("```", "").trim();
    }

    private PreAnalysisAiResult fallback(TradingPlaybookDto playbook, PreAnalysisMarket market, PreAnalysisTimeframe timeframe) {
        return fallback(playbook, market, timeframe, "AI provider is not configured, so TradePilot cannot read the chart screenshot.");
    }

    private PreAnalysisAiResult fallback(TradingPlaybookDto playbook, PreAnalysisMarket market, PreAnalysisTimeframe timeframe, String reason) {
        return new PreAnalysisAiResult(
                market,
                PreAnalysisPromptBuilder.displayTimeframe(timeframe),
                playbook.name(),
                "UNCLEAR",
                0.0,
                new DirectionalContext(Bias.NEUTRAL, "LOW", reason),
                SetupStatus.NO_TRADE,
                new Conditions(
                        new ConditionEvidence(ConditionStatus.NOT_APPLICABLE, "Chart analysis unavailable."),
                        new ConditionEvidence(ConditionStatus.NOT_APPLICABLE, "Chart analysis unavailable."),
                        new ConditionEvidence(ConditionStatus.NOT_APPLICABLE, "Chart analysis unavailable."),
                        new ConditionEvidence(ConditionStatus.NOT_APPLICABLE, "Chart analysis unavailable."),
                        new ConditionEvidence(ConditionStatus.NOT_APPLICABLE, "Chart analysis unavailable."),
                        new ConditionEvidence(ConditionStatus.NOT_APPLICABLE, "Chart analysis unavailable.")
                ),
                0,
                List.of(),
                new EntryAssessment(EntryAssessmentStatus.NO_TRADE, "No trade. TradePilot could not produce a safe chart interpretation.", List.of("Upload a clearer chart or try again.")),
                new Invalidation("No structural invalidation can be assessed without chart analysis.", List.of()),
                List.of(),
                List.of(),
                List.of(reason, "Do not treat this as a market read."),
                reason,
                new MarketRead("NEUTRAL", "UNCLEAR", reason, "NO_TRADE"),
                null,
                List.of(),
                new Scenario("NOT_AVAILABLE", "Bullish path cannot be assessed without a safe chart interpretation.", null, null, null, List.of(), null, List.of(), "POOR", List.of("Try again with a readable chart screenshot.")),
                new Scenario("NOT_AVAILABLE", "Bearish path cannot be assessed without a safe chart interpretation.", null, null, null, List.of(), null, List.of(), "POOR", List.of("Try again with a readable chart screenshot.")),
                new NoTradeScenario("TradePilot could not safely interpret the screenshot.", "No chart interpretation is available, so TradePilot cannot produce a setup plan."),
                new TradePlan("WAIT_FOR_MORE_DATA", "Upload a readable ES/NQ chart screenshot and try again.", List.of(), List.of("Do not treat this fallback as market analysis."), null, null),
                new CrossMarket(false, market.name(), market == PreAnalysisMarket.ES ? "NQ" : "ES", "UNKNOWN", "UNKNOWN", "NOT_ENOUGH_DATA", "NOT_ENOUGH_DATA", "Correlated-market context was not supplied.")
        );
    }

    private long ms(long start, long end) {
        return Duration.ofNanos(end - start).toMillis();
    }

    private boolean isRetryable(Exception ex, int attempt) {
        Throwable root = rootException(ex);
        if (isContractFailure(root)) {
            return false;
        }
        if (root instanceof JsonParseException) {
            return attempt == 1;
        }
        if (root instanceof ResourceAccessException || root instanceof HttpServerErrorException) {
            return true;
        }
        if (root instanceof HttpClientErrorException.TooManyRequests) {
            return true;
        }
        if (root instanceof HttpClientErrorException) {
            return false;
        }
        if (root instanceof JsonProcessingException) {
            return attempt == 1;
        }
        return false;
    }

    private boolean isContractFailure(Throwable ex) {
        Throwable root = rootException(ex);
        return root instanceof MismatchedInputException
                || root instanceof InvalidFormatException
                || root instanceof IllegalArgumentException;
    }

    private String fieldPath(Exception ex) {
        Throwable root = rootException(ex);
        if (!(root instanceof JsonMappingException mappingException) || mappingException.getPath().isEmpty()) {
            return "unknown";
        }
        return mappingException.getPath().stream()
                .map(reference -> reference.getFieldName() != null ? reference.getFieldName() : "[" + reference.getIndex() + "]")
                .reduce((left, right) -> left + "." + right)
                .orElse("unknown");
    }

    private Throwable rootException(Throwable ex) {
        Throwable current = ex;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        return current;
    }

    public PreAnalysisAiGeneratorService(RestTemplate restTemplate, ObjectMapper objectMapper, OpenAiProperties openAiProperties, PreAnalysisPromptBuilder promptBuilder, ObservabilityService observabilityService, PreAnalysisDecisionService decisionService) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.openAiProperties = openAiProperties;
        this.promptBuilder = promptBuilder;
        this.observabilityService = observabilityService;
        this.decisionService = decisionService;
    }
}
