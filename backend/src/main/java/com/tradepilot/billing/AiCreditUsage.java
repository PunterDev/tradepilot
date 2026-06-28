package com.tradepilot.billing;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "ai_credit_usage",
        indexes = {
                @Index(name = "idx_ai_credit_usage_user_id", columnList = "user_id"),
                @Index(name = "idx_ai_credit_usage_created_at", columnList = "created_at"),
                @Index(name = "idx_ai_credit_usage_user_created", columnList = "user_id, created_at"),
                @Index(name = "idx_ai_credit_usage_user_feature_created", columnList = "user_id, feature, created_at")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiCreditUsage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private Feature feature;

    @Column(nullable = false)
    private Integer credits;

    @Column(name = "monthly_credits")
    private Integer monthlyCredits;

    @Column(name = "purchased_credits")
    private Integer purchasedCredits;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "request_id")
    private Long requestId;

    @Column(columnDefinition = "TEXT")
    private String metadata;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
