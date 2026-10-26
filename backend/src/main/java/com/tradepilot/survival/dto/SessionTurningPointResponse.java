package com.tradepilot.survival.dto;

import com.tradepilot.survival.TurningPointType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record SessionTurningPointResponse(boolean detected, TurningPointType type, LocalDateTime timestamp, Long tradeId,
        String accountName, List<String> observableFacts, BigDecimal previousNormalizedExposure,
        BigDecimal newNormalizedExposure, Long minutesSincePriorLoss, BigDecimal pnlBefore, BigDecimal finalSessionPnl,
        BigDecimal stopBeforeActionPnl, BigDecimal lossesAvoided, BigDecimal winnersForgone, BigDecimal netImpact,
        String evidenceStrength, List<String> limitations, String engineVersion,
        CrossAccountContinuationEvidence crossAccountEvidence) {}
