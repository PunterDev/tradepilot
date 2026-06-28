package com.tradepilot.billing;

import jakarta.validation.constraints.NotNull;

public record CreditPackCheckoutRequest(@NotNull AiCreditPack packId) {
}
