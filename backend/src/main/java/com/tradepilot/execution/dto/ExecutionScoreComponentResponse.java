package com.tradepilot.execution.dto;

import java.util.List;

public record ExecutionScoreComponentResponse(
        String key,
        String label,
        int score,
        int maxScore,
        boolean evaluated,
        int availablePoints,
        List<String> reasons
) {
}
