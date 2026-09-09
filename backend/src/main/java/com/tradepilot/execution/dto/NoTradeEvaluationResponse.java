package com.tradepilot.execution.dto;

public record NoTradeEvaluationResponse(
        NoTradeEvaluationStatus status,
        Long preAnalysisId,
        String message
) {
}
