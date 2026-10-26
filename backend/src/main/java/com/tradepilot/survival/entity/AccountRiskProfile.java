package com.tradepilot.survival.entity;

import com.tradepilot.entity.TradingAccount;
import com.tradepilot.entity.User;
import com.tradepilot.survival.AccountStage;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;

@Entity
@Table(name = "account_risk_profiles", uniqueConstraints = @UniqueConstraint(name = "uk_account_risk_profiles_account", columnNames = "account_id"))
public class AccountRiskProfile {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "user_id", nullable = false) private User user;
    @OneToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "account_id", nullable = false) private TradingAccount account;
    @Column(name = "current_balance", precision = 18, scale = 2) private BigDecimal currentBalance;
    @Column(name = "failure_threshold", precision = 18, scale = 2) private BigDecimal failureThreshold;
    @Column(name = "daily_loss_limit", precision = 18, scale = 2) private BigDecimal dailyLossLimit;
    @Column(name = "planned_risk_per_trade", precision = 18, scale = 2) private BigDecimal plannedRiskPerTrade;
    @Column(name = "personal_max_contracts") private Integer personalMaxContracts;
    @Column(name = "firm_max_contracts") private Integer firmMaxContracts;
    @Column(name = "primary_window_start") private LocalTime primaryWindowStart;
    @Column(name = "primary_window_end") private LocalTime primaryWindowEnd;
    private LocalDate renewalDate;
    @Column(name = "payout_target", precision = 18, scale = 2) private BigDecimal payoutTarget;
    private LocalDate payoutDate;
    private String timezone;
    @Enumerated(EnumType.STRING) @Column(name = "account_stage") private AccountStage accountStage;
    private LocalDate effectiveDate;
    @Column(name = "created_at", nullable = false) private LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false) private LocalDateTime updatedAt;
    @PrePersist void create(){ var now=LocalDateTime.now(); if(createdAt==null) createdAt=now; updatedAt=now; }
    @PreUpdate void update(){ updatedAt=LocalDateTime.now(); }
    public Long getId(){return id;} public User getUser(){return user;} public TradingAccount getAccount(){return account;} public BigDecimal getCurrentBalance(){return currentBalance;} public BigDecimal getFailureThreshold(){return failureThreshold;} public BigDecimal getDailyLossLimit(){return dailyLossLimit;} public BigDecimal getPlannedRiskPerTrade(){return plannedRiskPerTrade;} public Integer getPersonalMaxContracts(){return personalMaxContracts;} public Integer getFirmMaxContracts(){return firmMaxContracts;} public LocalTime getPrimaryWindowStart(){return primaryWindowStart;} public LocalTime getPrimaryWindowEnd(){return primaryWindowEnd;} public LocalDate getRenewalDate(){return renewalDate;} public BigDecimal getPayoutTarget(){return payoutTarget;} public LocalDate getPayoutDate(){return payoutDate;} public String getTimezone(){return timezone;} public AccountStage getAccountStage(){return accountStage;} public LocalDate getEffectiveDate(){return effectiveDate;} public LocalDateTime getUpdatedAt(){return updatedAt;}
    public void setUser(User v){user=v;} public void setAccount(TradingAccount v){account=v;} public void setCurrentBalance(BigDecimal v){currentBalance=v;} public void setFailureThreshold(BigDecimal v){failureThreshold=v;} public void setDailyLossLimit(BigDecimal v){dailyLossLimit=v;} public void setPlannedRiskPerTrade(BigDecimal v){plannedRiskPerTrade=v;} public void setPersonalMaxContracts(Integer v){personalMaxContracts=v;} public void setFirmMaxContracts(Integer v){firmMaxContracts=v;} public void setPrimaryWindowStart(LocalTime v){primaryWindowStart=v;} public void setPrimaryWindowEnd(LocalTime v){primaryWindowEnd=v;} public void setRenewalDate(LocalDate v){renewalDate=v;} public void setPayoutTarget(BigDecimal v){payoutTarget=v;} public void setPayoutDate(LocalDate v){payoutDate=v;} public void setTimezone(String v){timezone=v;} public void setAccountStage(AccountStage v){accountStage=v;} public void setEffectiveDate(LocalDate v){effectiveDate=v;}
}
