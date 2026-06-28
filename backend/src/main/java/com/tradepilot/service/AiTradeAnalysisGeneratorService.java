package com.tradepilot.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradepilot.dto.ai.AiTradeAnalysisRequest;
import com.tradepilot.dto.ai.AiTradeAnalysisResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.*;

@Service
public class AiTradeAnalysisGeneratorService {
    @java.lang.SuppressWarnings("all")
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AiTradeAnalysisGeneratorService.class);
    private final ObjectMapper objectMapper;
    private final AiTradeAnalysisPromptBuilder promptBuilder;
    private final RestTemplate restTemplate = new RestTemplate();
    @Value("${app.openai.api-key:}")
    private String openAiApiKey;
    @Value("${app.openai.model:gpt-4.1-mini}")
    private String model;

    public AiTradeAnalysisResponse generate(AiTradeAnalysisRequest request, byte[] imageBytes, String mimeType) {
        int maxAttempts = 3;
        RuntimeException lastException = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return callOpenAi(request, imageBytes, mimeType, attempt);
            } catch (RuntimeException e) {
                lastException = e;
                log.warn("OpenAI trade analysis attempt failed. attempt={}, maxAttempts={}, tradeId={}, error={}", attempt, maxAttempts, request.getTradeId(), e.getMessage());
                if (attempt == maxAttempts) {
                    log.error("OpenAI trade analysis permanently failed. tradeId={}", request.getTradeId(), e);
                    break;
                }
                try {
                    Thread.sleep(1000L * attempt);
                } catch (InterruptedException interruptedException) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("AI trade analysis interrupted", interruptedException);
                }
            }
        }
        throw lastException == null ? new RuntimeException("AI trade analysis failed") : lastException;
    }

    private AiTradeAnalysisResponse callOpenAi(AiTradeAnalysisRequest request, byte[] imageBytes, String mimeType, int attempt) {
        try {
            long start = System.currentTimeMillis();
            boolean hasImage = imageBytes != null && imageBytes.length > 0;
            String prompt = promptBuilder.build(request, hasImage);
            List<Map<String, Object>> content = new ArrayList<>();
            content.add(Map.of("type", "text", "text", prompt));
            if (hasImage) {
                String safeMimeType = mimeType == null || mimeType.isBlank() ? "image/png" : mimeType;
                String base64 = Base64.getEncoder().encodeToString(imageBytes);
                content.add(Map.of("type", "image_url", "image_url", Map.of("url", "data:" + safeMimeType + ";base64," + base64)));
            }
            Map<String, Object> body = Map.of("model", model, "messages", List.of(Map.of("role", "user", "content", content)), "temperature", 0.2, "response_format", Map.of("type", "json_object"));
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(resolveApiKey());
            ResponseEntity<Map> response = restTemplate.exchange("https://api.openai.com/v1/chat/completions", HttpMethod.POST, new HttpEntity<>(body, headers), Map.class);
            Map<?, ?> usage = (Map<?, ?>) response.getBody().get("usage");
            log.info("OpenAI trade analysis response received. tradeId={}, attempt={}, status={}, elapsedMs={}, promptTokens={}, completionTokens={}, totalTokens={}", request.getTradeId(), attempt, response.getStatusCode(), System.currentTimeMillis() - start, usage == null ? null : usage.get("prompt_tokens"), usage == null ? null : usage.get("completion_tokens"), usage == null ? null : usage.get("total_tokens"));
            Map<?, ?> choice = (Map<?, ?>) ((List<?>) response.getBody().get("choices")).get(0);
            Map<?, ?> message = (Map<?, ?>) choice.get("message");
            String contentText = (String) message.get("content");
            return objectMapper.readValue(contentText, AiTradeAnalysisResponse.class);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    private String resolveApiKey() {
        String envKey = System.getenv("OPENAI_API_KEY");
        if (envKey != null && !envKey.isBlank()) return envKey;
        if (openAiApiKey != null && !openAiApiKey.isBlank()) return openAiApiKey;
        throw new IllegalStateException("OPENAI_API_KEY is missing");
    }

    @java.lang.SuppressWarnings("all")
    public AiTradeAnalysisGeneratorService(final ObjectMapper objectMapper, final AiTradeAnalysisPromptBuilder promptBuilder) {
        this.objectMapper = objectMapper;
        this.promptBuilder = promptBuilder;
    }
}
