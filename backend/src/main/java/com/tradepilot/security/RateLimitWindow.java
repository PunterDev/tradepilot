package com.tradepilot.security;

final class RateLimitWindow {
    private final long startedAtMs;
    private int count;

    RateLimitWindow(long startedAtMs, int count) {
        this.startedAtMs = startedAtMs;
        this.count = count;
    }

    boolean isExpired(long nowMs, long windowMs) {
        return nowMs - startedAtMs >= windowMs;
    }

    boolean isAtLimit(int maxRequests) {
        return count >= maxRequests;
    }

    void increment() {
        count++;
    }

    long retryAfterSeconds(long nowMs, long windowMs) {
        long retryAfterMs = Math.max(0, windowMs - (nowMs - startedAtMs));
        return Math.max(1, (retryAfterMs + 999) / 1000);
    }
}
