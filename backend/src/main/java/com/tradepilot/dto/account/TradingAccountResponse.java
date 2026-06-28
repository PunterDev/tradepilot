package com.tradepilot.dto.account;

import com.tradepilot.enums.PropFirmType;
import com.tradepilot.enums.TradingAccountStatus;
import com.tradepilot.enums.TradingAccountType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class TradingAccountResponse {
    private Long id;
    private String name;
    private PropFirmType propFirm;
    private String accountNumber;
    private TradingAccountType accountType;
    private BigDecimal startingBalance;
    private BigDecimal defaultRiskAmount;
    private TradingAccountStatus status;
    private LocalDateTime createdAt;

    public TradingAccountResponse() {
    }

    public TradingAccountResponse(Long id, String name, PropFirmType propFirm, String accountNumber,
                                  TradingAccountType accountType, BigDecimal startingBalance,
                                  BigDecimal defaultRiskAmount, TradingAccountStatus status, LocalDateTime createdAt) {
        this.id = id;
        this.name = name;
        this.propFirm = propFirm;
        this.accountNumber = accountNumber;
        this.accountType = accountType;
        this.startingBalance = startingBalance;
        this.defaultRiskAmount = defaultRiskAmount;
        this.status = status;
        this.createdAt = createdAt;
    }

    public static TradingAccountResponse of(Long id, String name, PropFirmType propFirm, String accountNumber,
                                            TradingAccountType accountType, BigDecimal startingBalance,
                                            BigDecimal defaultRiskAmount, TradingAccountStatus status, LocalDateTime createdAt) {
        return new TradingAccountResponse(id, name, propFirm, accountNumber, accountType, startingBalance, defaultRiskAmount, status, createdAt);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public PropFirmType getPropFirm() {
        return propFirm;
    }

    public void setPropFirm(PropFirmType propFirm) {
        this.propFirm = propFirm;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public TradingAccountType getAccountType() {
        return accountType;
    }

    public void setAccountType(TradingAccountType accountType) {
        this.accountType = accountType;
    }

    public BigDecimal getStartingBalance() {
        return startingBalance;
    }

    public void setStartingBalance(BigDecimal startingBalance) {
        this.startingBalance = startingBalance;
    }

    public BigDecimal getDefaultRiskAmount() {
        return defaultRiskAmount;
    }

    public void setDefaultRiskAmount(BigDecimal defaultRiskAmount) {
        this.defaultRiskAmount = defaultRiskAmount;
    }

    public TradingAccountStatus getStatus() {
        return status;
    }

    public void setStatus(TradingAccountStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
