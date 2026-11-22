package com.tradepilot.tradingdna.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradepilot.ai.openai.OpenAiResponsesClient;
import com.tradepilot.tradingdna.dto.TradingDnaAiResult;
import com.tradepilot.tradingdna.dto.TradingDnaContext;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class TradingDnaAiGeneratorService {
    @java.lang.SuppressWarnings("all")
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(TradingDnaAiGeneratorService.class);
    private final ObjectMapper objectMapper;
    private final OpenAiResponsesClient openAiResponsesClient;
    private final TradingDnaPromptBuilder promptBuilder;

    public TradingDnaAiResult generate(TradingDnaContext context) throws Exception {
        if (!openAiResponsesClient.isConfigured()) {
            log.debug("Trading DNA OpenAI call skipped; API key missing. userId={}, accountId={}, totalTrades={}", context.getUserId(), context.getAccountId(), context.getTotalTrades());
            return fallback(context);
        }
        String content = openAiResponsesClient.generateJson(context.getUserId(), "TRADING_DNA", promptBuilder.systemPrompt(), promptBuilder.buildPrompt(context)).content();
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
                List.of(),
                List.of(),
                defaultList(context.getBestSession(), "Best session: "),
                defaultList(context.getWorstSession(), "Weakest session: "),
                context.getMistakeCounts().entrySet().stream().limit(5).map(entry -> entry.getKey() + " (" + entry.getValue() + "x)").toList(),
                buildTrend(context),
                context.getDeterministicConsistencyScore(),
                context.getDeterministicDisciplineScore(),
                context.getDeterministicExecutionScore(),
                context.getDeterministicPatienceScore(),
                "No AI confidence conclusion was generated; use only the deterministic scores and recorded trade evidence.",
                "Review the deterministic signals and choose the next action from your documented trading plan."
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
        return "Consistency trend is based only on the deterministic scores and recorded trade data in the selected timeframe.";
    }

    @java.lang.SuppressWarnings("all")
    public TradingDnaAiGeneratorService(final ObjectMapper objectMapper, final OpenAiResponsesClient openAiResponsesClient, final TradingDnaPromptBuilder promptBuilder) {
        this.objectMapper = objectMapper;
        this.openAiResponsesClient = openAiResponsesClient;
        this.promptBuilder = promptBuilder;
    }
}
