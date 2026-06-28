package com.tradepilot.billing;

public enum AiCreditPack {
    STARTER_25(25, 999),
    POWER_75(75, 2499),
    ELITE_200(200, 5999);

    private final int credits;
    private final int amountCents;

    AiCreditPack(int credits, int amountCents) {
        this.credits = credits;
        this.amountCents = amountCents;
    }

    public int credits() {
        return credits;
    }

    public int amountCents() {
        return amountCents;
    }
}
