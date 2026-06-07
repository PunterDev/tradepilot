package com.tradepilot.dto.session;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AiSessionReviewResponse {
    private Long id;
    private String summary;
    private String whatWentWell;
    private String whatHurtPerformance;
    private String biggestMistake;
    private String bestSetup;
    private String worstDecision;
    private String emotionalPattern;
    private String executionPattern;
    private String riskManagementPattern;
    private String oneRuleForTomorrow;
    private LocalDateTime generatedAt;
}
