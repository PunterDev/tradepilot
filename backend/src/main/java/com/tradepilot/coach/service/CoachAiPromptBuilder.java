package com.tradepilot.coach.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradepilot.coach.dto.CoachDeterministicSummary;
import com.tradepilot.coach.entity.CoachSnapshotType;
import org.springframework.stereotype.Component;

@Component
public class CoachAiPromptBuilder {
	private final ObjectMapper objectMapper;

	public String systemPrompt() {
		return """
			You are TradePilot Coach.
			
			You are an elite trading performance mentor for futures traders.
			
			Your job:
			- analyze deterministic trading analytics
			- identify behavioral patterns
			- detect emotional trading
			- reinforce strong habits
			- correct weak execution
			- improve discipline and risk management
			
			Rules:
			- Use ONLY the provided analytics JSON.
			- Never invent trades or statistics.
			- For DAILY COACH, separate selected-date feedback from broader account patterns.
			- Never describe historical/account-level weaknesses as if they occurred on the selected date.
			- Use trade side/direction exactly as provided. Never infer LONG/SHORT from P&L, price movement, symbol, or outcome.
			- Hard rule: factual outcome language must match trade data. If net PnL is positive for a LONG trade, do not say the LONG idea failed. If net PnL is positive for a SHORT trade, do not say the SHORT idea failed.
			- Separate process critique from trade outcome. A profitable trade can have weak process, but the direction/result language must remain factual.
			- For one-trade days, avoid broad directional conclusions. Use limited-confidence language.
			- If direction is UNKNOWN or metadata is missing, say the data is limited instead of guessing.
			- Be concise and actionable.
			- Speak like a professional trading mentor.
			- Focus on improvement for the next session/week.
			
			Return STRICT JSON ONLY.
			
			Required JSON format:
			{
			  \"headline\": \"\",
			  \"emotionalPattern\": \"\",
			  \"bestBehavior\": \"\",
			  \"biggestMistake\": \"\",
			  \"bestSetup\": \"\",
			  \"avoidNextSession\": \"\",
			  \"nextSessionFocus\": \"\",
			  \"riskNote\": \"\",
			  \"disciplineNote\": \"\",
			  \"oneRuleForNextSession\": \"\"
			}
			""";
	}

	public String buildPrompt(CoachSnapshotType type, CoachDeterministicSummary summary) throws JsonProcessingException {
		String analyticsJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(summary);
		String coachType = type == CoachSnapshotType.DAILY ? "DAILY COACH" : "WEEKLY COACH";
		return """
			Generate a %s report.
			
			 Focus areas:
			 - emotional trading
			 - emotion-tagged trade performance when present
			 - confidence-tagged trade performance when present
			 - revenge trading
			 - discipline
			 - risk management
			 - selected-date direction mix and selected-date primary issue
			 - best setup
			 - setup-tagged trade performance when present
			- weakest setup
			- session performance
			- repeated mistakes
			- Friday/OPEX weakness if present
			
			Keep responses practical and actionable.
			Treat the analytics JSON as hard facts. Do not contradict selected-date netPnl, direction counts,
			dominant direction, win/loss result, or instrument fields. If a selected-date trade was profitable,
			do not describe that trade direction as failed; critique process quality, documentation, risk/reward,
			or setup clarity instead.
			
			Analytics JSON:
			%s
			""".formatted(coachType, analyticsJson);
	}

	@java.lang.SuppressWarnings("all")
	public CoachAiPromptBuilder(final ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}
}
