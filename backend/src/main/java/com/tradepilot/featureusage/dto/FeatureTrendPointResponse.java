package com.tradepilot.featureusage.dto;

import java.time.LocalDate;

public class FeatureTrendPointResponse {
    private LocalDate date;
    private long count;

    @java.lang.SuppressWarnings("all")
    FeatureTrendPointResponse(final LocalDate date, final long count) {
        this.date = date;
        this.count = count;
    }


    @java.lang.SuppressWarnings("all")
    public static class FeatureTrendPointResponseBuilder {
        @java.lang.SuppressWarnings("all")
        private LocalDate date;
        @java.lang.SuppressWarnings("all")
        private long count;

        @java.lang.SuppressWarnings("all")
        FeatureTrendPointResponseBuilder() {
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public FeatureTrendPointResponse.FeatureTrendPointResponseBuilder date(final LocalDate date) {
            this.date = date;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public FeatureTrendPointResponse.FeatureTrendPointResponseBuilder count(final long count) {
            this.count = count;
            return this;
        }

        @java.lang.SuppressWarnings("all")
        public FeatureTrendPointResponse build() {
            return new FeatureTrendPointResponse(this.date, this.count);
        }

        @java.lang.Override
        @java.lang.SuppressWarnings("all")
        public java.lang.String toString() {
            return "FeatureTrendPointResponse.FeatureTrendPointResponseBuilder(date=" + this.date + ", count=" + this.count + ")";
        }
    }

    @java.lang.SuppressWarnings("all")
    public static FeatureTrendPointResponse.FeatureTrendPointResponseBuilder builder() {
        return new FeatureTrendPointResponse.FeatureTrendPointResponseBuilder();
    }

    @java.lang.SuppressWarnings("all")
    public LocalDate getDate() {
        return this.date;
    }

    @java.lang.SuppressWarnings("all")
    public long getCount() {
        return this.count;
    }

    @java.lang.SuppressWarnings("all")
    public void setDate(final LocalDate date) {
        this.date = date;
    }

    @java.lang.SuppressWarnings("all")
    public void setCount(final long count) {
        this.count = count;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof FeatureTrendPointResponse)) return false;
        final FeatureTrendPointResponse other = (FeatureTrendPointResponse) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        if (this.getCount() != other.getCount()) return false;
        final java.lang.Object this$date = this.getDate();
        final java.lang.Object other$date = other.getDate();
        if (this$date == null ? other$date != null : !this$date.equals(other$date)) return false;
        return true;
    }

    @java.lang.SuppressWarnings("all")
    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof FeatureTrendPointResponse;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final long $count = this.getCount();
        result = result * PRIME + (int) ($count >>> 32 ^ $count);
        final java.lang.Object $date = this.getDate();
        result = result * PRIME + ($date == null ? 43 : $date.hashCode());
        return result;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public java.lang.String toString() {
        return "FeatureTrendPointResponse(date=" + this.getDate() + ", count=" + this.getCount() + ")";
    }
}
