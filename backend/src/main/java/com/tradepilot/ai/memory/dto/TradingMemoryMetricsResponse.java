package com.tradepilot.ai.memory.dto;

import java.math.BigDecimal;

public record TradingMemoryMetricsResponse(
        Integer sampleSize,
        BigDecimal averageResultR,
        BigDecimal medianResultR,
        BigDecimal averageMaeR,
        BigDecimal averageMfeR,
        BigDecimal averageExitEfficiency,
        Integer winningTrades,
        Integer losingTrades,
        String mostCommonMistake,
        Integer mistakeOccurrences,
        String mostCommonEmotion,
        Integer emotionOccurrences
) {
}
