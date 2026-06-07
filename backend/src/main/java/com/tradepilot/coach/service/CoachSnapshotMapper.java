package com.tradepilot.coach.service;

import com.tradepilot.coach.dto.CoachSnapshotDto;
import com.tradepilot.coach.entity.AiCoachSnapshot;
import org.springframework.stereotype.Component;

@Component
public class CoachSnapshotMapper {

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
        response.setCreatedAt(snapshot.getCreatedAt());
        response.setUpdatedAt(snapshot.getUpdatedAt());
        return response;
    }
}
