package com.tradepilot.observability;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "ai_request_metrics", indexes = {
        @Index(name = "idx_ai_request_metrics_created_at", columnList = "created_at"),
        @Index(name = "idx_ai_request_metrics_user_id_created_at", columnList = "user_id, created_at"),
        @Index(name = "idx_ai_request_metrics_feature_created_at", columnList = "feature, created_at")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiRequestMetric {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(nullable = false, length = 64)
    private String feature;

    @Column(nullable = false, length = 80)
    private String model;

    @Column(name = "prompt_tokens")
    private Integer promptTokens;

    @Column(name = "completion_tokens")
    private Integer completionTokens;

    @Column(name = "total_tokens")
    private Integer totalTokens;

    @Column(name = "estimated_cost", precision = 12, scale = 6)
    private BigDecimal estimatedCost;

    @Column(name = "duration_ms", nullable = false)
    private Long durationMs;

    @Column(nullable = false)
    private Boolean success;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
