package com.tradepilot.e2e.support;

public record E2eUserData(
        long userId,
        String email,
        String password,
        long primaryAccountId,
        long secondaryAccountId,
        long tradeId
) {
}
