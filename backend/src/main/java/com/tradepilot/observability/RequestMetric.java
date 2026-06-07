package com.tradepilot.observability;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "api_request_metrics", indexes = {
        @Index(name = "idx_api_request_metrics_created_at", columnList = "created_at"),
        @Index(name = "idx_api_request_metrics_created_duration", columnList = "created_at, duration_ms"),
        @Index(name = "idx_api_request_metrics_endpoint_created_at", columnList = "endpoint, created_at"),
        @Index(name = "idx_api_request_metrics_status_created_at", columnList = "status, created_at"),
        @Index(name = "idx_api_request_metrics_user_id_created_at", columnList = "user_id, created_at")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequestMetric {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 220)
    private String endpoint;

    @Column(nullable = false, length = 16)
    private String method;

    @Column(nullable = false)
    private Integer status;

    @Column(name = "duration_ms", nullable = false)
    private Long durationMs;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
