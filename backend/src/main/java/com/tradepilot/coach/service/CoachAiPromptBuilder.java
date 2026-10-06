package com.tradepilot.coach.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradepilot.coach.dto.CoachDeterministicSummary;
import com.tradepilot.coach.entity.CoachSnapshotType;
import com.tradepilot.leakdetector.dto.LeakResultDto;
import org.springframework.stereotype.Component;

import java.util.Map;

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
			- A supplied behavioral finding is deterministic evidence, not a request to discover another leak.
			- Never change its finding type, evidence strength, occurrence count, sample size, threshold, metrics, or counterfactual values.
			- Do not claim statistical significance, causality, future profitability, or that an unsupported counterfactual would have improved results.
			- Do not recommend increasing size or leverage, trading more to recover, revenge trading, widening stops, or violating account limits.
			- When a behavioral finding is supplied, give at most one next-session rule and keep the tone supportive, neutral, and non-shaming.
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
		return buildPrompt(type, summary, null);
	}

	public String buildPrompt(CoachSnapshotType type, CoachDeterministicSummary summary, LeakResultDto behavioralLeak) throws JsonProcessingException {
		String analyticsJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(summary);
		String coachType = type == CoachSnapshotType.DAILY ? "DAILY COACH" : "WEEKLY COACH";
		String behavioralEvidence = behavioralLeak == null || behavioralLeak.getBehavioralFinding() == null
				? "No deterministic behavioral finding was supplied. Do not infer one."
				: objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(Map.of(
						"findingType", behavioralLeak.getLeakType(),
						"finding", behavioralLeak.getBehavioralFinding()
				));
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

			Deterministic behavioral evidence (if present):
			%s

			Do not ask to discover behavioral mistakes. Explain only the supplied deterministic evidence.
			If counterfactual.supported is false or counterfactual.netHistoricalImpact is negative,
			do not describe the counterfactual as savings or improvement.
			""".formatted(coachType, analyticsJson, behavioralEvidence);
	}

	@java.lang.SuppressWarnings("all")
	public CoachAiPromptBuilder(final ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}
}
