package com.tradepilot.featureusage.dto;

import com.tradepilot.billing.PlanType;
import com.tradepilot.featureusage.FeatureKey;
import java.util.Map;

public class FeatureUsageUserResponse {
    private Long userId;
    private String email;
    private PlanType plan;
    private long trades;
    private Map<FeatureKey, Long> featureUsageCounts;

    @java.lang.SuppressWarnings("all")
    FeatureUsageUserResponse(final Long userId, final String email, final PlanType plan, final long trades, final Map<FeatureKey, Long> featureUsageCounts) {
        this.userId = userId;
        this.email = email;
        this.plan = plan;
        this.trades = trades;
        this.featureUsageCounts = featureUsageCounts;
    }


    @java.lang.SuppressWarnings("all")
    public static class FeatureUsageUserResponseBuilder {
        @java.lang.SuppressWarnings("all")
        private Long userId;
        @java.lang.SuppressWarnings("all")
        private String email;
        @java.lang.SuppressWarnings("all")
        private PlanType plan;
        @java.lang.SuppressWarnings("all")
        private long trades;
        @java.lang.SuppressWarnings("all")
        private Map<FeatureKey, Long> featureUsageCounts;

        @java.lang.SuppressWarnings("all")
        FeatureUsageUserResponseBuilder() {
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public FeatureUsageUserResponse.FeatureUsageUserResponseBuilder userId(final Long userId) {
            this.userId = userId;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public FeatureUsageUserResponse.FeatureUsageUserResponseBuilder email(final String email) {
            this.email = email;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public FeatureUsageUserResponse.FeatureUsageUserResponseBuilder plan(final PlanType plan) {
            this.plan = plan;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public FeatureUsageUserResponse.FeatureUsageUserResponseBuilder trades(final long trades) {
            this.trades = trades;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public FeatureUsageUserResponse.FeatureUsageUserResponseBuilder featureUsageCounts(final Map<FeatureKey, Long> featureUsageCounts) {
            this.featureUsageCounts = featureUsageCounts;
            return this;
        }

        @java.lang.SuppressWarnings("all")
        public FeatureUsageUserResponse build() {
            return new FeatureUsageUserResponse(this.userId, this.email, this.plan, this.trades, this.featureUsageCounts);
        }

        @java.lang.Override
        @java.lang.SuppressWarnings("all")
        public java.lang.String toString() {
            return "FeatureUsageUserResponse.FeatureUsageUserResponseBuilder(userId=" + this.userId + ", email=" + this.email + ", plan=" + this.plan + ", trades=" + this.trades + ", featureUsageCounts=" + this.featureUsageCounts + ")";
        }
    }

    @java.lang.SuppressWarnings("all")
    public static FeatureUsageUserResponse.FeatureUsageUserResponseBuilder builder() {
        return new FeatureUsageUserResponse.FeatureUsageUserResponseBuilder();
    }

    @java.lang.SuppressWarnings("all")
    public Long getUserId() {
        return this.userId;
    }

    @java.lang.SuppressWarnings("all")
    public String getEmail() {
        return this.email;
    }

    @java.lang.SuppressWarnings("all")
    public PlanType getPlan() {
        return this.plan;
    }

    @java.lang.SuppressWarnings("all")
    public long getTrades() {
        return this.trades;
    }

    @java.lang.SuppressWarnings("all")
    public Map<FeatureKey, Long> getFeatureUsageCounts() {
        return this.featureUsageCounts;
    }

    @java.lang.SuppressWarnings("all")
    public void setUserId(final Long userId) {
        this.userId = userId;
    }

    @java.lang.SuppressWarnings("all")
    public void setEmail(final String email) {
        this.email = email;
    }

    @java.lang.SuppressWarnings("all")
    public void setPlan(final PlanType plan) {
        this.plan = plan;
    }

    @java.lang.SuppressWarnings("all")
    public void setTrades(final long trades) {
        this.trades = trades;
    }

    @java.lang.SuppressWarnings("all")
    public void setFeatureUsageCounts(final Map<FeatureKey, Long> featureUsageCounts) {
        this.featureUsageCounts = featureUsageCounts;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof FeatureUsageUserResponse)) return false;
        final FeatureUsageUserResponse other = (FeatureUsageUserResponse) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        if (this.getTrades() != other.getTrades()) return false;
        final java.lang.Object this$userId = this.getUserId();
        final java.lang.Object other$userId = other.getUserId();
        if (this$userId == null ? other$userId != null : !this$userId.equals(other$userId)) return false;
        final java.lang.Object this$email = this.getEmail();
        final java.lang.Object other$email = other.getEmail();
        if (this$email == null ? other$email != null : !this$email.equals(other$email)) return false;
        final java.lang.Object this$plan = this.getPlan();
        final java.lang.Object other$plan = other.getPlan();
        if (this$plan == null ? other$plan != null : !this$plan.equals(other$plan)) return false;
        final java.lang.Object this$featureUsageCounts = this.getFeatureUsageCounts();
        final java.lang.Object other$featureUsageCounts = other.getFeatureUsageCounts();
        if (this$featureUsageCounts == null ? other$featureUsageCounts != null : !this$featureUsageCounts.equals(other$featureUsageCounts)) return false;
        return true;
    }

    @java.lang.SuppressWarnings("all")
    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof FeatureUsageUserResponse;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final long $trades = this.getTrades();
        result = result * PRIME + (int) ($trades >>> 32 ^ $trades);
        final java.lang.Object $userId = this.getUserId();
        result = result * PRIME + ($userId == null ? 43 : $userId.hashCode());
        final java.lang.Object $email = this.getEmail();
        result = result * PRIME + ($email == null ? 43 : $email.hashCode());
        final java.lang.Object $plan = this.getPlan();
        result = result * PRIME + ($plan == null ? 43 : $plan.hashCode());
        final java.lang.Object $featureUsageCounts = this.getFeatureUsageCounts();
        result = result * PRIME + ($featureUsageCounts == null ? 43 : $featureUsageCounts.hashCode());
        return result;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public java.lang.String toString() {
        return "FeatureUsageUserResponse(userId=" + this.getUserId() + ", email=" + this.getEmail() + ", plan=" + this.getPlan() + ", trades=" + this.getTrades() + ", featureUsageCounts=" + this.getFeatureUsageCounts() + ")";
    }
}
