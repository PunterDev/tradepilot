package com.tradepilot.observability.dto;

import java.math.BigDecimal;

public class AiFeatureCostDto {
    private String feature;
    private long calls;
    private long tokens;
    private BigDecimal estimatedCost;

    @java.lang.SuppressWarnings("all")
    AiFeatureCostDto(final String feature, final long calls, final long tokens, final BigDecimal estimatedCost) {
        this.feature = feature;
        this.calls = calls;
        this.tokens = tokens;
        this.estimatedCost = estimatedCost;
    }


    @java.lang.SuppressWarnings("all")
    public static class AiFeatureCostDtoBuilder {
        @java.lang.SuppressWarnings("all")
        private String feature;
        @java.lang.SuppressWarnings("all")
        private long calls;
        @java.lang.SuppressWarnings("all")
        private long tokens;
        @java.lang.SuppressWarnings("all")
        private BigDecimal estimatedCost;

        @java.lang.SuppressWarnings("all")
        AiFeatureCostDtoBuilder() {
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public AiFeatureCostDto.AiFeatureCostDtoBuilder feature(final String feature) {
            this.feature = feature;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public AiFeatureCostDto.AiFeatureCostDtoBuilder calls(final long calls) {
            this.calls = calls;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public AiFeatureCostDto.AiFeatureCostDtoBuilder tokens(final long tokens) {
            this.tokens = tokens;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public AiFeatureCostDto.AiFeatureCostDtoBuilder estimatedCost(final BigDecimal estimatedCost) {
            this.estimatedCost = estimatedCost;
            return this;
        }

        @java.lang.SuppressWarnings("all")
        public AiFeatureCostDto build() {
            return new AiFeatureCostDto(this.feature, this.calls, this.tokens, this.estimatedCost);
        }

        @java.lang.Override
        @java.lang.SuppressWarnings("all")
        public java.lang.String toString() {
            return "AiFeatureCostDto.AiFeatureCostDtoBuilder(feature=" + this.feature + ", calls=" + this.calls + ", tokens=" + this.tokens + ", estimatedCost=" + this.estimatedCost + ")";
        }
    }

    @java.lang.SuppressWarnings("all")
    public static AiFeatureCostDto.AiFeatureCostDtoBuilder builder() {
        return new AiFeatureCostDto.AiFeatureCostDtoBuilder();
    }

    @java.lang.SuppressWarnings("all")
    public String getFeature() {
        return this.feature;
    }

    @java.lang.SuppressWarnings("all")
    public long getCalls() {
        return this.calls;
    }

    @java.lang.SuppressWarnings("all")
    public long getTokens() {
        return this.tokens;
    }

    @java.lang.SuppressWarnings("all")
    public BigDecimal getEstimatedCost() {
        return this.estimatedCost;
    }

    @java.lang.SuppressWarnings("all")
    public void setFeature(final String feature) {
        this.feature = feature;
    }

    @java.lang.SuppressWarnings("all")
    public void setCalls(final long calls) {
        this.calls = calls;
    }

    @java.lang.SuppressWarnings("all")
    public void setTokens(final long tokens) {
        this.tokens = tokens;
    }

    @java.lang.SuppressWarnings("all")
    public void setEstimatedCost(final BigDecimal estimatedCost) {
        this.estimatedCost = estimatedCost;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof AiFeatureCostDto)) return false;
        final AiFeatureCostDto other = (AiFeatureCostDto) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        if (this.getCalls() != other.getCalls()) return false;
        if (this.getTokens() != other.getTokens()) return false;
        final java.lang.Object this$feature = this.getFeature();
        final java.lang.Object other$feature = other.getFeature();
        if (this$feature == null ? other$feature != null : !this$feature.equals(other$feature)) return false;
        final java.lang.Object this$estimatedCost = this.getEstimatedCost();
        final java.lang.Object other$estimatedCost = other.getEstimatedCost();
        if (this$estimatedCost == null ? other$estimatedCost != null : !this$estimatedCost.equals(other$estimatedCost)) return false;
        return true;
    }

    @java.lang.SuppressWarnings("all")
    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof AiFeatureCostDto;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final long $calls = this.getCalls();
        result = result * PRIME + (int) ($calls >>> 32 ^ $calls);
        final long $tokens = this.getTokens();
        result = result * PRIME + (int) ($tokens >>> 32 ^ $tokens);
        final java.lang.Object $feature = this.getFeature();
        result = result * PRIME + ($feature == null ? 43 : $feature.hashCode());
        final java.lang.Object $estimatedCost = this.getEstimatedCost();
        result = result * PRIME + ($estimatedCost == null ? 43 : $estimatedCost.hashCode());
        return result;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public java.lang.String toString() {
        return "AiFeatureCostDto(feature=" + this.getFeature() + ", calls=" + this.getCalls() + ", tokens=" + this.getTokens() + ", estimatedCost=" + this.getEstimatedCost() + ")";
    }
}
