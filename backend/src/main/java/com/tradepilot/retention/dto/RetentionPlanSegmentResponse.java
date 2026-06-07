package com.tradepilot.retention.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RetentionPlanSegmentResponse {
    private long dau;
    private long wau;
    private long mau;
}
