package com.tradepilot.observability;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "cache_metrics", indexes = {
        @Index(name = "idx_cache_metrics_cache_name_created_at", columnList = "cache_name, created_at"),
        @Index(name = "idx_cache_metrics_created_at", columnList = "created_at")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CacheMetric {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cache_name", nullable = false, length = 120)
    private String cacheName;

    @Column(nullable = false)
    private Boolean hit;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
