package com.tradepilot.coach.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "ai_coach_snapshot",
        indexes = {
                @Index(name = "idx_ai_coach_snapshot_user_id", columnList = "user_id"),
                @Index(name = "idx_ai_coach_snapshot_account_id", columnList = "account_id"),
                @Index(name = "idx_ai_coach_snapshot_user_account_type_period", columnList = "user_id, account_id, snapshot_type, period_start, period_end")
        },
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_ai_coach_snapshot",
                        columnNames = {
                                "user_id",
                                "account_id",
                                "snapshot_type",
                                "period_start",
                                "period_end",
                                "review_hash"
                        }
                )
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiCoachSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "account_id", nullable = false)
    private Long accountId;

    @Enumerated(EnumType.STRING)
    @Column(name = "snapshot_type", nullable = false, length = 20)
    private CoachSnapshotType snapshotType;

    @Column(name = "period_start", nullable = false)
    private LocalDate periodStart;

    @Column(name = "period_end", nullable = false)
    private LocalDate periodEnd;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CoachSnapshotStatus status;

    @Column(name = "total_trades")
    private Integer totalTrades;

    @Column(name = "net_pnl", precision = 12, scale = 2)
    private BigDecimal netPnl;

    @Column(name = "win_rate", precision = 6, scale = 2)
    private BigDecimal winRate;

    @Column(name = "profit_factor", precision = 8, scale = 2)
    private BigDecimal profitFactor;

    @Column(name = "best_setup", length = 100)
    private String bestSetup;

    @Column(name = "worst_setup", length = 100)
    private String worstSetup;

    @Column(name = "best_session", length = 50)
    private String bestSession;

    @Column(name = "worst_session", length = 50)
    private String worstSession;

    @Column(name = "biggest_mistake", length = 255)
    private String biggestMistake;

    @Column(name = "deterministic_summary_json", columnDefinition = "TEXT")
    private String deterministicSummaryJson;

    @Column(name = "ai_response_json", columnDefinition = "TEXT")
    private String aiResponseJson;

    @Column(name = "review_hash", length = 128)
    private String reviewHash;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;

        if (status == null) {
            status = CoachSnapshotStatus.PENDING;
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
