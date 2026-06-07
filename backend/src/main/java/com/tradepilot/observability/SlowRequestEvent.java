package com.tradepilot.observability;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "slow_request_events", indexes = {
        @Index(name = "idx_slow_request_events_created_at", columnList = "created_at"),
        @Index(name = "idx_slow_request_events_endpoint_created_at", columnList = "endpoint, created_at")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SlowRequestEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 220)
    private String endpoint;

    @Column(name = "duration_ms", nullable = false)
    private Long durationMs;

    @Column(nullable = false)
    private Integer status;

    @Column(name = "user_id")
    private Long userId;

    @Column(nullable = false, length = 16)
    private String severity;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
