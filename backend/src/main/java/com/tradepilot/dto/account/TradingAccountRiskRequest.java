package com.tradepilot.dto.account;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record TradingAccountRiskRequest(
        @NotNull(message = "Default risk amount is required")
        @DecimalMin(value = "0.01", message = "Default risk amount must be greater than 0")
        BigDecimal defaultRiskAmount
) {}
