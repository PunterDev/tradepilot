package com.tradepilot.backtest.dto;

public record BacktestAccessResponse(boolean allowed, String entitlementKey, String label, String badge) {
    public static BacktestAccessResponse privateBeta(boolean allowed) {
        return new BacktestAccessResponse(allowed, "PLAYBOOK_BACKTEST_BETA", "Playbook Backtest", "Private Research");
    }
}
