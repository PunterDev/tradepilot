package com.tradepilot.dto.admin;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
public class AdminTradeCleanupResponse {
    private Long userId;
    private Long accountId;
    private LocalDate startDate;
    private LocalDate endDate;
    private boolean dryRun;
    private long tradesMatched;
    private long tradesDeleted;
    private long tradeReviewsDeleted;
    private long tradeAnalysesDeleted;
    private long tradeRuleViolationsDeleted;
    private long dailyDisciplineScoresDeleted;
    private long sessionReviewsDeleted;
    private long sessionCachesInvalidated;
    private long leakSnapshotsDeleted;
    private long tradingDnaSnapshotsDeleted;
    private long dailyCoachSnapshotsDeleted;
    private long weeklyCoachSnapshotsDeleted;
}
