package com.tradepilot.security;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;

@Component
@ConditionalOnProperty(name = "app.rate-limit.store", havingValue = "database")
public class DatabaseRateLimiter implements RateLimiter {
    private final JdbcTemplate jdbcTemplate;
    private final Clock clock;

    @Autowired
    public DatabaseRateLimiter(JdbcTemplate jdbcTemplate) {
        this(jdbcTemplate, Clock.systemUTC());
    }

    DatabaseRateLimiter(JdbcTemplate jdbcTemplate, Clock clock) {
        this.jdbcTemplate = jdbcTemplate;
        this.clock = clock;
    }

    @Override
    public RateLimitResult check(String key, int maxRequests, long windowSeconds) {
        if (maxRequests <= 0) {
            return RateLimitResult.blocked(Math.max(1, windowSeconds));
        }

        long safeWindowSeconds = Math.max(1, windowSeconds);
        Instant now = clock.instant();
        Instant expiresAt = now.plusSeconds(safeWindowSeconds);
        Map<String, Object> row = jdbcTemplate.queryForMap("""
                insert into rate_limit_windows(rate_key, window_start, request_count, expires_at)
                values (?, ?, 1, ?)
                on conflict (rate_key) do update set
                    window_start = case
                        when rate_limit_windows.expires_at <= excluded.window_start then excluded.window_start
                        else rate_limit_windows.window_start
                    end,
                    request_count = case
                        when rate_limit_windows.expires_at <= excluded.window_start then 1
                        else rate_limit_windows.request_count + 1
                    end,
                    expires_at = case
                        when rate_limit_windows.expires_at <= excluded.window_start then excluded.expires_at
                        else rate_limit_windows.expires_at
                    end,
                    updated_at = current_timestamp
                returning request_count, expires_at
                """, key, Timestamp.from(now), Timestamp.from(expiresAt));

        int requestCount = ((Number) row.get("request_count")).intValue();
        Instant retryAt = ((Timestamp) row.get("expires_at")).toInstant();
        if (requestCount <= maxRequests) {
            return RateLimitResult.allowed(0);
        }
        long retryAfterSeconds = Math.max(1, retryAt.getEpochSecond() - now.getEpochSecond());
        return RateLimitResult.blocked(retryAfterSeconds);
    }
}
