package com.tradepilot.playbook.exception;

public class PlaybookAiReviewIneligibleException extends RuntimeException {
    private final int requiredTradeCount;
    private final long currentEligibleTradeCount;

    public PlaybookAiReviewIneligibleException(String message, int requiredTradeCount, long currentEligibleTradeCount) {
        super(message);
        this.requiredTradeCount = requiredTradeCount;
        this.currentEligibleTradeCount = currentEligibleTradeCount;
    }

    public int getRequiredTradeCount() {
        return requiredTradeCount;
    }

    public long getCurrentEligibleTradeCount() {
        return currentEligibleTradeCount;
    }
}
