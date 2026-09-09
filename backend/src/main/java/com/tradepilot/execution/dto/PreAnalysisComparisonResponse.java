package com.tradepilot.execution.dto;

import java.time.LocalDateTime;
import java.util.List;

public record PreAnalysisComparisonResponse(
        PlanAlignmentStatus status,
        boolean planAvailable,
        Long preAnalysisId,
        LocalDateTime preAnalysisCreatedAt,
        String market,
        List<PlanComparisonItemResponse> comparisons
) {
    public static PreAnalysisComparisonResponse notAvailable(String message) {
        return new PreAnalysisComparisonResponse(
                PlanAlignmentStatus.NOT_EVALUABLE,
                false,
                null,
                null,
                null,
                List.of(new PlanComparisonItemResponse("PLAN_LINK", PlanComparisonStatus.NOT_EVALUABLE, message))
        );
    }
}
