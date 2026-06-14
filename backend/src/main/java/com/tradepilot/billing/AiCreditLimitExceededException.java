package com.tradepilot.billing;

import lombok.Getter;

@Getter
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
}
