package com.tradepilot.entity;

import com.tradepilot.billing.PlanType;
import com.tradepilot.billing.SubscriptionStatus;
import com.tradepilot.enums.UserRole;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Entity
@Table(
        name = "users",
        indexes = {
                @Index(name = "idx_users_created_at", columnList = "created_at"),
                @Index(name = "idx_users_plan", columnList = "plan"),
                @Index(name = "idx_users_role", columnList = "role")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User implements UserDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 100)
    private String name;
    @Column(nullable = false, unique = true, length = 150)
    private String email;
    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;
    @Column(name = "email_verified", nullable = false)
    @Builder.Default
    private Boolean emailVerified = false;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private PlanType plan = PlanType.FREE;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private UserRole role = UserRole.USER;
    @Enumerated(EnumType.STRING)
    @Column(name = "subscription_status", nullable = false, length = 20)
    @Builder.Default
    private SubscriptionStatus subscriptionStatus = SubscriptionStatus.NONE;
    @Column(name = "current_period_start")
    private LocalDateTime currentPeriodStart;
    @Column(name = "current_period_end")
    private LocalDateTime currentPeriodEnd;
    @Column(name = "stripe_customer_id", length = 255)
    private String stripeCustomerId;
    @Column(name = "stripe_subscription_id", length = 255)
    private String stripeSubscriptionId;
    @Column(name = "stripe_cancel_at_period_end", nullable = false)
    @Builder.Default
    private Boolean stripeCancelAtPeriodEnd = false;
    @Column(name = "stripe_cancel_at")
    private LocalDateTime stripeCancelAt;
    @Column(name = "stripe_canceled_at")
    private LocalDateTime stripeCanceledAt;
    @Column(name = "pro_until")
    private LocalDateTime proUntil;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.plan == null) {
            this.plan = PlanType.FREE;
        }
        if (this.subscriptionStatus == null) {
            this.subscriptionStatus = SubscriptionStatus.NONE;
        }
        if (this.role == null) {
            this.role = UserRole.USER;
        }
        if (this.stripeCancelAtPeriodEnd == null) {
            this.stripeCancelAtPeriodEnd = false;
        }
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        UserRole resolvedRole = role == null ? UserRole.USER : role;
        return List.of(new SimpleGrantedAuthority("ROLE_" + resolvedRole.name()));
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
