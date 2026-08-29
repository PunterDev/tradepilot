package com.tradepilot.billing;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "stripe_checkout_attempts", uniqueConstraints = {
        @UniqueConstraint(name = "uk_stripe_checkout_attempts_user", columnNames = "user_id")
}, indexes = {
        @Index(name = "idx_stripe_checkout_attempts_status_expires", columnList = "status, expires_at")
})
public class StripeCheckoutAttempt {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "user_id", nullable = false)
    private Long userId;
    @Column(name = "stripe_checkout_session_id", length = 255)
    private String stripeCheckoutSessionId;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StripeCheckoutAttemptStatus status;
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public static StripeCheckoutAttempt pending(Long userId, LocalDateTime expiresAt) {
        StripeCheckoutAttempt attempt = new StripeCheckoutAttempt();
        attempt.setUserId(userId);
        attempt.setStatus(StripeCheckoutAttemptStatus.PENDING);
        attempt.setExpiresAt(expiresAt);
        return attempt;
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public String getStripeCheckoutSessionId() { return stripeCheckoutSessionId; }
    public StripeCheckoutAttemptStatus getStatus() { return status; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public void setId(Long id) { this.id = id; }
    public void setUserId(Long userId) { this.userId = userId; }
    public void setStripeCheckoutSessionId(String stripeCheckoutSessionId) { this.stripeCheckoutSessionId = stripeCheckoutSessionId; }
    public void setStatus(StripeCheckoutAttemptStatus status) { this.status = status; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
