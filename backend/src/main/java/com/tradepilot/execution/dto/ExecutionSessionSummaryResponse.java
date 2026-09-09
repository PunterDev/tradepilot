package com.tradepilot.execution.dto;

import java.math.BigDecimal;
import java.util.List;

public record ExecutionSessionSummaryResponse(
        Long accountId,
        String date,
        Integer sessionExecutionScore,
        Integer evidenceCoveragePercent,
        EvidenceConfidence evidenceConfidence,
        long cleanWins,
        long cleanLosses,
        long undisciplinedWins,
        long undisciplinedLosses,
        BigDecimal ruleAdherencePercent,
        BigDecimal avoidableImpactR,
        LossResponseResponse lossResponse,
        String processBreakPoint,
        ProcessBreakpointResponse processBreakpoint,
        PreAnalysisComparisonResponse preAnalysisComparison,
        NoTradeEvaluationResponse noTradeEvaluation,
        List<ExecutionScoreResponse> trades
) {
}
