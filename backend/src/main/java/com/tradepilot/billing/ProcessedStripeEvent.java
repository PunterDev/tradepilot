package com.tradepilot.billing;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "processed_stripe_events", indexes = {@Index(name = "idx_processed_stripe_events_processed_at", columnList = "processed_at"), @Index(name = "idx_processed_stripe_events_event_type_processed_at", columnList = "event_type, processed_at")})
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

    @java.lang.SuppressWarnings("all")
    public Long getId() {
        return this.id;
    }

    @java.lang.SuppressWarnings("all")
    public String getEventId() {
        return this.eventId;
    }

    @java.lang.SuppressWarnings("all")
    public String getEventType() {
        return this.eventType;
    }

    @java.lang.SuppressWarnings("all")
    public LocalDateTime getProcessedAt() {
        return this.processedAt;
    }

    @java.lang.SuppressWarnings("all")
    public void setId(final Long id) {
        this.id = id;
    }

    @java.lang.SuppressWarnings("all")
    public void setEventId(final String eventId) {
        this.eventId = eventId;
    }

    @java.lang.SuppressWarnings("all")
    public void setEventType(final String eventType) {
        this.eventType = eventType;
    }

    @java.lang.SuppressWarnings("all")
    public void setProcessedAt(final LocalDateTime processedAt) {
        this.processedAt = processedAt;
    }

    @java.lang.SuppressWarnings("all")
    public ProcessedStripeEvent() {
    }

    @java.lang.SuppressWarnings("all")
    public ProcessedStripeEvent(final Long id, final String eventId, final String eventType, final LocalDateTime processedAt) {
        this.id = id;
        this.eventId = eventId;
        this.eventType = eventType;
        this.processedAt = processedAt;
    }
}
