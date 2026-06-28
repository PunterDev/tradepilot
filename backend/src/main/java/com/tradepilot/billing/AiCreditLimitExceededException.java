package com.tradepilot.billing;

public class AiCreditLimitExceededException extends RuntimeException {
    private final int creditsUsed;
    private final int creditsLimit;
    private final int creditsRemaining;

    public AiCreditLimitExceededException(int creditsUsed, int creditsLimit, int creditsRemaining) {
        super("You have used all AI credits for this month.");
        this.creditsUsed = creditsUsed;
        this.creditsLimit = creditsLimit;
        this.creditsRemaining = creditsRemaining;
    }

    @java.lang.SuppressWarnings("all")
    public int getCreditsUsed() {
        return this.creditsUsed;
    }

    @java.lang.SuppressWarnings("all")
    public int getCreditsLimit() {
        return this.creditsLimit;
    }

    @java.lang.SuppressWarnings("all")
    public int getCreditsRemaining() {
        return this.creditsRemaining;
    }
}
