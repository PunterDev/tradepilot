package com.tradepilot.security;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryRateLimiterTests {

    @Test
    void allowsRequestsWithinLimitAndBlocksOverLimit() {
        MutableClock clock = new MutableClock();
        InMemoryRateLimiter limiter = new InMemoryRateLimiter(clock);

        assertThat(limiter.check("login:203.0.113.10", 2, 60).allowed()).isTrue();
        assertThat(limiter.check("login:203.0.113.10", 2, 60).allowed()).isTrue();

        RateLimitResult blocked = limiter.check("login:203.0.113.10", 2, 60);

        assertThat(blocked.allowed()).isFalse();
        assertThat(blocked.retryAfterSeconds()).isEqualTo(60);
    }

    @Test
    void resetsWindowAfterConfiguredDuration() {
        MutableClock clock = new MutableClock();
        InMemoryRateLimiter limiter = new InMemoryRateLimiter(clock);

        assertThat(limiter.check("login:203.0.113.11", 1, 60).allowed()).isTrue();
        assertThat(limiter.check("login:203.0.113.11", 1, 60).allowed()).isFalse();

        clock.advanceSeconds(61);

        assertThat(limiter.check("login:203.0.113.11", 1, 60).allowed()).isTrue();
    }

    @Test
    void invalidLimitFailsClosedWithoutThrowing() {
        InMemoryRateLimiter limiter = new InMemoryRateLimiter(new MutableClock());

        RateLimitResult result = limiter.check("login:203.0.113.12", 0, 60);

        assertThat(result.allowed()).isFalse();
        assertThat(result.retryAfterSeconds()).isEqualTo(60);
    }

    @Test
    void doesNotDeclareNestedWindowClass() {
        assertThat(InMemoryRateLimiter.class.getDeclaredClasses())
                .extracting(Class::getSimpleName)
                .doesNotContain("Window");
    }

    @Test
    void topLevelHelperClassesAreLoadableAtRuntime() throws Exception {
        assertThat(Class.forName("com.tradepilot.security.RateLimitWindow"))
                .isSameAs(RateLimitWindow.class);
        assertThat(Class.forName("com.tradepilot.security.RateLimitResult"))
                .isSameAs(RateLimitResult.class);
        assertThat(RateLimitWindow.class.getEnclosingClass()).isNull();
        assertThat(RateLimitResult.class.getEnclosingClass()).isNull();

        InMemoryRateLimiter limiter = new InMemoryRateLimiter(new MutableClock());
        assertThat(limiter.check("packaging-login-check", 1, 60).allowed()).isTrue();
        assertThat(limiter.check("packaging-login-check", 1, 60).allowed()).isFalse();
    }

    private static final class MutableClock extends Clock {
        private Instant instant = Instant.parse("2026-06-28T00:00:00Z");

        void advanceSeconds(long seconds) {
            instant = instant.plusSeconds(seconds);
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
