package com.tradepilot.security;

public record RateLimitResult(boolean allowed, long retryAfterSeconds) {
    static RateLimitResult allowed(long retryAfterSeconds) {
        return new RateLimitResult(true, retryAfterSeconds);
    }

    static RateLimitResult blocked(long retryAfterSeconds) {
        return new RateLimitResult(false, retryAfterSeconds);
    }
}
