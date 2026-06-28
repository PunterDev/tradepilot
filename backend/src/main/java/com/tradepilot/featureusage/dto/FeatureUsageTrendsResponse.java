package com.tradepilot.featureusage.dto;

import java.util.List;

public class FeatureUsageTrendsResponse {
    private List<FeatureTrendPointResponse> dailyCoach;
    private List<FeatureTrendPointResponse> weeklyCoach;
    private List<FeatureTrendPointResponse> tradingDna;
    private List<FeatureTrendPointResponse> aiReview;
    private List<FeatureTrendPointResponse> sessionReview;

    @java.lang.SuppressWarnings("all")
    FeatureUsageTrendsResponse(final List<FeatureTrendPointResponse> dailyCoach, final List<FeatureTrendPointResponse> weeklyCoach, final List<FeatureTrendPointResponse> tradingDna, final List<FeatureTrendPointResponse> aiReview, final List<FeatureTrendPointResponse> sessionReview) {
        this.dailyCoach = dailyCoach;
        this.weeklyCoach = weeklyCoach;
        this.tradingDna = tradingDna;
        this.aiReview = aiReview;
        this.sessionReview = sessionReview;
    }


    @java.lang.SuppressWarnings("all")
    public static class FeatureUsageTrendsResponseBuilder {
        @java.lang.SuppressWarnings("all")
        private List<FeatureTrendPointResponse> dailyCoach;
        @java.lang.SuppressWarnings("all")
        private List<FeatureTrendPointResponse> weeklyCoach;
        @java.lang.SuppressWarnings("all")
        private List<FeatureTrendPointResponse> tradingDna;
        @java.lang.SuppressWarnings("all")
        private List<FeatureTrendPointResponse> aiReview;
        @java.lang.SuppressWarnings("all")
        private List<FeatureTrendPointResponse> sessionReview;

        @java.lang.SuppressWarnings("all")
        FeatureUsageTrendsResponseBuilder() {
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public FeatureUsageTrendsResponse.FeatureUsageTrendsResponseBuilder dailyCoach(final List<FeatureTrendPointResponse> dailyCoach) {
            this.dailyCoach = dailyCoach;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public FeatureUsageTrendsResponse.FeatureUsageTrendsResponseBuilder weeklyCoach(final List<FeatureTrendPointResponse> weeklyCoach) {
            this.weeklyCoach = weeklyCoach;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public FeatureUsageTrendsResponse.FeatureUsageTrendsResponseBuilder tradingDna(final List<FeatureTrendPointResponse> tradingDna) {
            this.tradingDna = tradingDna;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public FeatureUsageTrendsResponse.FeatureUsageTrendsResponseBuilder aiReview(final List<FeatureTrendPointResponse> aiReview) {
            this.aiReview = aiReview;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public FeatureUsageTrendsResponse.FeatureUsageTrendsResponseBuilder sessionReview(final List<FeatureTrendPointResponse> sessionReview) {
            this.sessionReview = sessionReview;
            return this;
        }

        @java.lang.SuppressWarnings("all")
        public FeatureUsageTrendsResponse build() {
            return new FeatureUsageTrendsResponse(this.dailyCoach, this.weeklyCoach, this.tradingDna, this.aiReview, this.sessionReview);
        }

        @java.lang.Override
        @java.lang.SuppressWarnings("all")
        public java.lang.String toString() {
            return "FeatureUsageTrendsResponse.FeatureUsageTrendsResponseBuilder(dailyCoach=" + this.dailyCoach + ", weeklyCoach=" + this.weeklyCoach + ", tradingDna=" + this.tradingDna + ", aiReview=" + this.aiReview + ", sessionReview=" + this.sessionReview + ")";
        }
    }

    @java.lang.SuppressWarnings("all")
    public static FeatureUsageTrendsResponse.FeatureUsageTrendsResponseBuilder builder() {
        return new FeatureUsageTrendsResponse.FeatureUsageTrendsResponseBuilder();
    }

    @java.lang.SuppressWarnings("all")
    public List<FeatureTrendPointResponse> getDailyCoach() {
        return this.dailyCoach;
    }

    @java.lang.SuppressWarnings("all")
    public List<FeatureTrendPointResponse> getWeeklyCoach() {
        return this.weeklyCoach;
    }

    @java.lang.SuppressWarnings("all")
    public List<FeatureTrendPointResponse> getTradingDna() {
        return this.tradingDna;
    }

    @java.lang.SuppressWarnings("all")
    public List<FeatureTrendPointResponse> getAiReview() {
        return this.aiReview;
    }

    @java.lang.SuppressWarnings("all")
    public List<FeatureTrendPointResponse> getSessionReview() {
        return this.sessionReview;
    }

    @java.lang.SuppressWarnings("all")
    public void setDailyCoach(final List<FeatureTrendPointResponse> dailyCoach) {
        this.dailyCoach = dailyCoach;
    }

    @java.lang.SuppressWarnings("all")
    public void setWeeklyCoach(final List<FeatureTrendPointResponse> weeklyCoach) {
        this.weeklyCoach = weeklyCoach;
    }

    @java.lang.SuppressWarnings("all")
    public void setTradingDna(final List<FeatureTrendPointResponse> tradingDna) {
        this.tradingDna = tradingDna;
    }

    @java.lang.SuppressWarnings("all")
    public void setAiReview(final List<FeatureTrendPointResponse> aiReview) {
        this.aiReview = aiReview;
    }

    @java.lang.SuppressWarnings("all")
    public void setSessionReview(final List<FeatureTrendPointResponse> sessionReview) {
        this.sessionReview = sessionReview;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof FeatureUsageTrendsResponse)) return false;
        final FeatureUsageTrendsResponse other = (FeatureUsageTrendsResponse) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$dailyCoach = this.getDailyCoach();
        final java.lang.Object other$dailyCoach = other.getDailyCoach();
        if (this$dailyCoach == null ? other$dailyCoach != null : !this$dailyCoach.equals(other$dailyCoach)) return false;
        final java.lang.Object this$weeklyCoach = this.getWeeklyCoach();
        final java.lang.Object other$weeklyCoach = other.getWeeklyCoach();
        if (this$weeklyCoach == null ? other$weeklyCoach != null : !this$weeklyCoach.equals(other$weeklyCoach)) return false;
        final java.lang.Object this$tradingDna = this.getTradingDna();
        final java.lang.Object other$tradingDna = other.getTradingDna();
        if (this$tradingDna == null ? other$tradingDna != null : !this$tradingDna.equals(other$tradingDna)) return false;
        final java.lang.Object this$aiReview = this.getAiReview();
        final java.lang.Object other$aiReview = other.getAiReview();
        if (this$aiReview == null ? other$aiReview != null : !this$aiReview.equals(other$aiReview)) return false;
        final java.lang.Object this$sessionReview = this.getSessionReview();
        final java.lang.Object other$sessionReview = other.getSessionReview();
        if (this$sessionReview == null ? other$sessionReview != null : !this$sessionReview.equals(other$sessionReview)) return false;
        return true;
    }

    @java.lang.SuppressWarnings("all")
    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof FeatureUsageTrendsResponse;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $dailyCoach = this.getDailyCoach();
        result = result * PRIME + ($dailyCoach == null ? 43 : $dailyCoach.hashCode());
        final java.lang.Object $weeklyCoach = this.getWeeklyCoach();
        result = result * PRIME + ($weeklyCoach == null ? 43 : $weeklyCoach.hashCode());
        final java.lang.Object $tradingDna = this.getTradingDna();
        result = result * PRIME + ($tradingDna == null ? 43 : $tradingDna.hashCode());
        final java.lang.Object $aiReview = this.getAiReview();
        result = result * PRIME + ($aiReview == null ? 43 : $aiReview.hashCode());
        final java.lang.Object $sessionReview = this.getSessionReview();
        result = result * PRIME + ($sessionReview == null ? 43 : $sessionReview.hashCode());
        return result;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public java.lang.String toString() {
        return "FeatureUsageTrendsResponse(dailyCoach=" + this.getDailyCoach() + ", weeklyCoach=" + this.getWeeklyCoach() + ", tradingDna=" + this.getTradingDna() + ", aiReview=" + this.getAiReview() + ", sessionReview=" + this.getSessionReview() + ")";
    }
}
