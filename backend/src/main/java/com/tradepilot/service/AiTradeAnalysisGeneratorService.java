package com.tradepilot.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradepilot.ai.openai.OpenAiResponsesClient;
import com.tradepilot.dto.ai.AiTradeAnalysisRequest;
import com.tradepilot.dto.ai.AiTradeAnalysisResponse;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class AiTradeAnalysisGeneratorService {
    @java.lang.SuppressWarnings("all")
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AiTradeAnalysisGeneratorService.class);
    private final ObjectMapper objectMapper;
    private final AiTradeAnalysisPromptBuilder promptBuilder;
    private final OpenAiResponsesClient openAiResponsesClient;

    public AiTradeAnalysisResponse generate(Long userId, AiTradeAnalysisRequest request, byte[] imageBytes, String mimeType) {
        if (!openAiResponsesClient.isConfigured()) {
            throw new IllegalStateException("OpenAI is not configured");
        }
        try {
            boolean hasImage = imageBytes != null && imageBytes.length > 0;
            String content = openAiResponsesClient.generateJson(userId, "AI_TRADE_ANALYSIS", analysisSystemPrompt(), promptBuilder.build(request, hasImage), imageBytes, mimeType).content();
            return objectMapper.readValue(cleanJson(content), AiTradeAnalysisResponse.class);
        } catch (Exception e) {
            throw new RuntimeException("AI trade analysis failed", e);
        }
    }

    private String analysisSystemPrompt() {
        return "You are TradePilot's trade-analysis assistant. Return only valid JSON. Use only visible chart evidence and the explicitly supplied trade and journal facts. Do not infer emotions, fills, rule violations, or hidden market data.";
    }

    private String cleanJson(String content) { return content == null ? "{}" : content.replace("```json", "").replace("```", "").trim(); }

    @java.lang.SuppressWarnings("all")
    public AiTradeAnalysisGeneratorService(final ObjectMapper objectMapper, final AiTradeAnalysisPromptBuilder promptBuilder, final OpenAiResponsesClient openAiResponsesClient) {
        this.objectMapper = objectMapper;
        this.promptBuilder = promptBuilder;
        this.openAiResponsesClient = openAiResponsesClient;
    }
}
