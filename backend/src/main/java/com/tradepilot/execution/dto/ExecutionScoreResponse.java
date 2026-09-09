package com.tradepilot.execution.dto;

import java.util.List;
import java.util.Map;

public record ExecutionScoreResponse(
        Long tradeId,
        Integer executionScore,
        ExecutionGrade grade,
        ExecutionClassification classification,
        int availablePoints,
        int maxPoints,
        int evidenceCoveragePercent,
        EvidenceConfidence evidenceConfidence,
        Map<String, ExecutionScoreComponentResponse> components,
        List<String> reasons,
        List<String> detectedViolations,
        String processOutcomeMessage,
        PreAnalysisComparisonResponse preAnalysisComparison
) {
}
