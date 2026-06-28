package com.tradepilot.billing;

public class PlanLimitExceededException extends RuntimeException {
    private final String code = "PLAN_LIMIT_EXCEEDED";
    private final boolean upgradeRequired = true;

    public PlanLimitExceededException(String message) {
        super(message);
    }

    @java.lang.SuppressWarnings("all")
    public String getCode() {
        return this.code;
    }

    @java.lang.SuppressWarnings("all")
    public boolean isUpgradeRequired() {
        return this.upgradeRequired;
    }
}
