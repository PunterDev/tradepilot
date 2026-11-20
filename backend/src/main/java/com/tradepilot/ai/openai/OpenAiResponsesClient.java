package com.tradepilot.ai.openai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradepilot.config.OpenAiProperties;
import com.tradepilot.observability.ObservabilityService;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.Instant;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The only gateway for generated TradePilot analysis. Embeddings deliberately use their
 * dedicated endpoint and do not pass through this client.
 */
@Component
public class OpenAiResponsesClient {
    private static final String RESPONSES_URL = "https://api.openai.com/v1/responses";
    private static final String CHAT_COMPLETIONS_URL = "https://api.openai.com/v1/chat/completions";
    private static final int MAX_ATTEMPTS = 3;
    private static final long RETRY_DELAY_MS = 500;
    private static final Set<String> GPT6_LUNA_REASONING_EFFORTS = Set.of("none", "low", "medium", "high", "xhigh", "max");

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final OpenAiProperties properties;
    private final ObservabilityService observabilityService;

    public OpenAiResponsesClient(
            RestTemplate restTemplate,
            ObjectMapper objectMapper,
            OpenAiProperties properties,
            ObservabilityService observabilityService
    ) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.observabilityService = observabilityService;
    }

    public boolean isConfigured() {
        return properties.getApiKey() != null && !properties.getApiKey().isBlank();
    }

    public OpenAiGeneratedResponse generateJson(
            Long userId,
            String feature,
            String systemPrompt,
            String userPrompt
    ) {
        return generateJson(userId, feature, systemPrompt, userPrompt, null, null);
    }

    public OpenAiGeneratedResponse generateJson(
            Long userId,
            String feature,
            String systemPrompt,
            String userPrompt,
            byte[] imageBytes,
            String imageMimeType
    ) {
        if (!isConfigured()) {
            throw new OpenAiUnavailableException("OpenAI is not configured");
        }
        validateConfiguration();

        RuntimeException lastFailure = null;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            Instant started = Instant.now();
            try {
                OpenAiGeneratedResponse result = isResponsesModel()
                        ? requestResponses(systemPrompt, userPrompt, imageBytes, imageMimeType)
                        : requestLegacyChatCompletions(systemPrompt, userPrompt, imageBytes, imageMimeType);
                observabilityService.recordAiCall(
                        userId,
                        feature,
                        properties.getModel(),
                        result.usage(),
                        estimatedCost(result.usage()),
                        Duration.between(started, Instant.now()).toMillis(),
                        attempt,
                        true,
                        null
                );
                return result;
            } catch (RuntimeException failure) {
                lastFailure = failure;
                observabilityService.recordAiCall(
                        userId,
                        feature,
                        properties.getModel(),
                        OpenAiUsage.empty(),
                        BigDecimal.ZERO,
                        Duration.between(started, Instant.now()).toMillis(),
                        attempt,
                        false,
                        errorCategory(failure)
                );
                if (!isRetryable(failure) || attempt == MAX_ATTEMPTS) {
                    break;
                }
                sleep(attempt);
            }
        }
        throw lastFailure == null ? new OpenAiUnavailableException("OpenAI request failed") : lastFailure;
    }

    private OpenAiGeneratedResponse requestResponses(String systemPrompt, String userPrompt, byte[] imageBytes, String imageMimeType) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", properties.getModel());
        body.put("store", false);
        body.put("max_output_tokens", properties.getMaxOutputTokens());
        body.put("reasoning", Map.of("effort", properties.getReasoningEffort()));
        body.put("text", Map.of("format", Map.of("type", "json_object")));
        body.put("input", List.of(
                Map.of("role", "developer", "content", List.of(Map.of("type", "input_text", "text", systemPrompt))),
                Map.of("role", "user", "content", inputContent(userPrompt, imageBytes, imageMimeType, true))
        ));
        JsonNode root = exchange(RESPONSES_URL, body);
        if (!"completed".equals(root.path("status").asText())) {
            throw new OpenAiUnavailableException("OpenAI response was not completed: " + safeStatus(root));
        }
        String output = responseOutput(root);
        if (output.isBlank()) {
            throw new OpenAiUnavailableException("OpenAI returned no usable output");
        }
        return new OpenAiGeneratedResponse(output, responseUsage(root));
    }

    private OpenAiGeneratedResponse requestLegacyChatCompletions(String systemPrompt, String userPrompt, byte[] imageBytes, String imageMimeType) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", properties.getModel());
        body.put("messages", List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", inputContent(userPrompt, imageBytes, imageMimeType, false))
        ));
        body.put("temperature", properties.getLegacyTemperature());
        body.put("max_tokens", properties.getMaxOutputTokens());
        body.put("response_format", Map.of("type", "json_object"));
        JsonNode root = exchange(CHAT_COMPLETIONS_URL, body);
        String output = root.path("choices").path(0).path("message").path("content").asText("");
        if (output.isBlank()) {
            throw new OpenAiUnavailableException("OpenAI returned no usable output");
        }
        JsonNode usage = root.path("usage");
        return new OpenAiGeneratedResponse(output, new OpenAiUsage(
                usage.path("prompt_tokens").asInt(0),
                0,
                usage.path("completion_tokens").asInt(0),
                0,
                usage.path("total_tokens").asInt(0)
        ));
    }

    private JsonNode exchange(String url, Map<String, Object> body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(properties.getApiKey());
        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, new HttpEntity<>(body, headers), String.class);
            return objectMapper.readTree(response.getBody());
        } catch (Exception exception) {
            if (exception instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new OpenAiUnavailableException("OpenAI response could not be read", exception);
        }
    }

    private List<Map<String, Object>> inputContent(String prompt, byte[] imageBytes, String imageMimeType, boolean responses) {
        List<Map<String, Object>> content = new ArrayList<>();
        content.add(responses ? Map.of("type", "input_text", "text", prompt) : Map.of("type", "text", "text", prompt));
        if (imageBytes != null && imageBytes.length > 0) {
            String mime = imageMimeType == null || imageMimeType.isBlank() ? "image/png" : imageMimeType;
            String url = "data:" + mime + ";base64," + java.util.Base64.getEncoder().encodeToString(imageBytes);
            content.add(responses
                    ? Map.of("type", "input_image", "image_url", url)
                    : Map.of("type", "image_url", "image_url", Map.of("url", url)));
        }
        return content;
    }

    private String responseOutput(JsonNode root) {
        String topLevel = root.path("output_text").asText("");
        if (!topLevel.isBlank()) {
            return topLevel;
        }
        for (JsonNode item : root.path("output")) {
            if (!"message".equals(item.path("type").asText())) {
                continue;
            }
            for (JsonNode content : item.path("content")) {
                if ("output_text".equals(content.path("type").asText()) && !content.path("text").asText("").isBlank()) {
                    return content.path("text").asText();
                }
            }
        }
        return "";
    }

    private OpenAiUsage responseUsage(JsonNode root) {
        JsonNode usage = root.path("usage");
        return new OpenAiUsage(
                usage.path("input_tokens").asInt(0),
                usage.path("input_tokens_details").path("cached_tokens").asInt(0),
                usage.path("output_tokens").asInt(0),
                usage.path("output_tokens_details").path("reasoning_tokens").asInt(0),
                usage.path("total_tokens").asInt(0)
        );
    }

    private boolean isResponsesModel() {
        return !properties.isLegacyChatCompletionsEnabled();
    }

    private void validateConfiguration() {
        if (properties.isLegacyChatCompletionsEnabled()) {
            return;
        }
        if (!"gpt-6-luna".equals(properties.getModel())
                || !GPT6_LUNA_REASONING_EFFORTS.contains(properties.getReasoningEffort())
                || properties.getMaxOutputTokens() < 1
                || properties.getMaxOutputTokens() > 128_000) {
            throw new OpenAiUnavailableException("The configured OpenAI Responses model and reasoning effort are not supported");
        }
    }

    private boolean isRetryable(RuntimeException exception) {
        return exception instanceof ResourceAccessException
                || exception instanceof HttpServerErrorException
                || exception instanceof HttpClientErrorException.TooManyRequests;
    }

    private void sleep(int attempt) {
        try {
            Thread.sleep(RETRY_DELAY_MS * attempt);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new OpenAiUnavailableException("OpenAI retry was interrupted", interrupted);
        }
    }

    private String safeStatus(JsonNode root) {
        String detail = root.path("incomplete_details").path("reason").asText("");
        return detail.isBlank() ? root.path("status").asText("unknown") : detail;
    }

    private String errorCategory(RuntimeException exception) {
        if (exception instanceof HttpClientErrorException.TooManyRequests) return "RATE_LIMIT";
        if (exception instanceof HttpClientErrorException) return "CLIENT_ERROR";
        if (exception instanceof HttpServerErrorException) return "SERVER_ERROR";
        if (exception instanceof ResourceAccessException) return "NETWORK";
        return "RESPONSE_CONTRACT";
    }

    private BigDecimal estimatedCost(OpenAiUsage usage) {
        BigDecimal input = BigDecimal.valueOf(Math.max(0, usage.inputTokens() - usage.cachedInputTokens()))
                .multiply(properties.getInputCostPerMillionTokens());
        BigDecimal cached = BigDecimal.valueOf(Math.max(0, usage.cachedInputTokens()))
                .multiply(properties.getCachedInputCostPerMillionTokens());
        BigDecimal output = BigDecimal.valueOf(Math.max(0, usage.outputTokens()))
                .multiply(properties.getOutputCostPerMillionTokens());
        // OpenAI reports reasoning tokens as part of output_tokens, so pricing them again would double-count.
        return input.add(cached).add(output).divide(BigDecimal.valueOf(1_000_000), 6, RoundingMode.HALF_UP);
    }
}
