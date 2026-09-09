package com.tradepilot.execution.dto;

public record ProcessBreakpointResponse(
        ProcessBreakpointStatus status,
        Long previousTradeId,
        Long currentTradeId,
        Long timeToNextTradeMinutes,
        Long baselineTimeBetweenTradesMinutes,
        Integer previousExecutionScore,
        Integer currentExecutionScore,
        String message
) {
}
