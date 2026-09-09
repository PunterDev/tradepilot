package com.tradepilot.security;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DatabaseRateLimiterTests {

    @Test
    void allowsRequestsWithinDatabaseWindowLimit() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        Instant now = Instant.parse("2026-08-31T12:00:00Z");
        when(jdbcTemplate.queryForMap(any(String.class), eq("auth-login:ip"), any(Timestamp.class), any(Timestamp.class)))
                .thenReturn(Map.of("request_count", 2, "expires_at", Timestamp.from(now.plusSeconds(60))));

        DatabaseRateLimiter limiter = new DatabaseRateLimiter(jdbcTemplate, Clock.fixed(now, ZoneOffset.UTC));

        RateLimitResult result = limiter.check("auth-login:ip", 3, 60);

        assertThat(result.allowed()).isTrue();
        verify(jdbcTemplate).queryForMap(any(String.class), eq("auth-login:ip"), any(Timestamp.class), any(Timestamp.class));
    }

    @Test
    void blocksRequestsAboveDatabaseWindowLimit() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        Instant now = Instant.parse("2026-08-31T12:00:00Z");
        when(jdbcTemplate.queryForMap(any(String.class), eq("auth-login:ip"), any(Timestamp.class), any(Timestamp.class)))
                .thenReturn(Map.of("request_count", 4, "expires_at", Timestamp.from(now.plusSeconds(60))));

        DatabaseRateLimiter limiter = new DatabaseRateLimiter(jdbcTemplate, Clock.fixed(now, ZoneOffset.UTC));

        RateLimitResult result = limiter.check("auth-login:ip", 3, 60);

        assertThat(result.allowed()).isFalse();
        assertThat(result.retryAfterSeconds()).isEqualTo(60);
    }
}
