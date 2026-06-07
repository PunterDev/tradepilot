package com.tradepilot.service;

import com.tradepilot.dto.ai.AiTradeAnalysisRequest;
import com.tradepilot.dto.ai.TradeSnapshot;
import org.springframework.stereotype.Component;

@Component
public class AiTradeAnalysisPromptBuilder {

    public String build(AiTradeAnalysisRequest request, boolean hasChartImage) {
        TradeSnapshot trade = request == null ? null : request.getTrade();

        return """
                You are TradePilot AI, a senior futures trading coach for NQ/ES/MES/MNQ traders.

                Analyze this trade as ONE unified trade analysis.

                If a chart screenshot is provided, use it as supporting visual context.
                If no chart screenshot is provided, do not invent visual chart details; analyze only from trade data and journal notes.

                Return ONLY valid JSON. No markdown. No commentary outside JSON.

                Critical rules:
                - The structured trade.side field is the source of truth.
                - Never infer LONG/SHORT from PnL.
                - A losing long trade is still LONG.
                - A winning short trade is still SHORT.
                - Do not contradict structured trade data.
                - If the chart appears ambiguous, prioritize the structured trade fields.
                - Use the chart image only as supporting visual context.
                - Scores must be integers from 0 to 100.

                Trade context:
                - Trade ID: %s
                - Symbol: %s
                - Side: %s
                - PnL: %s
                - Quantity: %s
                - Entry price: %s
                - Exit price: %s
                - Entry time: %s
                - Exit time: %s
                - Notes: %s

                Journal text:
                %s

                Chart screenshot provided: %s

                Focus on:
                - execution quality
                - risk management
                - discipline
                - entry and exit quality
                - ICT concepts only when visible in chart or supported by notes: liquidity sweep, displacement, FVG, IFVG, HTF bias
                - emotional mistakes such as revenge trading, forcing, early entry, moving to breakeven too soon

                Rule violations:
                - Detect only behavioral/contextual rule violations that require judgment.
                - Do not duplicate deterministic SYSTEM rules such as trade before 9:45, max daily loss, or overtrading unless explicitly supported by journal or chart context.
                - Only include violations you can justify with trade data, journal notes, or visible chart context.
                - If unsure, return an empty violations array.
                - Use source "AI" for AI-detected violations.
                - Severity must be one of: MINOR, MODERATE, MAJOR, CRITICAL.
                - Category must be one of: RISK, ENTRY, EXIT, PSYCHOLOGY, SESSION, DISCIPLINE.

                Required JSON shape:
                {
                  "summary": "string",
                  "grade": "A/B/C/D/F",
                  "executionScore": 0,
                  "disciplineScore": 0,
                  "riskScore": 0,
                  "marketContext": "string",
                  "entryAnalysis": "string",
                  "exitAnalysis": "string",
                  "riskAnalysis": "string",
                  "psychologyAnalysis": "string",
                  "chartAnalysis": "string or empty when no screenshot is provided",
                  "mistakes": ["string"],
                  "strengths": ["string"],
                  "improvementPlan": ["string"],
                  "tags": ["string"],
                  "violations": []
                }
                """.formatted(
                request == null ? null : request.getTradeId(),
                trade == null ? null : trade.getSymbol(),
                trade == null ? null : trade.getSide(),
                trade == null ? null : trade.getPnl(),
                trade == null ? null : trade.getQuantity(),
                trade == null ? null : trade.getEntryPrice(),
                trade == null ? null : trade.getExitPrice(),
                trade == null ? null : trade.getEntryTime(),
                trade == null ? null : trade.getExitTime(),
                trade == null ? null : trade.getNotes(),
                request == null || request.getJournalText() == null ? "" : request.getJournalText(),
                hasChartImage ? "YES" : "NO"
        );
    }
}