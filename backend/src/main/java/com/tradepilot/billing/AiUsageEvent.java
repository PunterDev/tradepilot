package com.tradepilot.billing;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "ai_usage_events",
        indexes = {
                @Index(name = "idx_ai_usage_events_user_id", columnList = "user_id"),
                @Index(name = "idx_ai_usage_events_user_type_created", columnList = "user_id, usage_type, created_at")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiUsageEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "usage_type", nullable = false, length = 40)
    private UsageType usageType;

    @Column(name = "related_resource_id")
    private Long relatedResourceId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
