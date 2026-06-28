package com.tradepilot.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradepilot.dto.ai.AiChartReviewResponse;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.client.RestTemplate;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Service
public class AiChartReviewService {
    @java.lang.SuppressWarnings("all")
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AiChartReviewService.class);
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate = new RestTemplate();

    public AiChartReviewResponse reviewChart(MultipartFile file, Long tradeId, String journalText) {
        try {
            String base64Image = Base64.getEncoder().encodeToString(file.getBytes());
            String mimeType = file.getContentType();
            String prompt = buildPrompt(tradeId, journalText);
            Map<String, Object> requestBody = Map.of("model", "gpt-4.1-mini", "messages", List.of(Map.of("role", "user", "content", List.of(Map.of("type", "text", "text", prompt), Map.of("type", "image_url", "image_url", Map.of("url", "data:" + mimeType + ";base64," + base64Image))))), "temperature", 0.2, "response_format", Map.of("type", "json_object"));
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(System.getenv("OPENAI_API_KEY"));
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<Map> response = restTemplate.exchange("https://api.openai.com/v1/chat/completions", HttpMethod.POST, entity, Map.class);
            Map choice = (Map) ((List<?>) response.getBody().get("choices")).get(0);
            Map message = (Map) choice.get("message");
            String content = (String) message.get("content");
            return objectMapper.readValue(content, AiChartReviewResponse.class);
        } catch (Exception e) {
            log.error("AI chart review failed", e);
            throw new RuntimeException("AI chart review failed: " + e.getMessage());
        }
    }

    private String buildPrompt(Long tradeId, String journalText) {
        return """
            You are an expert futures trading coach reviewing an NQ/ES chart screenshot.
            
            Analyze the chart visually and return ONLY valid JSON.
            
            Focus on:
            - market structure
            - liquidity sweep
            - displacement
            - FVG / IFVG quality
            - entry timing
            - stop loss placement
            - take profit logic
            - risk/reward
            - emotional or discipline mistake
            
            Trade ID: %s
            
            Trader journal note:
            %s
            
            Return this JSON shape:
            {
              \"summary\": \"...\",
              \"bias\": \"...\",
              \"entryQuality\": \"...\",
              \"stopLossQuality\": \"...\",
              \"takeProfitQuality\": \"...\",
              \"riskManagement\": \"...\",
              \"grade\": \"A/B/C/D/F\",
              \"mistakes\": [\"...\"],
              \"strengths\": [\"...\"],
              \"improvementPlan\": [\"...\"],
              \"tags\": [\"FVG\", \"IFVG\", \"Liquidity Sweep\"]
            }
            """.formatted(tradeId == null ? "N/A" : tradeId, journalText == null ? "" : journalText);
    }

    @java.lang.SuppressWarnings("all")
    public AiChartReviewService(final ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }
}
