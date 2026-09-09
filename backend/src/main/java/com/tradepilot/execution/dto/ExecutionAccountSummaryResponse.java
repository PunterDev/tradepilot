package com.tradepilot.execution.dto;

import java.math.BigDecimal;
import java.util.List;

public record ExecutionAccountSummaryResponse(
        Long accountId,
        Integer executionScore,
        Integer evidenceCoveragePercent,
        EvidenceConfidence evidenceConfidence,
        BigDecimal ruleAdherencePercent,
        BigDecimal avoidableImpactR,
        long cleanWins,
        long cleanLosses,
        long undisciplinedWins,
        long undisciplinedLosses,
        WeeklyExecutionFocusResponse weeklyFocus,
        List<BehaviorPatternResponse> patterns
) {
}
