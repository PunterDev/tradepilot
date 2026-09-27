package com.tradepilot.billing;

public enum AiCreditPack {
    STARTER_25(25, 999, "usd"),
    POWER_75(75, 2499, "usd"),
    ELITE_200(200, 4999, "usd");

    private final int credits;
    private final int amountCents;
    private final String currency;

    AiCreditPack(int credits, int amountCents, String currency) {
        this.credits = credits;
        this.amountCents = amountCents;
        this.currency = currency;
    }

    public int credits() {
        return credits;
    }

    public int amountCents() {
        return amountCents;
    }

    public String currency() {
        return currency;
    }
}
