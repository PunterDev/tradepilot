package com.tradepilot.security;

import org.springframework.stereotype.Component;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import java.time.Clock;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@ConditionalOnProperty(name = "app.rate-limit.store", havingValue = "memory", matchIfMissing = true)
public class InMemoryRateLimiter implements RateLimiter {
    private final Clock clock;
    private final Map<String, RateLimitWindow> windows = new ConcurrentHashMap<>();

    public InMemoryRateLimiter() {
        this(Clock.systemUTC());
    }

    InMemoryRateLimiter(Clock clock) {
        this.clock = clock;
    }

    @Override
    public synchronized RateLimitResult check(String key, int maxRequests, long windowSeconds) {
        if (maxRequests <= 0) {
            return RateLimitResult.blocked(Math.max(1, windowSeconds));
        }
        long now = clock.millis();
        long windowMs = Math.max(1, windowSeconds) * 1000L;
        RateLimitWindow window = windows.get(key);
        if (window == null || window.isExpired(now, windowMs)) {
            windows.put(key, new RateLimitWindow(now, 1));
            cleanupExpired(now, windowMs);
            return RateLimitResult.allowed(0);
        }

        if (window.isAtLimit(maxRequests)) {
            return RateLimitResult.blocked(window.retryAfterSeconds(now, windowMs));
        }

        window.increment();
        return RateLimitResult.allowed(0);
    }

    private void cleanupExpired(long now, long windowMs) {
        if (windows.size() < 10_000) {
            return;
        }
        Iterator<Map.Entry<String, RateLimitWindow>> iterator = windows.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, RateLimitWindow> entry = iterator.next();
            if (entry.getValue().isExpired(now, windowMs)) {
                iterator.remove();
            }
        }
    }
}
