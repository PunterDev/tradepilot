package com.tradepilot.execution.dto;

public record PlanComparisonItemResponse(
        String type,
        PlanComparisonStatus status,
        String message
) {
}
