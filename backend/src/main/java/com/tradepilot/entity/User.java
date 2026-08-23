package com.tradepilot.entity;

import com.tradepilot.billing.PlanType;
import com.tradepilot.billing.SubscriptionStatus;
import com.tradepilot.enums.UserRole;
import jakarta.persistence.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Entity
@Table(name = "users", indexes = {@Index(name = "idx_users_created_at", columnList = "created_at"), @Index(name = "idx_users_plan", columnList = "plan"), @Index(name = "idx_users_role", columnList = "role")})
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
    private Boolean emailVerified;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PlanType plan;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role;
    @Enumerated(EnumType.STRING)
    @Column(name = "subscription_status", nullable = false, length = 20)
    private SubscriptionStatus subscriptionStatus;
    @Column(name = "current_period_start")
    private LocalDateTime currentPeriodStart;
    @Column(name = "current_period_end")
    private LocalDateTime currentPeriodEnd;
    @Column(name = "stripe_customer_id", length = 255)
    private String stripeCustomerId;
    @Column(name = "stripe_subscription_id", length = 255)
    private String stripeSubscriptionId;
    @Column(name = "stripe_cancel_at_period_end", nullable = false)
    private Boolean stripeCancelAtPeriodEnd;
    @Column(name = "stripe_cancel_at")
    private LocalDateTime stripeCancelAt;
    @Column(name = "stripe_canceled_at")
    private LocalDateTime stripeCanceledAt;
    @Column(name = "pro_until")
    private LocalDateTime proUntil;
    @Column(name = "purchased_ai_credits_balance", nullable = false)
    private Integer purchasedAiCreditsBalance;
    @Column(name = "token_version", nullable = false)
    private Integer tokenVersion;
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
        if (this.purchasedAiCreditsBalance == null) {
            this.purchasedAiCreditsBalance = 0;
        }
        if (this.tokenVersion == null) {
            this.tokenVersion = 0;
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

    @java.lang.SuppressWarnings("all")
    private static Boolean $default$emailVerified() {
        return false;
    }

    @java.lang.SuppressWarnings("all")
    private static PlanType $default$plan() {
        return PlanType.FREE;
    }

    @java.lang.SuppressWarnings("all")
    private static UserRole $default$role() {
        return UserRole.USER;
    }

    @java.lang.SuppressWarnings("all")
    private static SubscriptionStatus $default$subscriptionStatus() {
        return SubscriptionStatus.NONE;
    }

    @java.lang.SuppressWarnings("all")
    private static Boolean $default$stripeCancelAtPeriodEnd() {
        return false;
    }

    @java.lang.SuppressWarnings("all")
    private static Integer $default$purchasedAiCreditsBalance() {
        return 0;
    }

    @java.lang.SuppressWarnings("all")
    private static Integer $default$tokenVersion() {
        return 0;
    }


    @java.lang.SuppressWarnings("all")
    public static class UserBuilder {
        @java.lang.SuppressWarnings("all")
        private Long id;
        @java.lang.SuppressWarnings("all")
        private String name;
        @java.lang.SuppressWarnings("all")
        private String email;
        @java.lang.SuppressWarnings("all")
        private String passwordHash;
        @java.lang.SuppressWarnings("all")
        private boolean emailVerified$set;
        @java.lang.SuppressWarnings("all")
        private Boolean emailVerified$value;
        @java.lang.SuppressWarnings("all")
        private boolean plan$set;
        @java.lang.SuppressWarnings("all")
        private PlanType plan$value;
        @java.lang.SuppressWarnings("all")
        private boolean role$set;
        @java.lang.SuppressWarnings("all")
        private UserRole role$value;
        @java.lang.SuppressWarnings("all")
        private boolean subscriptionStatus$set;
        @java.lang.SuppressWarnings("all")
        private SubscriptionStatus subscriptionStatus$value;
        @java.lang.SuppressWarnings("all")
        private LocalDateTime currentPeriodStart;
        @java.lang.SuppressWarnings("all")
        private LocalDateTime currentPeriodEnd;
        @java.lang.SuppressWarnings("all")
        private String stripeCustomerId;
        @java.lang.SuppressWarnings("all")
        private String stripeSubscriptionId;
        @java.lang.SuppressWarnings("all")
        private boolean stripeCancelAtPeriodEnd$set;
        @java.lang.SuppressWarnings("all")
        private Boolean stripeCancelAtPeriodEnd$value;
        @java.lang.SuppressWarnings("all")
        private LocalDateTime stripeCancelAt;
        @java.lang.SuppressWarnings("all")
        private LocalDateTime stripeCanceledAt;
        @java.lang.SuppressWarnings("all")
        private LocalDateTime proUntil;
        @java.lang.SuppressWarnings("all")
        private boolean purchasedAiCreditsBalance$set;
        @java.lang.SuppressWarnings("all")
        private Integer purchasedAiCreditsBalance$value;
        @java.lang.SuppressWarnings("all")
        private boolean tokenVersion$set;
        @java.lang.SuppressWarnings("all")
        private Integer tokenVersion$value;
        @java.lang.SuppressWarnings("all")
        private LocalDateTime createdAt;
        @java.lang.SuppressWarnings("all")
        private LocalDateTime updatedAt;

        @java.lang.SuppressWarnings("all")
        UserBuilder() {
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public User.UserBuilder id(final Long id) {
            this.id = id;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public User.UserBuilder name(final String name) {
            this.name = name;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public User.UserBuilder email(final String email) {
            this.email = email;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public User.UserBuilder passwordHash(final String passwordHash) {
            this.passwordHash = passwordHash;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public User.UserBuilder emailVerified(final Boolean emailVerified) {
            this.emailVerified$value = emailVerified;
            emailVerified$set = true;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public User.UserBuilder plan(final PlanType plan) {
            this.plan$value = plan;
            plan$set = true;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public User.UserBuilder role(final UserRole role) {
            this.role$value = role;
            role$set = true;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public User.UserBuilder subscriptionStatus(final SubscriptionStatus subscriptionStatus) {
            this.subscriptionStatus$value = subscriptionStatus;
            subscriptionStatus$set = true;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public User.UserBuilder currentPeriodStart(final LocalDateTime currentPeriodStart) {
            this.currentPeriodStart = currentPeriodStart;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public User.UserBuilder currentPeriodEnd(final LocalDateTime currentPeriodEnd) {
            this.currentPeriodEnd = currentPeriodEnd;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public User.UserBuilder stripeCustomerId(final String stripeCustomerId) {
            this.stripeCustomerId = stripeCustomerId;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public User.UserBuilder stripeSubscriptionId(final String stripeSubscriptionId) {
            this.stripeSubscriptionId = stripeSubscriptionId;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public User.UserBuilder stripeCancelAtPeriodEnd(final Boolean stripeCancelAtPeriodEnd) {
            this.stripeCancelAtPeriodEnd$value = stripeCancelAtPeriodEnd;
            stripeCancelAtPeriodEnd$set = true;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public User.UserBuilder stripeCancelAt(final LocalDateTime stripeCancelAt) {
            this.stripeCancelAt = stripeCancelAt;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public User.UserBuilder stripeCanceledAt(final LocalDateTime stripeCanceledAt) {
            this.stripeCanceledAt = stripeCanceledAt;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public User.UserBuilder proUntil(final LocalDateTime proUntil) {
            this.proUntil = proUntil;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public User.UserBuilder purchasedAiCreditsBalance(final Integer purchasedAiCreditsBalance) {
            this.purchasedAiCreditsBalance$value = purchasedAiCreditsBalance;
            purchasedAiCreditsBalance$set = true;
            return this;
        }

        @java.lang.SuppressWarnings("all")
        public User.UserBuilder tokenVersion(final Integer tokenVersion) {
            this.tokenVersion$value = tokenVersion;
            tokenVersion$set = true;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public User.UserBuilder createdAt(final LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public User.UserBuilder updatedAt(final LocalDateTime updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        @java.lang.SuppressWarnings("all")
        public User build() {
            Boolean emailVerified$value = this.emailVerified$value;
            if (!this.emailVerified$set) emailVerified$value = User.$default$emailVerified();
            PlanType plan$value = this.plan$value;
            if (!this.plan$set) plan$value = User.$default$plan();
            UserRole role$value = this.role$value;
            if (!this.role$set) role$value = User.$default$role();
            SubscriptionStatus subscriptionStatus$value = this.subscriptionStatus$value;
            if (!this.subscriptionStatus$set) subscriptionStatus$value = User.$default$subscriptionStatus();
            Boolean stripeCancelAtPeriodEnd$value = this.stripeCancelAtPeriodEnd$value;
            if (!this.stripeCancelAtPeriodEnd$set) stripeCancelAtPeriodEnd$value = User.$default$stripeCancelAtPeriodEnd();
            Integer purchasedAiCreditsBalance$value = this.purchasedAiCreditsBalance$value;
            if (!this.purchasedAiCreditsBalance$set) purchasedAiCreditsBalance$value = User.$default$purchasedAiCreditsBalance();
            Integer tokenVersion$value = this.tokenVersion$value;
            if (!this.tokenVersion$set) tokenVersion$value = User.$default$tokenVersion();
            return new User(this.id, this.name, this.email, this.passwordHash, emailVerified$value, plan$value, role$value, subscriptionStatus$value, this.currentPeriodStart, this.currentPeriodEnd, this.stripeCustomerId, this.stripeSubscriptionId, stripeCancelAtPeriodEnd$value, this.stripeCancelAt, this.stripeCanceledAt, this.proUntil, purchasedAiCreditsBalance$value, tokenVersion$value, this.createdAt, this.updatedAt);
        }

        @java.lang.Override
        @java.lang.SuppressWarnings("all")
        public java.lang.String toString() {
            return "User.UserBuilder(id=" + this.id + ", name=" + this.name + ", email=" + this.email + ", passwordHash=" + this.passwordHash + ", emailVerified$value=" + this.emailVerified$value + ", plan$value=" + this.plan$value + ", role$value=" + this.role$value + ", subscriptionStatus$value=" + this.subscriptionStatus$value + ", currentPeriodStart=" + this.currentPeriodStart + ", currentPeriodEnd=" + this.currentPeriodEnd + ", stripeCustomerId=" + this.stripeCustomerId + ", stripeSubscriptionId=" + this.stripeSubscriptionId + ", stripeCancelAtPeriodEnd$value=" + this.stripeCancelAtPeriodEnd$value + ", stripeCancelAt=" + this.stripeCancelAt + ", stripeCanceledAt=" + this.stripeCanceledAt + ", proUntil=" + this.proUntil + ", purchasedAiCreditsBalance$value=" + this.purchasedAiCreditsBalance$value + ", tokenVersion$value=" + this.tokenVersion$value + ", createdAt=" + this.createdAt + ", updatedAt=" + this.updatedAt + ")";
        }
    }

    @java.lang.SuppressWarnings("all")
    public static User.UserBuilder builder() {
        return new User.UserBuilder();
    }

    @java.lang.SuppressWarnings("all")
    public Long getId() {
        return this.id;
    }

    @java.lang.SuppressWarnings("all")
    public String getName() {
        return this.name;
    }

    @java.lang.SuppressWarnings("all")
    public String getEmail() {
        return this.email;
    }

    @java.lang.SuppressWarnings("all")
    public String getPasswordHash() {
        return this.passwordHash;
    }

    @java.lang.SuppressWarnings("all")
    public Boolean getEmailVerified() {
        return this.emailVerified;
    }

    @java.lang.SuppressWarnings("all")
    public PlanType getPlan() {
        return this.plan;
    }

    @java.lang.SuppressWarnings("all")
    public UserRole getRole() {
        return this.role;
    }

    @java.lang.SuppressWarnings("all")
    public SubscriptionStatus getSubscriptionStatus() {
        return this.subscriptionStatus;
    }

    @java.lang.SuppressWarnings("all")
    public LocalDateTime getCurrentPeriodStart() {
        return this.currentPeriodStart;
    }

    @java.lang.SuppressWarnings("all")
    public LocalDateTime getCurrentPeriodEnd() {
        return this.currentPeriodEnd;
    }

    @java.lang.SuppressWarnings("all")
    public String getStripeCustomerId() {
        return this.stripeCustomerId;
    }

    @java.lang.SuppressWarnings("all")
    public String getStripeSubscriptionId() {
        return this.stripeSubscriptionId;
    }

    @java.lang.SuppressWarnings("all")
    public Boolean getStripeCancelAtPeriodEnd() {
        return this.stripeCancelAtPeriodEnd;
    }

    @java.lang.SuppressWarnings("all")
    public LocalDateTime getStripeCancelAt() {
        return this.stripeCancelAt;
    }

    @java.lang.SuppressWarnings("all")
    public LocalDateTime getStripeCanceledAt() {
        return this.stripeCanceledAt;
    }

    @java.lang.SuppressWarnings("all")
    public LocalDateTime getProUntil() {
        return this.proUntil;
    }

    @java.lang.SuppressWarnings("all")
    public Integer getPurchasedAiCreditsBalance() {
        return this.purchasedAiCreditsBalance;
    }

    @java.lang.SuppressWarnings("all")
    public Integer getTokenVersion() {
        return this.tokenVersion;
    }

    @java.lang.SuppressWarnings("all")
    public LocalDateTime getCreatedAt() {
        return this.createdAt;
    }

    @java.lang.SuppressWarnings("all")
    public LocalDateTime getUpdatedAt() {
        return this.updatedAt;
    }

    @java.lang.SuppressWarnings("all")
    public void setId(final Long id) {
        this.id = id;
    }

    @java.lang.SuppressWarnings("all")
    public void setName(final String name) {
        this.name = name;
    }

    @java.lang.SuppressWarnings("all")
    public void setEmail(final String email) {
        this.email = email;
    }

    @java.lang.SuppressWarnings("all")
    public void setPasswordHash(final String passwordHash) {
        this.passwordHash = passwordHash;
    }

    @java.lang.SuppressWarnings("all")
    public void setEmailVerified(final Boolean emailVerified) {
        this.emailVerified = emailVerified;
    }

    @java.lang.SuppressWarnings("all")
    public void setPlan(final PlanType plan) {
        this.plan = plan;
    }

    @java.lang.SuppressWarnings("all")
    public void setRole(final UserRole role) {
        this.role = role;
    }

    @java.lang.SuppressWarnings("all")
    public void setSubscriptionStatus(final SubscriptionStatus subscriptionStatus) {
        this.subscriptionStatus = subscriptionStatus;
    }

    @java.lang.SuppressWarnings("all")
    public void setCurrentPeriodStart(final LocalDateTime currentPeriodStart) {
        this.currentPeriodStart = currentPeriodStart;
    }

    @java.lang.SuppressWarnings("all")
    public void setCurrentPeriodEnd(final LocalDateTime currentPeriodEnd) {
        this.currentPeriodEnd = currentPeriodEnd;
    }

    @java.lang.SuppressWarnings("all")
    public void setStripeCustomerId(final String stripeCustomerId) {
        this.stripeCustomerId = stripeCustomerId;
    }

    @java.lang.SuppressWarnings("all")
    public void setStripeSubscriptionId(final String stripeSubscriptionId) {
        this.stripeSubscriptionId = stripeSubscriptionId;
    }

    @java.lang.SuppressWarnings("all")
    public void setStripeCancelAtPeriodEnd(final Boolean stripeCancelAtPeriodEnd) {
        this.stripeCancelAtPeriodEnd = stripeCancelAtPeriodEnd;
    }

    @java.lang.SuppressWarnings("all")
    public void setStripeCancelAt(final LocalDateTime stripeCancelAt) {
        this.stripeCancelAt = stripeCancelAt;
    }

    @java.lang.SuppressWarnings("all")
    public void setStripeCanceledAt(final LocalDateTime stripeCanceledAt) {
        this.stripeCanceledAt = stripeCanceledAt;
    }

    @java.lang.SuppressWarnings("all")
    public void setProUntil(final LocalDateTime proUntil) {
        this.proUntil = proUntil;
    }

    @java.lang.SuppressWarnings("all")
    public void setPurchasedAiCreditsBalance(final Integer purchasedAiCreditsBalance) {
        this.purchasedAiCreditsBalance = purchasedAiCreditsBalance;
    }

    @java.lang.SuppressWarnings("all")
    public void setTokenVersion(final Integer tokenVersion) {
        this.tokenVersion = tokenVersion;
    }

    @java.lang.SuppressWarnings("all")
    public void setCreatedAt(final LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @java.lang.SuppressWarnings("all")
    public void setUpdatedAt(final LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @java.lang.SuppressWarnings("all")
    public User() {
        this.emailVerified = User.$default$emailVerified();
        this.plan = User.$default$plan();
        this.role = User.$default$role();
        this.subscriptionStatus = User.$default$subscriptionStatus();
        this.stripeCancelAtPeriodEnd = User.$default$stripeCancelAtPeriodEnd();
        this.purchasedAiCreditsBalance = User.$default$purchasedAiCreditsBalance();
        this.tokenVersion = User.$default$tokenVersion();
    }

    @java.lang.SuppressWarnings("all")
    public User(final Long id, final String name, final String email, final String passwordHash, final Boolean emailVerified, final PlanType plan, final UserRole role, final SubscriptionStatus subscriptionStatus, final LocalDateTime currentPeriodStart, final LocalDateTime currentPeriodEnd, final String stripeCustomerId, final String stripeSubscriptionId, final Boolean stripeCancelAtPeriodEnd, final LocalDateTime stripeCancelAt, final LocalDateTime stripeCanceledAt, final LocalDateTime proUntil, final Integer purchasedAiCreditsBalance, final Integer tokenVersion, final LocalDateTime createdAt, final LocalDateTime updatedAt) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.passwordHash = passwordHash;
        this.emailVerified = emailVerified;
        this.plan = plan;
        this.role = role;
        this.subscriptionStatus = subscriptionStatus;
        this.currentPeriodStart = currentPeriodStart;
        this.currentPeriodEnd = currentPeriodEnd;
        this.stripeCustomerId = stripeCustomerId;
        this.stripeSubscriptionId = stripeSubscriptionId;
        this.stripeCancelAtPeriodEnd = stripeCancelAtPeriodEnd;
        this.stripeCancelAt = stripeCancelAt;
        this.stripeCanceledAt = stripeCanceledAt;
        this.proUntil = proUntil;
        this.purchasedAiCreditsBalance = purchasedAiCreditsBalance;
        this.tokenVersion = tokenVersion;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}
