package com.tradepilot.dto.account;

import com.tradepilot.enums.PropFirmType;
import com.tradepilot.enums.TradingAccountStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record TradingAccountRequest(
        @NotBlank String name,
        @NotNull PropFirmType propFirm,
        String accountNumber,
        BigDecimal startingBalance,
        TradingAccountStatus status
) {}