package com.tradepilot.retention.dto;

import com.tradepilot.billing.PlanType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class RetentionUserResponse {
    private Long userId;
    private String email;
    private String name;
    private PlanType plan;
    private LocalDateTime lastActive;
    private long activityCount30d;
    private long daysSinceActivity;
    private RetentionStatus status;
}
