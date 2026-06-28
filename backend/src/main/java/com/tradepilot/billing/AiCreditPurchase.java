package com.tradepilot.billing;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "ai_credit_purchases", uniqueConstraints = {@UniqueConstraint(name = "uk_ai_credit_purchases_checkout_session", columnNames = "stripe_checkout_session_id")}, indexes = {@Index(name = "idx_ai_credit_purchases_user_id", columnList = "user_id"), @Index(name = "idx_ai_credit_purchases_created_at", columnList = "created_at")})
public class AiCreditPurchase {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "user_id", nullable = false)
    private Long userId;
    @Column(name = "stripe_checkout_session_id", nullable = false, length = 255)
    private String stripeCheckoutSessionId;
    @Column(name = "stripe_payment_intent_id", length = 255)
    private String stripePaymentIntentId;
    @Column(name = "credits_purchased", nullable = false)
    private Integer creditsPurchased;
    @Column(name = "amount_cents", nullable = false)
    private Integer amountCents;
    @Column(nullable = false, length = 10)
    private String currency;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AiCreditPurchaseStatus status;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }


    @java.lang.SuppressWarnings("all")
    public static class AiCreditPurchaseBuilder {
        @java.lang.SuppressWarnings("all")
        private Long id;
        @java.lang.SuppressWarnings("all")
        private Long userId;
        @java.lang.SuppressWarnings("all")
        private String stripeCheckoutSessionId;
        @java.lang.SuppressWarnings("all")
        private String stripePaymentIntentId;
        @java.lang.SuppressWarnings("all")
        private Integer creditsPurchased;
        @java.lang.SuppressWarnings("all")
        private Integer amountCents;
        @java.lang.SuppressWarnings("all")
        private String currency;
        @java.lang.SuppressWarnings("all")
        private AiCreditPurchaseStatus status;
        @java.lang.SuppressWarnings("all")
        private LocalDateTime createdAt;
        @java.lang.SuppressWarnings("all")
        private LocalDateTime completedAt;

        @java.lang.SuppressWarnings("all")
        AiCreditPurchaseBuilder() {
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public AiCreditPurchase.AiCreditPurchaseBuilder id(final Long id) {
            this.id = id;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public AiCreditPurchase.AiCreditPurchaseBuilder userId(final Long userId) {
            this.userId = userId;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public AiCreditPurchase.AiCreditPurchaseBuilder stripeCheckoutSessionId(final String stripeCheckoutSessionId) {
            this.stripeCheckoutSessionId = stripeCheckoutSessionId;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public AiCreditPurchase.AiCreditPurchaseBuilder stripePaymentIntentId(final String stripePaymentIntentId) {
            this.stripePaymentIntentId = stripePaymentIntentId;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public AiCreditPurchase.AiCreditPurchaseBuilder creditsPurchased(final Integer creditsPurchased) {
            this.creditsPurchased = creditsPurchased;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public AiCreditPurchase.AiCreditPurchaseBuilder amountCents(final Integer amountCents) {
            this.amountCents = amountCents;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public AiCreditPurchase.AiCreditPurchaseBuilder currency(final String currency) {
            this.currency = currency;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public AiCreditPurchase.AiCreditPurchaseBuilder status(final AiCreditPurchaseStatus status) {
            this.status = status;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public AiCreditPurchase.AiCreditPurchaseBuilder createdAt(final LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public AiCreditPurchase.AiCreditPurchaseBuilder completedAt(final LocalDateTime completedAt) {
            this.completedAt = completedAt;
            return this;
        }

        @java.lang.SuppressWarnings("all")
        public AiCreditPurchase build() {
            return new AiCreditPurchase(this.id, this.userId, this.stripeCheckoutSessionId, this.stripePaymentIntentId, this.creditsPurchased, this.amountCents, this.currency, this.status, this.createdAt, this.completedAt);
        }

        @java.lang.Override
        @java.lang.SuppressWarnings("all")
        public java.lang.String toString() {
            return "AiCreditPurchase.AiCreditPurchaseBuilder(id=" + this.id + ", userId=" + this.userId + ", stripeCheckoutSessionId=" + this.stripeCheckoutSessionId + ", stripePaymentIntentId=" + this.stripePaymentIntentId + ", creditsPurchased=" + this.creditsPurchased + ", amountCents=" + this.amountCents + ", currency=" + this.currency + ", status=" + this.status + ", createdAt=" + this.createdAt + ", completedAt=" + this.completedAt + ")";
        }
    }

    @java.lang.SuppressWarnings("all")
    public static AiCreditPurchase.AiCreditPurchaseBuilder builder() {
        return new AiCreditPurchase.AiCreditPurchaseBuilder();
    }

    @java.lang.SuppressWarnings("all")
    public Long getId() {
        return this.id;
    }

    @java.lang.SuppressWarnings("all")
    public Long getUserId() {
        return this.userId;
    }

    @java.lang.SuppressWarnings("all")
    public String getStripeCheckoutSessionId() {
        return this.stripeCheckoutSessionId;
    }

    @java.lang.SuppressWarnings("all")
    public String getStripePaymentIntentId() {
        return this.stripePaymentIntentId;
    }

    @java.lang.SuppressWarnings("all")
    public Integer getCreditsPurchased() {
        return this.creditsPurchased;
    }

    @java.lang.SuppressWarnings("all")
    public Integer getAmountCents() {
        return this.amountCents;
    }

    @java.lang.SuppressWarnings("all")
    public String getCurrency() {
        return this.currency;
    }

    @java.lang.SuppressWarnings("all")
    public AiCreditPurchaseStatus getStatus() {
        return this.status;
    }

    @java.lang.SuppressWarnings("all")
    public LocalDateTime getCreatedAt() {
        return this.createdAt;
    }

    @java.lang.SuppressWarnings("all")
    public LocalDateTime getCompletedAt() {
        return this.completedAt;
    }

    @java.lang.SuppressWarnings("all")
    public void setId(final Long id) {
        this.id = id;
    }

    @java.lang.SuppressWarnings("all")
    public void setUserId(final Long userId) {
        this.userId = userId;
    }

    @java.lang.SuppressWarnings("all")
    public void setStripeCheckoutSessionId(final String stripeCheckoutSessionId) {
        this.stripeCheckoutSessionId = stripeCheckoutSessionId;
    }

    @java.lang.SuppressWarnings("all")
    public void setStripePaymentIntentId(final String stripePaymentIntentId) {
        this.stripePaymentIntentId = stripePaymentIntentId;
    }

    @java.lang.SuppressWarnings("all")
    public void setCreditsPurchased(final Integer creditsPurchased) {
        this.creditsPurchased = creditsPurchased;
    }

    @java.lang.SuppressWarnings("all")
    public void setAmountCents(final Integer amountCents) {
        this.amountCents = amountCents;
    }

    @java.lang.SuppressWarnings("all")
    public void setCurrency(final String currency) {
        this.currency = currency;
    }

    @java.lang.SuppressWarnings("all")
    public void setStatus(final AiCreditPurchaseStatus status) {
        this.status = status;
    }

    @java.lang.SuppressWarnings("all")
    public void setCreatedAt(final LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @java.lang.SuppressWarnings("all")
    public void setCompletedAt(final LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    @java.lang.SuppressWarnings("all")
    public AiCreditPurchase() {
    }

    @java.lang.SuppressWarnings("all")
    public AiCreditPurchase(final Long id, final Long userId, final String stripeCheckoutSessionId, final String stripePaymentIntentId, final Integer creditsPurchased, final Integer amountCents, final String currency, final AiCreditPurchaseStatus status, final LocalDateTime createdAt, final LocalDateTime completedAt) {
        this.id = id;
        this.userId = userId;
        this.stripeCheckoutSessionId = stripeCheckoutSessionId;
        this.stripePaymentIntentId = stripePaymentIntentId;
        this.creditsPurchased = creditsPurchased;
        this.amountCents = amountCents;
        this.currency = currency;
        this.status = status;
        this.createdAt = createdAt;
        this.completedAt = completedAt;
    }
}
