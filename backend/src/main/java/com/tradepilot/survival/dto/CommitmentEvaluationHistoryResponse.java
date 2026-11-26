package com.tradepilot.survival.dto;

import com.tradepilot.survival.ProtectionCommitmentStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record CommitmentEvaluationHistoryResponse(
        Long accountId,
        Long commitmentId,
        LocalDate effectiveSessionDate,
        int limit,
        boolean hasMore,
        List<Entry> evaluations) {

    public record Entry(
            Long evaluationId,
            LocalDate evaluatedSessionDate,
            LocalDateTime evaluatedAt,
            ProtectionCommitmentStatus overallStatus,
            String evaluationVersion,
            boolean current,
            List<CommitmentEvaluationResponse.RuleResult> rules) {
    }
}
