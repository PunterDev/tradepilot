package com.tradepilot.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "ai_session_reviews",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_ai_session_reviews_user_account_date", columnNames = {"user_id", "account_id", "session_date"})
        },
        indexes = {
                @Index(name = "idx_ai_session_reviews_user_id", columnList = "user_id"),
                @Index(name = "idx_ai_session_reviews_user_account_date", columnList = "user_id, account_id, session_date"),
                @Index(name = "idx_ai_session_reviews_user_account_date_hash", columnList = "user_id, account_id, session_date, review_hash")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiSessionReview {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "account_id", nullable = false)
    private Long accountId;

    @Column(name = "session_date", nullable = false)
    private LocalDate sessionDate;

    @Column(name = "review_hash", nullable = false, length = 64)
    private String reviewHash;

    @Column(columnDefinition = "text")
    private String summary;

    @Column(name = "what_went_well", columnDefinition = "text")
    private String whatWentWell;

    @Column(name = "what_hurt_performance", columnDefinition = "text")
    private String whatHurtPerformance;

    @Column(name = "biggest_mistake", columnDefinition = "text")
    private String biggestMistake;

    @Column(name = "best_setup", columnDefinition = "text")
    private String bestSetup;

    @Column(name = "worst_decision", columnDefinition = "text")
    private String worstDecision;

    @Column(name = "emotional_pattern", columnDefinition = "text")
    private String emotionalPattern;

    @Column(name = "execution_pattern", columnDefinition = "text")
    private String executionPattern;

    @Column(name = "risk_management_pattern", columnDefinition = "text")
    private String riskManagementPattern;

    @Column(name = "one_rule_for_tomorrow", columnDefinition = "text")
    private String oneRuleForTomorrow;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
