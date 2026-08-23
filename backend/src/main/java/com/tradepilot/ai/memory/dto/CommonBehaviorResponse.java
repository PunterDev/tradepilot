package com.tradepilot.ai.memory.dto;

public record CommonBehaviorResponse(
        String mistake,
        String mistakeLabel,
        Integer occurrences,
        Integer sampleSize,
        String emotion
) {
}
