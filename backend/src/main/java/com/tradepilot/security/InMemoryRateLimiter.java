package com.tradepilot.security;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class InMemoryRateLimiter {
    private final Clock clock;
    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    public InMemoryRateLimiter() {
        this(Clock.systemUTC());
    }

    InMemoryRateLimiter(Clock clock) {
        this.clock = clock;
    }

    public synchronized RateLimitResult check(String key, int maxRequests, long windowSeconds) {
        long now = clock.millis();
        long windowMs = Math.max(1, windowSeconds) * 1000L;
        Window window = windows.get(key);
        if (window == null || now - window.startedAtMs >= windowMs) {
            windows.put(key, new Window(now, 1));
            cleanupExpired(now, windowMs);
            return RateLimitResult.allowed(0);
        }

        if (window.count >= maxRequests) {
            long retryAfterMs = Math.max(0, windowMs - (now - window.startedAtMs));
            return RateLimitResult.blocked(Math.max(1, (retryAfterMs + 999) / 1000));
        }

        window.count++;
        return RateLimitResult.allowed(0);
    }

    private void cleanupExpired(long now, long windowMs) {
        if (windows.size() < 10_000) {
            return;
        }
        Iterator<Map.Entry<String, Window>> iterator = windows.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Window> entry = iterator.next();
            if (now - entry.getValue().startedAtMs >= windowMs) {
                iterator.remove();
            }
        }
    }

    private static final class Window {
        private final long startedAtMs;
        private int count;

        private Window(long startedAtMs, int count) {
            this.startedAtMs = startedAtMs;
            this.count = count;
        }
    }

    public record RateLimitResult(boolean allowed, long retryAfterSeconds) {
        private static RateLimitResult allowed(long retryAfterSeconds) {
            return new RateLimitResult(true, retryAfterSeconds);
        }

        private static RateLimitResult blocked(long retryAfterSeconds) {
            return new RateLimitResult(false, retryAfterSeconds);
        }
    }
}
