package com.tradepilot.retention.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RetentionSummaryResponse {
    private long totalUsers;
    private long dau;
    private long wau;
    private long mau;
    private double dauMauRatio;
    private long newUsers30d;
    private double retention7d;
    private double retention30d;
    private long powerUsers;
    private long atRiskUsers;
    private long inactiveUsers;
    private RetentionPlanSegmentResponse freeUsers;
    private RetentionPlanSegmentResponse proUsers;
}
