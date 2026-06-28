package com.tradepilot.coach.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradepilot.coach.dto.CoachSnapshotDto;
import com.tradepilot.coach.entity.AiCoachSnapshot;
import com.tradepilot.coach.entity.CoachSnapshotStatus;
import com.tradepilot.coach.entity.CoachSnapshotType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
public class CoachSnapshotMapper {

    private final ObjectMapper objectMapper;

    public CoachSnapshotDto toDto(AiCoachSnapshot snapshot) {
        CoachSnapshotDto response = new CoachSnapshotDto();
        response.setId(snapshot.getId());
        response.setAccountId(snapshot.getAccountId());
        response.setSnapshotType(snapshot.getSnapshotType());
        response.setStatus(snapshot.getStatus());
        response.setPeriodStart(snapshot.getPeriodStart());
        response.setPeriodEnd(snapshot.getPeriodEnd());
        response.setTotalTrades(snapshot.getTotalTrades());
        response.setNetPnl(snapshot.getNetPnl());
        response.setWinRate(snapshot.getWinRate());
        response.setProfitFactor(snapshot.getProfitFactor());
        response.setBestSetup(snapshot.getBestSetup());
        response.setWorstSetup(snapshot.getWorstSetup());
        response.setBestSession(snapshot.getBestSession());
        response.setWorstSession(snapshot.getWorstSession());
        response.setBiggestMistake(snapshot.getBiggestMistake());
        response.setDeterministicSummaryJson(snapshot.getDeterministicSummaryJson());
        response.setAiResponseJson(snapshot.getAiResponseJson());
        response.setErrorMessage(snapshot.getErrorMessage());
        applyDeterministicSummary(response, snapshot.getDeterministicSummaryJson());
        response.setAiGenerated(snapshot.getAiResponseJson() != null && !snapshot.getAiResponseJson().isBlank());
        boolean completedNoTrades = snapshot.getStatus() == CoachSnapshotStatus.COMPLETED
                && snapshot.getTotalTrades() != null
                && snapshot.getTotalTrades() == 0;
        response.setNoTradeDay(completedNoTrades && snapshot.getSnapshotType() == CoachSnapshotType.DAILY);
        response.setNoTradeWeek(completedNoTrades && snapshot.getSnapshotType() == CoachSnapshotType.WEEKLY);
        response.setCreditsConsumed(response.getAiGenerated() ? creditsFor(snapshot) : 0);
        response.setCreatedAt(snapshot.getCreatedAt());
        response.setUpdatedAt(snapshot.getUpdatedAt());
        return response;
    }

    private int creditsFor(AiCoachSnapshot snapshot) {
        return switch (snapshot.getSnapshotType()) {
            case DAILY -> 2;
            case WEEKLY -> 4;
        };
    }

    private void applyDeterministicSummary(CoachSnapshotDto response, String json) {
        if (json == null || json.isBlank()) {
            return;
        }
        try {
            var summary = objectMapper.readTree(json);
            response.setSelectedDaySummary(text(summary, "selectedDaySummary"));
            response.setPrimaryIssueToday(text(summary, "primaryIssueToday"));
            response.setSetupDirectionReview(text(summary, "setupDirectionReview"));
            response.setLongTradeCount(integer(summary, "longTradeCount"));
            response.setShortTradeCount(integer(summary, "shortTradeCount"));
            response.setUnknownTradeCount(integer(summary, "unknownTradeCount"));
            response.setLongPnl(decimal(summary, "longPnl"));
            response.setShortPnl(decimal(summary, "shortPnl"));
            response.setWorstTradeSide(text(summary, "worstTradeSide"));
            response.setDataQuality(text(summary, "dataQuality"));
            response.setDataQualityReason(text(summary, "dataQualityReason"));
            response.setNextSessionRule(text(summary, "nextSessionRule"));
        } catch (Exception ignored) {
            // Legacy snapshots may contain older summary JSON. Keep the snapshot viewable.
        }
    }

    private String text(com.fasterxml.jackson.databind.JsonNode node, String field) {
        return node.hasNonNull(field) ? node.get(field).asText() : null;
    }

    private Integer integer(com.fasterxml.jackson.databind.JsonNode node, String field) {
        return node.hasNonNull(field) ? node.get(field).asInt() : null;
    }

    private BigDecimal decimal(com.fasterxml.jackson.databind.JsonNode node, String field) {
        return node.hasNonNull(field) ? node.get(field).decimalValue() : null;
    }
}
