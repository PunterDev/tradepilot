package com.tradepilot.survival.dto;

import com.tradepilot.survival.ProtectionCommitmentStatus;
import java.time.LocalDate;
import java.util.List;

public record CommitmentEvaluationResponse(boolean available, LocalDate evaluatedSessionDate,
        ProtectionCommitmentStatus overallStatus, String evaluationVersion, List<RuleResult> rules, String limitation) {
    public record RuleResult(String rule, String status, String configuredValue, String observedValue, String message) {}
}
