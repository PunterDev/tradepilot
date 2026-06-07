package com.tradepilot.billing;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "processed_stripe_events", indexes = {
        @Index(name = "idx_processed_stripe_events_processed_at", columnList = "processed_at"),
        @Index(name = "idx_processed_stripe_events_event_type_processed_at", columnList = "event_type, processed_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProcessedStripeEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false, unique = true, length = 120)
    private String eventId;

    @Column(name = "event_type", nullable = false, length = 120)
    private String eventType;

    @Column(name = "processed_at", nullable = false)
    private LocalDateTime processedAt;

    public static ProcessedStripeEvent of(String eventId, String eventType) {
        ProcessedStripeEvent event = new ProcessedStripeEvent();
        event.setEventId(eventId);
        event.setEventType(eventType);
        event.setProcessedAt(LocalDateTime.now());
        return event;
    }
}
