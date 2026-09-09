package com.tradepilot.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradepilot.ai.strategy.StrategyContext;
import com.tradepilot.ai.strategy.StrategyContextService;
import com.tradepilot.dto.ai.AiChartReviewResponse;
import com.tradepilot.entity.Trade;
import com.tradepilot.entity.User;
import com.tradepilot.security.OwnershipGuard;
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
    private final OwnershipGuard ownershipGuard;
    private final TradeService tradeService;
    private final StrategyContextService strategyContextService;
    private final RestTemplate restTemplate = new RestTemplate();

    public AiChartReviewResponse reviewChart(User user, MultipartFile file, Long tradeId, String journalText) {
        try {
            ownershipGuard.requireCurrentUserId(user);
            Trade trade = null;
            if (tradeId != null) {
                ownershipGuard.requireTradeOwner(user, tradeId);
                trade = tradeService.getOwnedTrade(user, tradeId);
            }
            StrategyContext strategyContext = strategyContextService.resolve(user == null ? null : user.getId(), trade, journalText);
            String base64Image = Base64.getEncoder().encodeToString(file.getBytes());
            String mimeType = file.getContentType();
            String prompt = buildPrompt(tradeId, journalText, strategyContext);
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
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("AI chart review failed", e);
            throw new RuntimeException("AI chart review failed: " + e.getMessage());
        }
    }

    public String buildPrompt(Long tradeId, String journalText, StrategyContext strategyContext) {
        StrategyContext context = strategyContext == null ? StrategyContext.general() : strategyContext;
        return """
            You are an expert futures trading coach reviewing an NQ/ES chart screenshot.
            
            Analyze the chart visually and return ONLY valid JSON.

            %s
            
            Focus on:
            - observable chart evidence relevant to StrategyContext
            - general execution context
            - entry timing relative to the user's own rules
            - entry timing
            - stop loss placement
            - take profit logic
            - risk/reward
            - emotional or discipline mistake

            Strategy rules:
            - User playbook and user rules override generic strategy-profile assumptions.
            - Apply strategy-specific concepts only when StrategyContext supports them.
            - Do not introduce requirements or terminology from trading methodologies that are not part of the user's strategy, setup, playbook, or explicitly documented trade rationale.
            - General market-structure, retracement, or liquidity language does not by itself imply any specific methodology.
            - If StrategyContext is GENERAL or CUSTOM, stay methodology-neutral and evaluate only visible price action, documented setup, risk, timing, management, discipline, and plan adherence.
            
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
              \"tags\": %s
            }
            """.formatted(context.promptBlock(), tradeId == null ? "N/A" : tradeId, journalText == null ? "" : journalText, suggestedTags(context));
    }

    private String suggestedTags(StrategyContext context) {
        if (context != null && context.isIct()) {
            return "[\"FVG\", \"IFVG\", \"Liquidity Sweep\", \"Displacement\", \"SMT\"]";
        }
        return "[\"Good Patience\", \"Late Entry\", \"Early Entry\", \"Poor Risk Management\", \"Good Risk Management\", \"Rule Violation\", \"Choppy Market\"]";
    }

    @java.lang.SuppressWarnings("all")
    public AiChartReviewService(final ObjectMapper objectMapper, final OwnershipGuard ownershipGuard, final TradeService tradeService, final StrategyContextService strategyContextService) {
        this.objectMapper = objectMapper;
        this.ownershipGuard = ownershipGuard;
        this.tradeService = tradeService;
        this.strategyContextService = strategyContextService;
    }
}
