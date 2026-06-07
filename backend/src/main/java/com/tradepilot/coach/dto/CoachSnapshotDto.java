package com.tradepilot.coach.dto;

import com.tradepilot.coach.entity.CoachSnapshotStatus;
import com.tradepilot.coach.entity.CoachSnapshotType;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class CoachSnapshotDto {

    private Long id;

    private Long accountId;

    private CoachSnapshotType snapshotType;

    private CoachSnapshotStatus status;

    private LocalDate periodStart;

    private LocalDate periodEnd;

    private Integer totalTrades;

    private BigDecimal netPnl;

    private BigDecimal winRate;

    private BigDecimal profitFactor;

    private String bestSetup;

    private String worstSetup;

    private String bestSession;

    private String worstSession;

    private String biggestMistake;

    private String deterministicSummaryJson;

    private String aiResponseJson;

    private String errorMessage;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
