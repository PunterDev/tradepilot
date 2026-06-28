package com.tradepilot.dto.account;

import com.tradepilot.enums.PropFirmType;
import com.tradepilot.enums.TradingAccountStatus;
import com.tradepilot.enums.TradingAccountType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record TradingAccountRequest(
        @NotBlank String name,
        @NotNull PropFirmType propFirm,
        String accountNumber,
        TradingAccountType accountType,
        BigDecimal startingBalance,
        @DecimalMin(value = "0.01", message = "Default risk amount must be greater than 0")
        BigDecimal defaultRiskAmount,
        TradingAccountStatus status
) {}
