package com.tradepilot.tradereplay.entity;

import com.tradepilot.entity.Trade;
import com.tradepilot.entity.User;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "trade_replays",
        indexes = {
                @Index(name = "idx_trade_replays_user_trade", columnList = "user_id, trade_id"),
                @Index(name = "idx_trade_replays_status_next_retry", columnList = "status, next_retry_at"),
                @Index(name = "idx_trade_replays_eligible_at", columnList = "eligible_at")
        },
        uniqueConstraints = {@UniqueConstraint(name = "uk_trade_replays_user_trade", columnNames = {"user_id", "trade_id"})})
public class TradeReplay {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "trade_id", nullable = false)
    private Trade trade;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private TradeReplayStatus status = TradeReplayStatus.PENDING;
    @Column(name = "original_broker_symbol", length = 40)
    private String originalBrokerSymbol;
    @Column(name = "resolved_provider_symbol", length = 40)
    private String resolvedProviderSymbol;
    @Column(length = 40)
    private String provider;
    @Column(name = "candle_interval", length = 20)
    private String candleInterval;
    @Column(name = "window_start")
    private Instant windowStart;
    @Column(name = "window_end")
    private Instant windowEnd;
    @Column(name = "eligible_at")
    private Instant eligibleAt;
    @Column(name = "entry_timestamp")
    private Instant entryTimestamp;
    @Column(name = "final_exit_timestamp")
    private Instant finalExitTimestamp;
    @Column(name = "generated_at")
    private Instant generatedAt;
    @Column(name = "deterministic_analysis_version", length = 40)
    private String deterministicAnalysisVersion;
    @Column(name = "ai_analysis_version", length = 40)
    private String aiAnalysisVersion;
    @Column(name = "failure_code", length = 80)
    private String failureCode;
    @Column(name = "sanitized_failure_message", columnDefinition = "TEXT")
    private String sanitizedFailureMessage;
    @Column(name = "retry_count", nullable = false)
    private int retryCount;
    @Column(name = "next_retry_at")
    private Instant nextRetryAt;
    @Column(name = "metrics_json", columnDefinition = "TEXT")
    private String metricsJson;
    @Column(name = "ai_summary", columnDefinition = "TEXT")
    private String aiSummary;
    @Column(name = "reflection_question", columnDefinition = "TEXT")
    private String reflectionQuestion;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    public void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    public void onUpdate() {
        updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public Trade getTrade() { return trade; }
    public void setTrade(Trade trade) { this.trade = trade; }
    public TradeReplayStatus getStatus() { return status; }
    public void setStatus(TradeReplayStatus status) { this.status = status; }
    public String getOriginalBrokerSymbol() { return originalBrokerSymbol; }
    public void setOriginalBrokerSymbol(String originalBrokerSymbol) { this.originalBrokerSymbol = originalBrokerSymbol; }
    public String getResolvedProviderSymbol() { return resolvedProviderSymbol; }
    public void setResolvedProviderSymbol(String resolvedProviderSymbol) { this.resolvedProviderSymbol = resolvedProviderSymbol; }
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public String getCandleInterval() { return candleInterval; }
    public void setCandleInterval(String candleInterval) { this.candleInterval = candleInterval; }
    public Instant getWindowStart() { return windowStart; }
    public void setWindowStart(Instant windowStart) { this.windowStart = windowStart; }
    public Instant getWindowEnd() { return windowEnd; }
    public void setWindowEnd(Instant windowEnd) { this.windowEnd = windowEnd; }
    public Instant getEligibleAt() { return eligibleAt; }
    public void setEligibleAt(Instant eligibleAt) { this.eligibleAt = eligibleAt; }
    public Instant getEntryTimestamp() { return entryTimestamp; }
    public void setEntryTimestamp(Instant entryTimestamp) { this.entryTimestamp = entryTimestamp; }
    public Instant getFinalExitTimestamp() { return finalExitTimestamp; }
    public void setFinalExitTimestamp(Instant finalExitTimestamp) { this.finalExitTimestamp = finalExitTimestamp; }
    public Instant getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(Instant generatedAt) { this.generatedAt = generatedAt; }
    public String getDeterministicAnalysisVersion() { return deterministicAnalysisVersion; }
    public void setDeterministicAnalysisVersion(String deterministicAnalysisVersion) { this.deterministicAnalysisVersion = deterministicAnalysisVersion; }
    public String getAiAnalysisVersion() { return aiAnalysisVersion; }
    public void setAiAnalysisVersion(String aiAnalysisVersion) { this.aiAnalysisVersion = aiAnalysisVersion; }
    public String getFailureCode() { return failureCode; }
    public void setFailureCode(String failureCode) { this.failureCode = failureCode; }
    public String getSanitizedFailureMessage() { return sanitizedFailureMessage; }
    public void setSanitizedFailureMessage(String sanitizedFailureMessage) { this.sanitizedFailureMessage = sanitizedFailureMessage; }
    public int getRetryCount() { return retryCount; }
    public void setRetryCount(int retryCount) { this.retryCount = retryCount; }
    public Instant getNextRetryAt() { return nextRetryAt; }
    public void setNextRetryAt(Instant nextRetryAt) { this.nextRetryAt = nextRetryAt; }
    public String getMetricsJson() { return metricsJson; }
    public void setMetricsJson(String metricsJson) { this.metricsJson = metricsJson; }
    public String getAiSummary() { return aiSummary; }
    public void setAiSummary(String aiSummary) { this.aiSummary = aiSummary; }
    public String getReflectionQuestion() { return reflectionQuestion; }
    public void setReflectionQuestion(String reflectionQuestion) { this.reflectionQuestion = reflectionQuestion; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
