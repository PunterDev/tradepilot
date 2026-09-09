package com.tradepilot.e2e.support;

public record E2eTestData(
        E2eUserData proUser,
        E2eUserData freeUser,
        E2eUserData unverifiedUser
) {
}
