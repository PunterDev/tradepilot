package com.tradepilot.billing;

import lombok.Getter;

@Getter
public class PlanLimitExceededException extends RuntimeException {
    private final String code = "PLAN_LIMIT_EXCEEDED";
    private final boolean upgradeRequired = true;

    public PlanLimitExceededException(String message) {
        super(message);
    }
}
