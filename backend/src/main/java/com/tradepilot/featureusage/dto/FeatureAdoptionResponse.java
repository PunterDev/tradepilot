package com.tradepilot.featureusage.dto;

import com.tradepilot.featureusage.FeatureKey;

public class FeatureAdoptionResponse {
    private FeatureKey feature;
    private long uniqueUsers;
    private long totalUsage;
    private double overallAdoptionRate;
    private double activeUserAdoptionRate;

    @java.lang.SuppressWarnings("all")
    FeatureAdoptionResponse(final FeatureKey feature, final long uniqueUsers, final long totalUsage, final double overallAdoptionRate, final double activeUserAdoptionRate) {
        this.feature = feature;
        this.uniqueUsers = uniqueUsers;
        this.totalUsage = totalUsage;
        this.overallAdoptionRate = overallAdoptionRate;
        this.activeUserAdoptionRate = activeUserAdoptionRate;
    }


    @java.lang.SuppressWarnings("all")
    public static class FeatureAdoptionResponseBuilder {
        @java.lang.SuppressWarnings("all")
        private FeatureKey feature;
        @java.lang.SuppressWarnings("all")
        private long uniqueUsers;
        @java.lang.SuppressWarnings("all")
        private long totalUsage;
        @java.lang.SuppressWarnings("all")
        private double overallAdoptionRate;
        @java.lang.SuppressWarnings("all")
        private double activeUserAdoptionRate;

        @java.lang.SuppressWarnings("all")
        FeatureAdoptionResponseBuilder() {
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public FeatureAdoptionResponse.FeatureAdoptionResponseBuilder feature(final FeatureKey feature) {
            this.feature = feature;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public FeatureAdoptionResponse.FeatureAdoptionResponseBuilder uniqueUsers(final long uniqueUsers) {
            this.uniqueUsers = uniqueUsers;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public FeatureAdoptionResponse.FeatureAdoptionResponseBuilder totalUsage(final long totalUsage) {
            this.totalUsage = totalUsage;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public FeatureAdoptionResponse.FeatureAdoptionResponseBuilder overallAdoptionRate(final double overallAdoptionRate) {
            this.overallAdoptionRate = overallAdoptionRate;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public FeatureAdoptionResponse.FeatureAdoptionResponseBuilder activeUserAdoptionRate(final double activeUserAdoptionRate) {
            this.activeUserAdoptionRate = activeUserAdoptionRate;
            return this;
        }

        @java.lang.SuppressWarnings("all")
        public FeatureAdoptionResponse build() {
            return new FeatureAdoptionResponse(this.feature, this.uniqueUsers, this.totalUsage, this.overallAdoptionRate, this.activeUserAdoptionRate);
        }

        @java.lang.Override
        @java.lang.SuppressWarnings("all")
        public java.lang.String toString() {
            return "FeatureAdoptionResponse.FeatureAdoptionResponseBuilder(feature=" + this.feature + ", uniqueUsers=" + this.uniqueUsers + ", totalUsage=" + this.totalUsage + ", overallAdoptionRate=" + this.overallAdoptionRate + ", activeUserAdoptionRate=" + this.activeUserAdoptionRate + ")";
        }
    }

    @java.lang.SuppressWarnings("all")
    public static FeatureAdoptionResponse.FeatureAdoptionResponseBuilder builder() {
        return new FeatureAdoptionResponse.FeatureAdoptionResponseBuilder();
    }

    @java.lang.SuppressWarnings("all")
    public FeatureKey getFeature() {
        return this.feature;
    }

    @java.lang.SuppressWarnings("all")
    public long getUniqueUsers() {
        return this.uniqueUsers;
    }

    @java.lang.SuppressWarnings("all")
    public long getTotalUsage() {
        return this.totalUsage;
    }

    @java.lang.SuppressWarnings("all")
    public double getOverallAdoptionRate() {
        return this.overallAdoptionRate;
    }

    @java.lang.SuppressWarnings("all")
    public double getActiveUserAdoptionRate() {
        return this.activeUserAdoptionRate;
    }

    @java.lang.SuppressWarnings("all")
    public void setFeature(final FeatureKey feature) {
        this.feature = feature;
    }

    @java.lang.SuppressWarnings("all")
    public void setUniqueUsers(final long uniqueUsers) {
        this.uniqueUsers = uniqueUsers;
    }

    @java.lang.SuppressWarnings("all")
    public void setTotalUsage(final long totalUsage) {
        this.totalUsage = totalUsage;
    }

    @java.lang.SuppressWarnings("all")
    public void setOverallAdoptionRate(final double overallAdoptionRate) {
        this.overallAdoptionRate = overallAdoptionRate;
    }

    @java.lang.SuppressWarnings("all")
    public void setActiveUserAdoptionRate(final double activeUserAdoptionRate) {
        this.activeUserAdoptionRate = activeUserAdoptionRate;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof FeatureAdoptionResponse)) return false;
        final FeatureAdoptionResponse other = (FeatureAdoptionResponse) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        if (this.getUniqueUsers() != other.getUniqueUsers()) return false;
        if (this.getTotalUsage() != other.getTotalUsage()) return false;
        if (java.lang.Double.compare(this.getOverallAdoptionRate(), other.getOverallAdoptionRate()) != 0) return false;
        if (java.lang.Double.compare(this.getActiveUserAdoptionRate(), other.getActiveUserAdoptionRate()) != 0) return false;
        final java.lang.Object this$feature = this.getFeature();
        final java.lang.Object other$feature = other.getFeature();
        if (this$feature == null ? other$feature != null : !this$feature.equals(other$feature)) return false;
        return true;
    }

    @java.lang.SuppressWarnings("all")
    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof FeatureAdoptionResponse;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final long $uniqueUsers = this.getUniqueUsers();
        result = result * PRIME + (int) ($uniqueUsers >>> 32 ^ $uniqueUsers);
        final long $totalUsage = this.getTotalUsage();
        result = result * PRIME + (int) ($totalUsage >>> 32 ^ $totalUsage);
        final long $overallAdoptionRate = java.lang.Double.doubleToLongBits(this.getOverallAdoptionRate());
        result = result * PRIME + (int) ($overallAdoptionRate >>> 32 ^ $overallAdoptionRate);
        final long $activeUserAdoptionRate = java.lang.Double.doubleToLongBits(this.getActiveUserAdoptionRate());
        result = result * PRIME + (int) ($activeUserAdoptionRate >>> 32 ^ $activeUserAdoptionRate);
        final java.lang.Object $feature = this.getFeature();
        result = result * PRIME + ($feature == null ? 43 : $feature.hashCode());
        return result;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public java.lang.String toString() {
        return "FeatureAdoptionResponse(feature=" + this.getFeature() + ", uniqueUsers=" + this.getUniqueUsers() + ", totalUsage=" + this.getTotalUsage() + ", overallAdoptionRate=" + this.getOverallAdoptionRate() + ", activeUserAdoptionRate=" + this.getActiveUserAdoptionRate() + ")";
    }
}
