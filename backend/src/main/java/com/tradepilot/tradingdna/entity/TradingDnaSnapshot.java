package com.tradepilot.tradingdna.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.math.BigDecimal;

@Entity
@Table(
        name = "trading_dna_snapshots",
        indexes = {
                @Index(name = "idx_trading_dna_user_id", columnList = "user_id"),
                @Index(name = "idx_trading_dna_user_account_timeframe", columnList = "user_id, account_id, days"),
                @Index(name = "idx_trading_dna_data_hash", columnList = "data_hash"),
                @Index(name = "idx_trading_dna_user_account_hash", columnList = "user_id, account_id, data_hash"),
                @Index(name = "idx_trading_dna_user_account_days_hash", columnList = "user_id, account_id, days, include_all_accounts, data_hash"),
                @Index(name = "idx_trading_dna_user_created_at", columnList = "user_id, created_at")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TradingDnaSnapshot {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "account_id")
    private Long accountId;

    @Column(nullable = false)
    private Integer days;

    @Column(name = "include_all_accounts", nullable = false)
    private Boolean includeAllAccounts;

    @Column(name = "data_hash", nullable = false, length = 128)
    private String dataHash;

    @Column(nullable = false, length = 32)
    private String status;

    @Column(name = "total_trades")
    private Integer totalTrades;

    @Column(name = "ai_response_json", columnDefinition = "TEXT")
    private String aiResponseJson;

    @Column(name = "context_json", columnDefinition = "TEXT")
    private String contextJson;

    @Column(name = "timeline_json", columnDefinition = "TEXT")
    private String timelineJson;

    @Column(length = 80)
    private String model;

    @Column(name = "prompt_tokens")
    private Integer promptTokens;

    @Column(name = "completion_tokens")
    private Integer completionTokens;

    @Column(name = "total_tokens")
    private Integer totalTokens;

    @Column(name = "estimated_cost", precision = 12, scale = 6)
    private BigDecimal estimatedCost;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
