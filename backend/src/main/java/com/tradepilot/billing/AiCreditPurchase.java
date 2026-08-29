package com.tradepilot.billing;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "ai_credit_purchases", uniqueConstraints = {@UniqueConstraint(name = "uk_ai_credit_purchases_checkout_session", columnNames = "stripe_checkout_session_id")}, indexes = {@Index(name = "idx_ai_credit_purchases_user_id", columnList = "user_id"), @Index(name = "idx_ai_credit_purchases_created_at", columnList = "created_at"), @Index(name = "idx_ai_credit_purchases_payment_intent", columnList = "stripe_payment_intent_id"), @Index(name = "idx_ai_credit_purchases_charge", columnList = "stripe_charge_id")})
public class AiCreditPurchase {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "user_id", nullable = false)
    private Long userId;
    @Column(name = "stripe_checkout_session_id", nullable = false, length = 255)
    private String stripeCheckoutSessionId;
    @Column(name = "stripe_customer_id", length = 255)
    private String stripeCustomerId;
    @Column(name = "stripe_payment_intent_id", length = 255)
    private String stripePaymentIntentId;
    @Column(name = "stripe_charge_id", length = 255)
    private String stripeChargeId;
    @Column(name = "pack_id", length = 50)
    private String packId;
    @Column(name = "credits_purchased", nullable = false)
    private Integer creditsPurchased;
    @Column(name = "amount_cents", nullable = false)
    private Integer amountCents;
    @Column(name = "refunded_amount_cents", nullable = false)
    private Integer refundedAmountCents;
    @Column(name = "reversed_credits", nullable = false)
    private Integer reversedCredits;
    @Column(name = "refund_reversal_credits", nullable = false)
    private Integer refundReversalCredits;
    @Column(name = "dispute_reversal_credits", nullable = false)
    private Integer disputeReversalCredits;
    @Column(name = "dispute_restored_credits", nullable = false)
    private Integer disputeRestoredCredits;
    @Column(nullable = false, length = 10)
    private String currency;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AiCreditPurchaseStatus status;
    @Column(name = "dispute_status", length = 50)
    private String disputeStatus;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    @Column(name = "completed_at")
    private LocalDateTime completedAt;
    @Column(name = "refunded_at")
    private LocalDateTime refundedAt;
    @Column(name = "reversed_at")
    private LocalDateTime reversedAt;
    @Column(name = "disputed_at")
    private LocalDateTime disputedAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (refundedAmountCents == null) {
            refundedAmountCents = 0;
        }
        if (reversedCredits == null) {
            reversedCredits = 0;
        }
        if (refundReversalCredits == null) {
            refundReversalCredits = 0;
        }
        if (disputeReversalCredits == null) {
            disputeReversalCredits = 0;
        }
        if (disputeRestoredCredits == null) {
            disputeRestoredCredits = 0;
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
        private String stripeCustomerId;
        @java.lang.SuppressWarnings("all")
        private String stripePaymentIntentId;
        @java.lang.SuppressWarnings("all")
        private String stripeChargeId;
        @java.lang.SuppressWarnings("all")
        private String packId;
        @java.lang.SuppressWarnings("all")
        private Integer creditsPurchased;
        @java.lang.SuppressWarnings("all")
        private Integer amountCents;
        @java.lang.SuppressWarnings("all")
        private Integer refundedAmountCents;
        @java.lang.SuppressWarnings("all")
        private Integer reversedCredits;
        @java.lang.SuppressWarnings("all")
        private Integer refundReversalCredits;
        @java.lang.SuppressWarnings("all")
        private Integer disputeReversalCredits;
        @java.lang.SuppressWarnings("all")
        private Integer disputeRestoredCredits;
        @java.lang.SuppressWarnings("all")
        private String currency;
        @java.lang.SuppressWarnings("all")
        private AiCreditPurchaseStatus status;
        @java.lang.SuppressWarnings("all")
        private String disputeStatus;
        @java.lang.SuppressWarnings("all")
        private LocalDateTime createdAt;
        @java.lang.SuppressWarnings("all")
        private LocalDateTime completedAt;
        @java.lang.SuppressWarnings("all")
        private LocalDateTime refundedAt;
        @java.lang.SuppressWarnings("all")
        private LocalDateTime reversedAt;
        @java.lang.SuppressWarnings("all")
        private LocalDateTime disputedAt;

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
        public AiCreditPurchase.AiCreditPurchaseBuilder stripeCustomerId(final String stripeCustomerId) {
            this.stripeCustomerId = stripeCustomerId;
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
        public AiCreditPurchase.AiCreditPurchaseBuilder stripeChargeId(final String stripeChargeId) {
            this.stripeChargeId = stripeChargeId;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public AiCreditPurchase.AiCreditPurchaseBuilder packId(final String packId) {
            this.packId = packId;
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
        public AiCreditPurchase.AiCreditPurchaseBuilder refundedAmountCents(final Integer refundedAmountCents) {
            this.refundedAmountCents = refundedAmountCents;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public AiCreditPurchase.AiCreditPurchaseBuilder reversedCredits(final Integer reversedCredits) {
            this.reversedCredits = reversedCredits;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public AiCreditPurchase.AiCreditPurchaseBuilder refundReversalCredits(final Integer refundReversalCredits) {
            this.refundReversalCredits = refundReversalCredits;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public AiCreditPurchase.AiCreditPurchaseBuilder disputeReversalCredits(final Integer disputeReversalCredits) {
            this.disputeReversalCredits = disputeReversalCredits;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public AiCreditPurchase.AiCreditPurchaseBuilder disputeRestoredCredits(final Integer disputeRestoredCredits) {
            this.disputeRestoredCredits = disputeRestoredCredits;
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
        public AiCreditPurchase.AiCreditPurchaseBuilder disputeStatus(final String disputeStatus) {
            this.disputeStatus = disputeStatus;
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

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public AiCreditPurchase.AiCreditPurchaseBuilder refundedAt(final LocalDateTime refundedAt) {
            this.refundedAt = refundedAt;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public AiCreditPurchase.AiCreditPurchaseBuilder reversedAt(final LocalDateTime reversedAt) {
            this.reversedAt = reversedAt;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public AiCreditPurchase.AiCreditPurchaseBuilder disputedAt(final LocalDateTime disputedAt) {
            this.disputedAt = disputedAt;
            return this;
        }

        @java.lang.SuppressWarnings("all")
        public AiCreditPurchase build() {
            return new AiCreditPurchase(this.id, this.userId, this.stripeCheckoutSessionId, this.stripeCustomerId, this.stripePaymentIntentId, this.stripeChargeId, this.packId, this.creditsPurchased, this.amountCents, this.refundedAmountCents, this.reversedCredits, this.refundReversalCredits, this.disputeReversalCredits, this.disputeRestoredCredits, this.currency, this.status, this.disputeStatus, this.createdAt, this.completedAt, this.refundedAt, this.reversedAt, this.disputedAt);
        }

        @java.lang.Override
        @java.lang.SuppressWarnings("all")
        public java.lang.String toString() {
            return "AiCreditPurchase.AiCreditPurchaseBuilder(id=" + this.id + ", userId=" + this.userId + ", stripeCheckoutSessionId=" + this.stripeCheckoutSessionId + ", stripeCustomerId=" + this.stripeCustomerId + ", stripePaymentIntentId=" + this.stripePaymentIntentId + ", stripeChargeId=" + this.stripeChargeId + ", packId=" + this.packId + ", creditsPurchased=" + this.creditsPurchased + ", amountCents=" + this.amountCents + ", refundedAmountCents=" + this.refundedAmountCents + ", reversedCredits=" + this.reversedCredits + ", refundReversalCredits=" + this.refundReversalCredits + ", disputeReversalCredits=" + this.disputeReversalCredits + ", disputeRestoredCredits=" + this.disputeRestoredCredits + ", currency=" + this.currency + ", status=" + this.status + ", disputeStatus=" + this.disputeStatus + ", createdAt=" + this.createdAt + ", completedAt=" + this.completedAt + ", refundedAt=" + this.refundedAt + ", reversedAt=" + this.reversedAt + ", disputedAt=" + this.disputedAt + ")";
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
    public String getStripeCustomerId() {
        return this.stripeCustomerId;
    }

    @java.lang.SuppressWarnings("all")
    public String getStripePaymentIntentId() {
        return this.stripePaymentIntentId;
    }

    @java.lang.SuppressWarnings("all")
    public String getStripeChargeId() {
        return this.stripeChargeId;
    }

    @java.lang.SuppressWarnings("all")
    public String getPackId() {
        return this.packId;
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
    public Integer getRefundedAmountCents() {
        return this.refundedAmountCents;
    }

    @java.lang.SuppressWarnings("all")
    public Integer getReversedCredits() {
        return this.reversedCredits;
    }

    @java.lang.SuppressWarnings("all")
    public Integer getRefundReversalCredits() {
        return this.refundReversalCredits;
    }

    @java.lang.SuppressWarnings("all")
    public Integer getDisputeReversalCredits() {
        return this.disputeReversalCredits;
    }

    @java.lang.SuppressWarnings("all")
    public Integer getDisputeRestoredCredits() {
        return this.disputeRestoredCredits;
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
    public String getDisputeStatus() {
        return this.disputeStatus;
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
    public LocalDateTime getRefundedAt() {
        return this.refundedAt;
    }

    @java.lang.SuppressWarnings("all")
    public LocalDateTime getReversedAt() {
        return this.reversedAt;
    }

    @java.lang.SuppressWarnings("all")
    public LocalDateTime getDisputedAt() {
        return this.disputedAt;
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
    public void setStripeCustomerId(final String stripeCustomerId) {
        this.stripeCustomerId = stripeCustomerId;
    }

    @java.lang.SuppressWarnings("all")
    public void setStripePaymentIntentId(final String stripePaymentIntentId) {
        this.stripePaymentIntentId = stripePaymentIntentId;
    }

    @java.lang.SuppressWarnings("all")
    public void setStripeChargeId(final String stripeChargeId) {
        this.stripeChargeId = stripeChargeId;
    }

    @java.lang.SuppressWarnings("all")
    public void setPackId(final String packId) {
        this.packId = packId;
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
    public void setRefundedAmountCents(final Integer refundedAmountCents) {
        this.refundedAmountCents = refundedAmountCents;
    }

    @java.lang.SuppressWarnings("all")
    public void setReversedCredits(final Integer reversedCredits) {
        this.reversedCredits = reversedCredits;
    }

    @java.lang.SuppressWarnings("all")
    public void setRefundReversalCredits(final Integer refundReversalCredits) {
        this.refundReversalCredits = refundReversalCredits;
    }

    @java.lang.SuppressWarnings("all")
    public void setDisputeReversalCredits(final Integer disputeReversalCredits) {
        this.disputeReversalCredits = disputeReversalCredits;
    }

    @java.lang.SuppressWarnings("all")
    public void setDisputeRestoredCredits(final Integer disputeRestoredCredits) {
        this.disputeRestoredCredits = disputeRestoredCredits;
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
    public void setDisputeStatus(final String disputeStatus) {
        this.disputeStatus = disputeStatus;
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
    public void setRefundedAt(final LocalDateTime refundedAt) {
        this.refundedAt = refundedAt;
    }

    @java.lang.SuppressWarnings("all")
    public void setReversedAt(final LocalDateTime reversedAt) {
        this.reversedAt = reversedAt;
    }

    @java.lang.SuppressWarnings("all")
    public void setDisputedAt(final LocalDateTime disputedAt) {
        this.disputedAt = disputedAt;
    }

    @java.lang.SuppressWarnings("all")
    public AiCreditPurchase() {
    }

    @java.lang.SuppressWarnings("all")
    public AiCreditPurchase(final Long id, final Long userId, final String stripeCheckoutSessionId, final String stripeCustomerId, final String stripePaymentIntentId, final String stripeChargeId, final String packId, final Integer creditsPurchased, final Integer amountCents, final Integer refundedAmountCents, final Integer reversedCredits, final Integer refundReversalCredits, final Integer disputeReversalCredits, final Integer disputeRestoredCredits, final String currency, final AiCreditPurchaseStatus status, final String disputeStatus, final LocalDateTime createdAt, final LocalDateTime completedAt, final LocalDateTime refundedAt, final LocalDateTime reversedAt, final LocalDateTime disputedAt) {
        this.id = id;
        this.userId = userId;
        this.stripeCheckoutSessionId = stripeCheckoutSessionId;
        this.stripeCustomerId = stripeCustomerId;
        this.stripePaymentIntentId = stripePaymentIntentId;
        this.stripeChargeId = stripeChargeId;
        this.packId = packId;
        this.creditsPurchased = creditsPurchased;
        this.amountCents = amountCents;
        this.refundedAmountCents = refundedAmountCents;
        this.reversedCredits = reversedCredits;
        this.refundReversalCredits = refundReversalCredits;
        this.disputeReversalCredits = disputeReversalCredits;
        this.disputeRestoredCredits = disputeRestoredCredits;
        this.currency = currency;
        this.status = status;
        this.disputeStatus = disputeStatus;
        this.createdAt = createdAt;
        this.completedAt = completedAt;
        this.refundedAt = refundedAt;
        this.reversedAt = reversedAt;
        this.disputedAt = disputedAt;
    }
}
