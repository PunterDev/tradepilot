package com.tradepilot.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradepilot.ai.openai.OpenAiResponsesClient;
import com.tradepilot.config.OpenAiProperties;
import com.tradepilot.dto.session.AiSessionReviewResponse;
import com.tradepilot.dto.trade.TradeResponse;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
public class AiSessionReviewGeneratorService {
	@java.lang.SuppressWarnings("all")
	private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AiSessionReviewGeneratorService.class);
	private final ObjectMapper objectMapper;
	private final OpenAiResponsesClient openAiResponsesClient;

	public AiSessionReviewResponse generate(Long userId, LocalDate sessionDate, List<TradeResponse> trades, BigDecimal totalPnl, BigDecimal winRate, Map<String, Long> mistakeSummary, Integer disciplineScore) {
		if (!openAiResponsesClient.isConfigured()) {
			log.debug("Session review OpenAI call skipped; API key missing. userId={}, sessionDate={}", userId, sessionDate);
			return fallback(sessionDate, trades, totalPnl, winRate, mistakeSummary, disciplineScore);
		}
		try {
			String content = openAiResponsesClient.generateJson(userId, "SESSION_REVIEW", systemPrompt(), prompt(sessionDate, trades, totalPnl, winRate, mistakeSummary, disciplineScore)).content();
			return objectMapper.readValue(content, AiSessionReviewResponse.class);
		} catch (Exception error) {
			log.warn("AI session review failed. sessionDate={}", sessionDate, error);
			return fallback(sessionDate, trades, totalPnl, winRate, mistakeSummary, disciplineScore);
		}
	}

	private String systemPrompt() {
		return """
			You are TradePilot AI, a strict but practical futures trading session coach.
			Return ONLY valid JSON. Do not use markdown. Do not include extra fields.
			
			JSON shape:
			{
			  \"summary\": \"string\",
			  \"whatWentWell\": \"string\",
			  \"whatHurtPerformance\": \"string\",
			  \"biggestMistake\": \"string\",
			  \"bestSetup\": \"string\",
			  \"worstDecision\": \"string\",
			  \"emotionalPattern\": \"string\",
			  \"executionPattern\": \"string\",
			  \"riskManagementPattern\": \"string\",
			  \"oneRuleForTomorrow\": \"string\"
			}
			""";
	}

	private String prompt(LocalDate sessionDate, List<TradeResponse> trades, BigDecimal totalPnl, BigDecimal winRate, Map<String, Long> mistakeSummary, Integer disciplineScore) {
		return """
			Review this full trading session/day.
			
			Date: %s
			Total P&L: %s
			Win rate: %s%%
			Discipline score: %s/100
			Mistake summary: %s
			
			Trades in timeline order:
			%s
			
			 Identify:
			 - session summary
			 - what went well
			 - what hurt performance
			 - Setup Analysis if setup metadata exists
			 - Emotion Analysis if emotion metadata exists
			 - Confidence Analysis if confidence metadata exists
			 - best trade
			- worst trade
			- biggest mistake
			- emotional pattern
			- execution pattern
			- risk management pattern
			- worst decision
			- one clear rule for tomorrow
			
			Be concise, direct, and specific to the trades.
			""".formatted(sessionDate, totalPnl, winRate, disciplineScore, mistakeSummary, trades);
	}

	private AiSessionReviewResponse fallback(LocalDate sessionDate, List<TradeResponse> trades, BigDecimal totalPnl, BigDecimal winRate, Map<String, Long> mistakeSummary, Integer disciplineScore) {
		String dominantMistake = mistakeSummary.entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse("No repeated mistake tagged");
		AiSessionReviewResponse response = new AiSessionReviewResponse();
		response.setSummary("Session review for " + sessionDate + ": " + trades.size() + " trades, " + totalPnl + " total P&L, " + winRate + "% win rate.");
		response.setWhatWentWell("Deterministic session result: " + totalPnl + " total P&L across " + trades.size() + " recorded trades.");
		response.setWhatHurtPerformance(dominantMistake);
		response.setBiggestMistake(dominantMistake);
		response.setBestSetup(trades.stream().filter(t -> t.getPnlAmount() != null && t.getPnlAmount().compareTo(BigDecimal.ZERO) > 0).findFirst().map(t -> String.valueOf(t.getSetup())).orElse("No winning setup stood out."));
		response.setWorstDecision("No AI decision inference was generated. Review the supplied trades and any tagged mistakes.");
		response.setEmotionalPattern("No emotional pattern can be determined without an available AI response and recorded emotion metadata.");
		response.setExecutionPattern("No execution conclusion was generated; compare each documented entry with its stated plan.");
		response.setRiskManagementPattern("Review recorded size, stop, and realized P&L before changing risk.");
		response.setOneRuleForTomorrow("Use one rule taken from your documented plan after reviewing the session evidence.");
		return response;
	}

	@java.lang.SuppressWarnings("all")
	public AiSessionReviewGeneratorService(final ObjectMapper objectMapper, final OpenAiResponsesClient openAiResponsesClient) {
		this.objectMapper = objectMapper;
		this.openAiResponsesClient = openAiResponsesClient;
	}
}
