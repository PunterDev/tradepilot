package com.tradepilot.survival.dto;

import java.util.List;

public record CommitmentEvaluationHistoryResponse(
        List<CommitmentEvaluationResponse> evaluations,
        int reportingWindowSessions,
        int evaluatedSessions,
        int followedSessions,
        int brokenSessions,
        int partiallyObservedSessions,
        int notEvaluableSessions,
        int fullyEvaluableSessions,
        Integer adherencePercent) {
}
