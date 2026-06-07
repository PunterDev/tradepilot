package com.tradepilot.observability;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "email_delivery_events", indexes = {
        @Index(name = "idx_email_delivery_events_created_at", columnList = "created_at"),
        @Index(name = "idx_email_delivery_events_success_created_at", columnList = "success, created_at")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailDeliveryEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 240)
    private String recipient;

    @Column(nullable = false, length = 120)
    private String template;

    @Column(nullable = false)
    private Boolean success;

    @Column(name = "duration_ms", nullable = false)
    private Long durationMs;

    @Column(name = "error_message", length = 700)
    private String errorMessage;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
