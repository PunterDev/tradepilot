package com.tradepilot.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Getter
@Setter
@Table(
        name = "ai_trade_analyses",
        indexes = {
                @Index(name = "idx_ai_trade_analysis_user_id", columnList = "user_id"),
                @Index(name = "idx_ai_trade_analysis_trade_id", columnList = "trade_id"),
                @Index(name = "idx_ai_trade_analysis_hash", columnList = "review_hash"),
                @Index(name = "idx_ai_trade_analysis_user_cache", columnList = "user_id, trade_id, review_version, review_hash")
        },
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_ai_trade_analysis_cache",
                        columnNames = {"user_id", "trade_id", "review_version", "review_hash"}
                )
        }
)
public class AiTradeAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "trade_id", nullable = false)
    private Long tradeId;

    @Column(name = "review_version", nullable = false, length = 32)
    private String reviewVersion;

    @Column(name = "review_hash", nullable = false, length = 128)
    private String reviewHash;

    @Column(length = 32, nullable = false)
    private String status;

    @Column(length = 32)
    private String symbol;

    @Column(precision = 14, scale = 2)
    private BigDecimal pnl;

    @Column(length = 8)
    private String grade;

    @Column(name = "execution_score")
    private Integer executionScore;

    @Column(name = "discipline_score")
    private Integer disciplineScore;

    @Column(name = "risk_score")
    private Integer riskScore;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(name = "market_context", columnDefinition = "TEXT")
    private String marketContext;

    @Column(name = "entry_analysis", columnDefinition = "TEXT")
    private String entryAnalysis;

    @Column(name = "exit_analysis", columnDefinition = "TEXT")
    private String exitAnalysis;

    @Column(name = "risk_analysis", columnDefinition = "TEXT")
    private String riskAnalysis;

    @Column(name = "psychology_analysis", columnDefinition = "TEXT")
    private String psychologyAnalysis;

    @Column(name = "chart_analysis", columnDefinition = "TEXT")
    private String chartAnalysis;

    @Column(name = "mistakes_json", columnDefinition = "TEXT")
    private String mistakesJson;

    @Column(name = "strengths_json", columnDefinition = "TEXT")
    private String strengthsJson;

    @Column(name = "improvement_plan_json", columnDefinition = "TEXT")
    private String improvementPlanJson;

    @Column(name = "tags_json", columnDefinition = "TEXT")
    private String tagsJson;

    @Column(name = "has_chart_image", nullable = false)
    private boolean hasChartImage;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
