package com.tradepilot.featureusage.dto;

import com.tradepilot.featureusage.FeatureKey;
import java.util.List;

public class FeatureUsageSummaryResponse {
    private long totalUsers;
    private long activeUsers30d;
    private List<FeatureAdoptionResponse> features;
    private FeatureKey mostUsedFeature;
    private FeatureKey leastUsedFeature;

    @java.lang.SuppressWarnings("all")
    FeatureUsageSummaryResponse(final long totalUsers, final long activeUsers30d, final List<FeatureAdoptionResponse> features, final FeatureKey mostUsedFeature, final FeatureKey leastUsedFeature) {
        this.totalUsers = totalUsers;
        this.activeUsers30d = activeUsers30d;
        this.features = features;
        this.mostUsedFeature = mostUsedFeature;
        this.leastUsedFeature = leastUsedFeature;
    }


    @java.lang.SuppressWarnings("all")
    public static class FeatureUsageSummaryResponseBuilder {
        @java.lang.SuppressWarnings("all")
        private long totalUsers;
        @java.lang.SuppressWarnings("all")
        private long activeUsers30d;
        @java.lang.SuppressWarnings("all")
        private List<FeatureAdoptionResponse> features;
        @java.lang.SuppressWarnings("all")
        private FeatureKey mostUsedFeature;
        @java.lang.SuppressWarnings("all")
        private FeatureKey leastUsedFeature;

        @java.lang.SuppressWarnings("all")
        FeatureUsageSummaryResponseBuilder() {
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public FeatureUsageSummaryResponse.FeatureUsageSummaryResponseBuilder totalUsers(final long totalUsers) {
            this.totalUsers = totalUsers;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public FeatureUsageSummaryResponse.FeatureUsageSummaryResponseBuilder activeUsers30d(final long activeUsers30d) {
            this.activeUsers30d = activeUsers30d;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public FeatureUsageSummaryResponse.FeatureUsageSummaryResponseBuilder features(final List<FeatureAdoptionResponse> features) {
            this.features = features;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public FeatureUsageSummaryResponse.FeatureUsageSummaryResponseBuilder mostUsedFeature(final FeatureKey mostUsedFeature) {
            this.mostUsedFeature = mostUsedFeature;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public FeatureUsageSummaryResponse.FeatureUsageSummaryResponseBuilder leastUsedFeature(final FeatureKey leastUsedFeature) {
            this.leastUsedFeature = leastUsedFeature;
            return this;
        }

        @java.lang.SuppressWarnings("all")
        public FeatureUsageSummaryResponse build() {
            return new FeatureUsageSummaryResponse(this.totalUsers, this.activeUsers30d, this.features, this.mostUsedFeature, this.leastUsedFeature);
        }

        @java.lang.Override
        @java.lang.SuppressWarnings("all")
        public java.lang.String toString() {
            return "FeatureUsageSummaryResponse.FeatureUsageSummaryResponseBuilder(totalUsers=" + this.totalUsers + ", activeUsers30d=" + this.activeUsers30d + ", features=" + this.features + ", mostUsedFeature=" + this.mostUsedFeature + ", leastUsedFeature=" + this.leastUsedFeature + ")";
        }
    }

    @java.lang.SuppressWarnings("all")
    public static FeatureUsageSummaryResponse.FeatureUsageSummaryResponseBuilder builder() {
        return new FeatureUsageSummaryResponse.FeatureUsageSummaryResponseBuilder();
    }

    @java.lang.SuppressWarnings("all")
    public long getTotalUsers() {
        return this.totalUsers;
    }

    @java.lang.SuppressWarnings("all")
    public long getActiveUsers30d() {
        return this.activeUsers30d;
    }

    @java.lang.SuppressWarnings("all")
    public List<FeatureAdoptionResponse> getFeatures() {
        return this.features;
    }

    @java.lang.SuppressWarnings("all")
    public FeatureKey getMostUsedFeature() {
        return this.mostUsedFeature;
    }

    @java.lang.SuppressWarnings("all")
    public FeatureKey getLeastUsedFeature() {
        return this.leastUsedFeature;
    }

    @java.lang.SuppressWarnings("all")
    public void setTotalUsers(final long totalUsers) {
        this.totalUsers = totalUsers;
    }

    @java.lang.SuppressWarnings("all")
    public void setActiveUsers30d(final long activeUsers30d) {
        this.activeUsers30d = activeUsers30d;
    }

    @java.lang.SuppressWarnings("all")
    public void setFeatures(final List<FeatureAdoptionResponse> features) {
        this.features = features;
    }

    @java.lang.SuppressWarnings("all")
    public void setMostUsedFeature(final FeatureKey mostUsedFeature) {
        this.mostUsedFeature = mostUsedFeature;
    }

    @java.lang.SuppressWarnings("all")
    public void setLeastUsedFeature(final FeatureKey leastUsedFeature) {
        this.leastUsedFeature = leastUsedFeature;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof FeatureUsageSummaryResponse)) return false;
        final FeatureUsageSummaryResponse other = (FeatureUsageSummaryResponse) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        if (this.getTotalUsers() != other.getTotalUsers()) return false;
        if (this.getActiveUsers30d() != other.getActiveUsers30d()) return false;
        final java.lang.Object this$features = this.getFeatures();
        final java.lang.Object other$features = other.getFeatures();
        if (this$features == null ? other$features != null : !this$features.equals(other$features)) return false;
        final java.lang.Object this$mostUsedFeature = this.getMostUsedFeature();
        final java.lang.Object other$mostUsedFeature = other.getMostUsedFeature();
        if (this$mostUsedFeature == null ? other$mostUsedFeature != null : !this$mostUsedFeature.equals(other$mostUsedFeature)) return false;
        final java.lang.Object this$leastUsedFeature = this.getLeastUsedFeature();
        final java.lang.Object other$leastUsedFeature = other.getLeastUsedFeature();
        if (this$leastUsedFeature == null ? other$leastUsedFeature != null : !this$leastUsedFeature.equals(other$leastUsedFeature)) return false;
        return true;
    }

    @java.lang.SuppressWarnings("all")
    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof FeatureUsageSummaryResponse;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final long $totalUsers = this.getTotalUsers();
        result = result * PRIME + (int) ($totalUsers >>> 32 ^ $totalUsers);
        final long $activeUsers30d = this.getActiveUsers30d();
        result = result * PRIME + (int) ($activeUsers30d >>> 32 ^ $activeUsers30d);
        final java.lang.Object $features = this.getFeatures();
        result = result * PRIME + ($features == null ? 43 : $features.hashCode());
        final java.lang.Object $mostUsedFeature = this.getMostUsedFeature();
        result = result * PRIME + ($mostUsedFeature == null ? 43 : $mostUsedFeature.hashCode());
        final java.lang.Object $leastUsedFeature = this.getLeastUsedFeature();
        result = result * PRIME + ($leastUsedFeature == null ? 43 : $leastUsedFeature.hashCode());
        return result;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public java.lang.String toString() {
        return "FeatureUsageSummaryResponse(totalUsers=" + this.getTotalUsers() + ", activeUsers30d=" + this.getActiveUsers30d() + ", features=" + this.getFeatures() + ", mostUsedFeature=" + this.getMostUsedFeature() + ", leastUsedFeature=" + this.getLeastUsedFeature() + ")";
    }
}
