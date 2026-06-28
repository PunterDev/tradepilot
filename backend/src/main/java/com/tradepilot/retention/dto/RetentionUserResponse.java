package com.tradepilot.retention.dto;

import com.tradepilot.billing.PlanType;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RetentionUserResponse {
    private Long userId;
    private String email;
    private String name;
    private PlanType plan;
    private LocalDateTime lastActive;
    private long activityCount30d;
    private long daysSinceActivity;
    private RetentionStatus status;

    public static RetentionUserResponse of(
            Long userId,
            String email,
            String name,
            PlanType plan,
            LocalDateTime lastActive,
            long activityCount30d,
            long daysSinceActivity,
            RetentionStatus status
    ) {
        RetentionUserResponse response = new RetentionUserResponse();
        response.setUserId(userId);
        response.setEmail(email);
        response.setName(name);
        response.setPlan(plan);
        response.setLastActive(lastActive);
        response.setActivityCount30d(activityCount30d);
        response.setDaysSinceActivity(daysSinceActivity);
        response.setStatus(status);
        return response;
    }
}
