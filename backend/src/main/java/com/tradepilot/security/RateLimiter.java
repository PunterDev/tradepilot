package com.tradepilot.security;

public interface RateLimiter {
    RateLimitResult check(String key, int maxRequests, long windowSeconds);
}
