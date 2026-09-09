package com.tradepilot.execution.dto;

import java.math.BigDecimal;

public record WeeklyExecutionFocusResponse(
        String title,
        String reason,
        BigDecimal impactR,
        String goal,
        Long previousFrequency,
        Long currentFrequency,
        Integer previousExecutionScore,
        Integer currentExecutionScore,
        WeeklyExecutionFocusStatus status,
        String confidence
) {
}
