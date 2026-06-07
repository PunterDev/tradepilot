package com.tradepilot.discipline;

import com.tradepilot.dto.discipline.RuleViolationDto;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DisciplineScoreCalculator {
    public int calculate(List<RuleViolationDto> violations) {
        int score = 100;
        for (RuleViolationDto v : violations) {
            score -= penalty(v.getSeverity());
        }
        return Math.max(0, Math.min(100, score));
    }

    public int penalty(String severity) {
        if (severity == null) return 5;
        return switch (severity.toUpperCase()) {
            case "MINOR" -> 5;
            case "MODERATE" -> 10;
            case "MAJOR" -> 20;
            case "CRITICAL" -> 35;
            default -> 5;
        };
    }

    public boolean isClean(List<RuleViolationDto> violations) {
        return violations.stream().noneMatch(v ->
                "MAJOR".equalsIgnoreCase(v.getSeverity()) ||
                        "CRITICAL".equalsIgnoreCase(v.getSeverity()));
    }
}
