package com.tradepilot.survival;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Keeps Account Survival's production clock on the host clock while allowing
 * the isolated e2e profile to create a deterministic, chronological journey.
 */
@Component
public class AccountSurvivalTimeSource {
    private final boolean e2eProfile;
    private final AtomicReference<Clock> clock = new AtomicReference<>(Clock.systemDefaultZone());

    public AccountSurvivalTimeSource(Environment environment) {
        e2eProfile = Arrays.asList(environment.getActiveProfiles()).contains("e2e");
    }

    public LocalDateTime now() {
        return LocalDateTime.now(clock.get());
    }

    public LocalDate today(ZoneId zone) {
        return LocalDate.now(clock.get().withZone(zone));
    }

    public void resetE2eDate(LocalDate date) {
        requireE2eProfile();
        clock.set(fixedAtMidday(date));
    }

    public void advanceE2eDate(LocalDate date) {
        requireE2eProfile();
        if (!date.isAfter(today(ZoneId.systemDefault()))) {
            throw new IllegalArgumentException("The e2e clock can only move forward.");
        }
        clock.set(fixedAtMidday(date));
    }

    private Clock fixedAtMidday(LocalDate date) {
        ZoneId zone = ZoneId.systemDefault();
        return Clock.fixed(date.atTime(LocalTime.NOON).atZone(zone).toInstant(), zone);
    }

    private void requireE2eProfile() {
        if (!e2eProfile) {
            throw new IllegalStateException("The Account Survival test clock is available only in the e2e profile.");
        }
    }
}
