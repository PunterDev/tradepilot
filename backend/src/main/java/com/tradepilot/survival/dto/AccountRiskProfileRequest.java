package com.tradepilot.survival.dto;

import com.tradepilot.survival.AccountStage;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

public record AccountRiskProfileRequest(BigDecimal currentBalance, BigDecimal failureThreshold, BigDecimal dailyLossLimit,
        BigDecimal plannedRiskPerTrade, Integer personalMaxContracts, Integer firmMaxContracts,
        LocalTime primaryWindowStart, LocalTime primaryWindowEnd, LocalDate renewalDate, BigDecimal payoutTarget,
        LocalDate payoutDate, String timezone, AccountStage accountStage, LocalDate effectiveDate) {}
