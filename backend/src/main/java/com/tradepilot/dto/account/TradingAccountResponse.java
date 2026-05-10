package com.tradepilot.dto.account;

import com.tradepilot.enums.PropFirmType;
import com.tradepilot.enums.TradingAccountStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class TradingAccountResponse {
    private Long id;
    private String name;
    private PropFirmType propFirm;
    private String accountNumber;
    private BigDecimal startingBalance;
    private TradingAccountStatus status;
    private LocalDateTime createdAt;
}