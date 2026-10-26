package com.tradepilot.survival.dto;

import com.tradepilot.survival.AccountSurvivalState;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record AccountSurvivalAssessmentResponse(Long accountId, LocalDate sessionDate, AccountSurvivalState state,
        BigDecimal currentBalance, BigDecimal failureThreshold, BigDecimal remainingBuffer, BigDecimal plannedRiskPerTrade,
        BigDecimal typicalLoss, Integer typicalLossSampleSize, BigDecimal plannedRiskBufferPercent,
        BigDecimal typicalLossBufferPercent, BigDecimal normalLossCapacity, Integer firmMaxContracts,
        Integer personalMaxContracts, Integer sustainableMaxContracts, String primaryThreat, String evidenceStrength,
        List<String> missingDataReasons, SessionTurningPointResponse turningPoint, LocalDateTime calculatedAt,
        String calculationVersion) {}
