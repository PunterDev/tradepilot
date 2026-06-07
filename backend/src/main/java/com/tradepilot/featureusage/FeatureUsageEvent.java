package com.tradepilot.featureusage;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "feature_usage_events",
        indexes = {
                @Index(name = "idx_feature_usage_user_id", columnList = "user_id"),
                @Index(name = "idx_feature_usage_created_at", columnList = "created_at"),
                @Index(name = "idx_feature_usage_feature_created", columnList = "feature, created_at"),
                @Index(name = "idx_feature_usage_user_feature_created", columnList = "user_id, feature, created_at")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeatureUsageEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private FeatureKey feature;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
