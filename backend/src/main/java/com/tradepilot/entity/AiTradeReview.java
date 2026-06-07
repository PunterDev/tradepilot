package com.tradepilot.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(
        name = "ai_trade_reviews",
        indexes = {
                @Index(name = "idx_ai_trade_reviews_user_id", columnList = "user_id"),
                @Index(name = "idx_ai_trade_reviews_trade_id", columnList = "trade_id"),
                @Index(name = "idx_ai_trade_reviews_cache", columnList = "user_id, trade_id, review_version, review_hash")
        },
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_ai_trade_reviews_cache",
                        columnNames = {"user_id", "trade_id", "review_version", "review_hash"}
                )
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiTradeReview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    private Long tradeId;

    private Long accountId;

    private String reviewDate;

    private String symbol;

    private Double pnl;

    private String grade;

    private String reviewVersion;

    private String reviewHash;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(columnDefinition = "TEXT")
    private String mistakesJson;

    @Column(columnDefinition = "TEXT")
    private String strengthsJson;

    @Column(columnDefinition = "TEXT")
    private String improvementPlanJson;

    private Instant createdAt;

    private String status; // PENDING, COMPLETED, FAILED

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    @Column(columnDefinition = "TEXT")
    private String tagsJson;

}
