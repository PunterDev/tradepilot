package com.tradepilot.featurefunnel.dto;

import com.tradepilot.billing.PlanType;

import java.time.LocalDateTime;

public record FeatureFunnelUserDrilldownResponse(
        Long userId,
        String maskedEmail,
        PlanType plan,
        String stageReached,
        LocalDateTime entryDate,
        LocalDateTime lastStageDate,
        Double completionTimeHours,
        LocalDateTime lastActiveAt,
        String tradeBand,
        String engagementStatus
) {}
