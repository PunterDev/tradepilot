package com.tradepilot.dev.e2e;

public record E2eTestDataResponse(
        E2eUserData proUser,
        E2eUserData freeUser,
        E2eUserData unverifiedUser,
        String message
) {
    public record E2eUserData(
            Long userId,
            String email,
            String password,
            Long primaryAccountId,
            Long secondaryAccountId,
            Long tradeId
    ) {
    }
}
