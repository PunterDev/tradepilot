package com.tradepilot.observability;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "stripe_events", indexes = {
        @Index(name = "idx_stripe_events_created_at", columnList = "created_at"),
        @Index(name = "idx_stripe_events_event_type_created_at", columnList = "event_type, created_at"),
        @Index(name = "idx_stripe_events_success_created_at", columnList = "success, created_at")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StripeEventMetric {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_type", nullable = false, length = 120)
    private String eventType;

    @Column(nullable = false)
    private Boolean success;

    @Column(name = "duration_ms", nullable = false)
    private Long durationMs;

    @Column(name = "error_message", length = 700)
    private String errorMessage;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
