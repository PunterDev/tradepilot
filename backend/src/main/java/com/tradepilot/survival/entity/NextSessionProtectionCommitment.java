package com.tradepilot.survival.entity;

import com.tradepilot.entity.TradingAccount;
import com.tradepilot.entity.User;
import com.tradepilot.survival.*;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;

@Entity @Table(name = "next_session_protection_commitments", uniqueConstraints = @UniqueConstraint(name = "uk_next_session_protection_account_date", columnNames = {"account_id", "effective_session_date"}))
public class NextSessionProtectionCommitment {
 @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
 @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="user_id", nullable=false) private User user;
 @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="account_id", nullable=false) private TradingAccount account;
 @Column(name="effective_session_date", nullable=false) private LocalDate effectiveSessionDate;
 @Column(name="max_normalized_exposure", precision=10, scale=2) private BigDecimal maxNormalizedExposure;
 private Integer maxAttempts;
 @Column(name="max_session_loss", precision=18, scale=2) private BigDecimal maxSessionLoss;
 private Integer postLossPauseMinutes;
 private LocalTime allowedWindowStart; private LocalTime allowedWindowEnd;
 private Boolean requireFreshSetupConfirmation;
 @Enumerated(EnumType.STRING) @Column(nullable=false) private ProtectionCommitmentSource source;
 @Enumerated(EnumType.STRING) @Column(nullable=false) private ProtectionCommitmentStatus status;
 private LocalDateTime acceptedAt;
 @Column(nullable=false) private LocalDateTime createdAt; @Column(nullable=false) private LocalDateTime updatedAt;
 @PrePersist void create(){var n=LocalDateTime.now();if(createdAt==null)createdAt=n;updatedAt=n;} @PreUpdate void update(){updatedAt=LocalDateTime.now();}
 public Long getId(){return id;} public User getUser(){return user;} public TradingAccount getAccount(){return account;} public LocalDate getEffectiveSessionDate(){return effectiveSessionDate;} public BigDecimal getMaxNormalizedExposure(){return maxNormalizedExposure;} public Integer getMaxAttempts(){return maxAttempts;} public BigDecimal getMaxSessionLoss(){return maxSessionLoss;} public Integer getPostLossPauseMinutes(){return postLossPauseMinutes;} public LocalTime getAllowedWindowStart(){return allowedWindowStart;} public LocalTime getAllowedWindowEnd(){return allowedWindowEnd;} public Boolean getRequireFreshSetupConfirmation(){return requireFreshSetupConfirmation;} public ProtectionCommitmentSource getSource(){return source;} public ProtectionCommitmentStatus getStatus(){return status;} public LocalDateTime getAcceptedAt(){return acceptedAt;}
 public void setUser(User v){user=v;} public void setAccount(TradingAccount v){account=v;} public void setEffectiveSessionDate(LocalDate v){effectiveSessionDate=v;} public void setMaxNormalizedExposure(BigDecimal v){maxNormalizedExposure=v;} public void setMaxAttempts(Integer v){maxAttempts=v;} public void setMaxSessionLoss(BigDecimal v){maxSessionLoss=v;} public void setPostLossPauseMinutes(Integer v){postLossPauseMinutes=v;} public void setAllowedWindowStart(LocalTime v){allowedWindowStart=v;} public void setAllowedWindowEnd(LocalTime v){allowedWindowEnd=v;} public void setRequireFreshSetupConfirmation(Boolean v){requireFreshSetupConfirmation=v;} public void setSource(ProtectionCommitmentSource v){source=v;} public void setStatus(ProtectionCommitmentStatus v){status=v;} public void setAcceptedAt(LocalDateTime v){acceptedAt=v;}
}
