package com.tradepilot.activation;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "user_feature_previews",
        uniqueConstraints = @UniqueConstraint(name = "uk_user_feature_previews_user_feature", columnNames = {"user_id", "feature_key"}),
        indexes = {
                @Index(name = "idx_user_feature_previews_user_id", columnList = "user_id"),
                @Index(name = "idx_user_feature_previews_status", columnList = "status")
        }
)
public class UserFeaturePreview {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "user_id", nullable = false)
    private Long userId;
    @Column(name = "feature_key", nullable = false, length = 80)
    private String featureKey;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserFeaturePreviewStatus status;
    @Column(name = "granted_at", nullable = false)
    private LocalDateTime grantedAt;
    @Column(name = "consumed_at")
    private LocalDateTime consumedAt;
    @Column(name = "reserved_at")
    private LocalDateTime reservedAt;
    @Column(name = "reserved_resource_id")
    private Long reservedResourceId;
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (status == null) status = UserFeaturePreviewStatus.AVAILABLE;
        if (grantedAt == null) grantedAt = now;
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getFeatureKey() {
        return featureKey;
    }

    public void setFeatureKey(String featureKey) {
        this.featureKey = featureKey;
    }

    public UserFeaturePreviewStatus getStatus() {
        return status;
    }

    public void setStatus(UserFeaturePreviewStatus status) {
        this.status = status;
    }

    public LocalDateTime getGrantedAt() {
        return grantedAt;
    }

    public void setGrantedAt(LocalDateTime grantedAt) {
        this.grantedAt = grantedAt;
    }

    public LocalDateTime getConsumedAt() {
        return consumedAt;
    }

    public void setConsumedAt(LocalDateTime consumedAt) {
        this.consumedAt = consumedAt;
    }

    public LocalDateTime getReservedAt() {
        return reservedAt;
    }

    public void setReservedAt(LocalDateTime reservedAt) {
        this.reservedAt = reservedAt;
    }

    public Long getReservedResourceId() {
        return reservedResourceId;
    }

    public void setReservedResourceId(Long reservedResourceId) {
        this.reservedResourceId = reservedResourceId;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
