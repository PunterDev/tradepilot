package com.tradepilot.billing;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "stripe_subscription_states", indexes = {
        @Index(name = "idx_stripe_subscription_states_last_event_created", columnList = "last_event_created")
})
public class StripeSubscriptionState {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "subscription_id", nullable = false, unique = true, length = 255)
    private String subscriptionId;
    @Column(name = "last_event_id", nullable = false, length = 120)
    private String lastEventId;
    @Column(name = "last_event_type", nullable = false, length = 120)
    private String lastEventType;
    @Column(name = "last_event_created", nullable = false)
    private LocalDateTime lastEventCreated;
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    void touch() {
        updatedAt = LocalDateTime.now();
    }

    public static StripeSubscriptionState create(String subscriptionId, String eventId, String eventType, LocalDateTime eventCreated) {
        StripeSubscriptionState state = new StripeSubscriptionState();
        state.setSubscriptionId(subscriptionId);
        state.setLastEventId(eventId);
        state.setLastEventType(eventType);
        state.setLastEventCreated(eventCreated);
        return state;
    }

    public Long getId() { return id; }
    public String getSubscriptionId() { return subscriptionId; }
    public String getLastEventId() { return lastEventId; }
    public String getLastEventType() { return lastEventType; }
    public LocalDateTime getLastEventCreated() { return lastEventCreated; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public void setId(Long id) { this.id = id; }
    public void setSubscriptionId(String subscriptionId) { this.subscriptionId = subscriptionId; }
    public void setLastEventId(String lastEventId) { this.lastEventId = lastEventId; }
    public void setLastEventType(String lastEventType) { this.lastEventType = lastEventType; }
    public void setLastEventCreated(LocalDateTime lastEventCreated) { this.lastEventCreated = lastEventCreated; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
