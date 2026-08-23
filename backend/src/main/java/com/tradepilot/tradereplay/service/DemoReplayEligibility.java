package com.tradepilot.tradereplay.service;

import com.tradepilot.entity.Trade;

public final class DemoReplayEligibility {
    public static final String DEMO_PREFIX = "DEV-DEMO-";
    public static final String REPLAY_TOKEN = "-REPLAY-";
    public static final String GENERAL_TOKEN = "-GENERAL-";
    public static final String UNAVAILABLE_CODE = "DEMO_REPLAY_UNAVAILABLE";
    public static final String UNAVAILABLE_MESSAGE = "Replay is not available for this demo trade.";

    private DemoReplayEligibility() {
    }

    public static boolean isDemoTrade(Trade trade) {
        String key = trade == null ? null : trade.getExternalTradeKey();
        return key != null && key.startsWith(DEMO_PREFIX);
    }

    public static boolean isReplayAllowed(Trade trade) {
        if (!isDemoTrade(trade)) {
            return true;
        }
        String key = trade.getExternalTradeKey();
        if (key.contains(REPLAY_TOKEN)) {
            return true;
        }
        if (key.contains(GENERAL_TOKEN)) {
            return false;
        }
        return true;
    }
}
