package com.tradepilot.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradepilot.ai.openai.OpenAiResponsesClient;
import com.tradepilot.config.OpenAiProperties;
import com.tradepilot.dto.ai.AiTradeReviewRequest;
import com.tradepilot.dto.ai.AiTradeReviewResponse;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class AiTradeReviewGeneratorService {
    @java.lang.SuppressWarnings("all")
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AiTradeReviewGeneratorService.class);
    private final ObjectMapper objectMapper;
    private final OpenAiResponsesClient openAiResponsesClient;
    private final AiTradeReviewPromptBuilder promptBuilder;

    public AiTradeReviewResponse generateFreshReview(AiTradeReviewRequest request, Long userId) throws Exception {
        if (!openAiResponsesClient.isConfigured()) {
            return fallbackReview("OpenAI API key is missing. Using fallback review.");
        }
        String content = openAiResponsesClient.generateJson(userId, "AI_REVIEW", promptBuilder.systemPrompt(request.getStrategyContext()), promptBuilder.buildPrompt(request)).content();
        return objectMapper.readValue(cleanJson(content), AiTradeReviewResponse.class);
    }

    private AiTradeReviewResponse fallbackReview(String reason) {
        return AiTradeReviewResponse.builder().grade("N/A").summary(reason).mistakes(List.of()).strengths(List.of()).improvementPlan(List.of("Review the documented entry, stop, target, and journal facts before drawing a conclusion.")).tags(List.of()).build();
    }

    private String cleanJson(String content) { return content == null ? "{}" : content.replace("```json", "").replace("```", "").trim(); }

    @java.lang.SuppressWarnings("all")
    public AiTradeReviewGeneratorService(final ObjectMapper objectMapper, final OpenAiResponsesClient openAiResponsesClient, final AiTradeReviewPromptBuilder promptBuilder) {
        this.objectMapper = objectMapper;
        this.openAiResponsesClient = openAiResponsesClient;
        this.promptBuilder = promptBuilder;
    }
}
