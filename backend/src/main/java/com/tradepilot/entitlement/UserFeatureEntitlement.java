package com.tradepilot.entitlement;

import com.tradepilot.entity.User;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_feature_entitlements",
        uniqueConstraints = @UniqueConstraint(name = "uk_user_feature_entitlements_user_key", columnNames = {"user_id", "entitlement_key"}),
        indexes = {
                @Index(name = "idx_user_feature_entitlements_user", columnList = "user_id"),
                @Index(name = "idx_user_feature_entitlements_key", columnList = "entitlement_key")
        })
public class UserFeatureEntitlement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    @Enumerated(EnumType.STRING)
    @Column(name = "entitlement_key", nullable = false, length = 80)
    private FeatureEntitlementKey entitlementKey;
    @Column(nullable = false)
    private boolean enabled = true;
    @Column(length = 280)
    private String reason;
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

    public Long getId() { return id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public FeatureEntitlementKey getEntitlementKey() { return entitlementKey; }
    public void setEntitlementKey(FeatureEntitlementKey entitlementKey) { this.entitlementKey = entitlementKey; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
