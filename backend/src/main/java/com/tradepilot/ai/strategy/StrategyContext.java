package com.tradepilot.ai.strategy;

import java.util.List;

public record StrategyContext(
        StrategyType strategyType,
        String strategyName,
        String setupType,
        String setupName,
        String playbookCategory,
        String playbookName,
        List<String> userRules,
        List<String> entryCriteria,
        List<String> invalidationCriteria,
        List<String> managementRules,
        List<String> noTradeConditions,
        List<String> allowedStrategyConcepts,
        String strategySource,
        double confidence
) {
    public static StrategyContext general() {
        return new StrategyContext(
                StrategyType.GENERAL,
                "General execution review",
                null,
                null,
                null,
                null,
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of("plan adherence", "risk management", "entry timing", "exit quality", "trade management", "discipline"),
                "GENERAL_FALLBACK",
                0.0
        );
    }

    public boolean isIct() {
        return strategyType == StrategyType.ICT_SMC;
    }

    public String promptBlock() {
        return """
                StrategyContext:
                - strategyType: %s
                - strategyName: %s
                - setupType: %s
                - setupName: %s
                - playbookCategory: %s
                - playbookName: %s
                - strategySource: %s
                - confidence: %.2f
                - allowedStrategyConcepts: %s
                - userRules: %s
                - entryCriteria: %s
                - invalidationCriteria: %s
                - managementRules: %s
                - noTradeConditions: %s
                """.formatted(
                value(strategyType),
                value(strategyName),
                value(setupType),
                value(setupName),
                value(playbookCategory),
                value(playbookName),
                value(strategySource),
                confidence,
                values(allowedStrategyConcepts),
                values(userRules),
                values(entryCriteria),
                values(invalidationCriteria),
                values(managementRules),
                values(noTradeConditions)
        );
    }

    private static String value(Object value) {
        return value == null ? "unspecified" : String.valueOf(value);
    }

    private static String values(List<String> values) {
        return values == null || values.isEmpty() ? "[]" : values.toString();
    }
}
