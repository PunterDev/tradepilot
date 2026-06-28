package com.tradepilot.retention.dto;

public class RetentionPlanSegmentResponse {
    private long dau;
    private long wau;
    private long mau;

    @java.lang.SuppressWarnings("all")
    RetentionPlanSegmentResponse(final long dau, final long wau, final long mau) {
        this.dau = dau;
        this.wau = wau;
        this.mau = mau;
    }


    @java.lang.SuppressWarnings("all")
    public static class RetentionPlanSegmentResponseBuilder {
        @java.lang.SuppressWarnings("all")
        private long dau;
        @java.lang.SuppressWarnings("all")
        private long wau;
        @java.lang.SuppressWarnings("all")
        private long mau;

        @java.lang.SuppressWarnings("all")
        RetentionPlanSegmentResponseBuilder() {
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public RetentionPlanSegmentResponse.RetentionPlanSegmentResponseBuilder dau(final long dau) {
            this.dau = dau;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public RetentionPlanSegmentResponse.RetentionPlanSegmentResponseBuilder wau(final long wau) {
            this.wau = wau;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public RetentionPlanSegmentResponse.RetentionPlanSegmentResponseBuilder mau(final long mau) {
            this.mau = mau;
            return this;
        }

        @java.lang.SuppressWarnings("all")
        public RetentionPlanSegmentResponse build() {
            return new RetentionPlanSegmentResponse(this.dau, this.wau, this.mau);
        }

        @java.lang.Override
        @java.lang.SuppressWarnings("all")
        public java.lang.String toString() {
            return "RetentionPlanSegmentResponse.RetentionPlanSegmentResponseBuilder(dau=" + this.dau + ", wau=" + this.wau + ", mau=" + this.mau + ")";
        }
    }

    @java.lang.SuppressWarnings("all")
    public static RetentionPlanSegmentResponse.RetentionPlanSegmentResponseBuilder builder() {
        return new RetentionPlanSegmentResponse.RetentionPlanSegmentResponseBuilder();
    }

    @java.lang.SuppressWarnings("all")
    public long getDau() {
        return this.dau;
    }

    @java.lang.SuppressWarnings("all")
    public long getWau() {
        return this.wau;
    }

    @java.lang.SuppressWarnings("all")
    public long getMau() {
        return this.mau;
    }

    @java.lang.SuppressWarnings("all")
    public void setDau(final long dau) {
        this.dau = dau;
    }

    @java.lang.SuppressWarnings("all")
    public void setWau(final long wau) {
        this.wau = wau;
    }

    @java.lang.SuppressWarnings("all")
    public void setMau(final long mau) {
        this.mau = mau;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof RetentionPlanSegmentResponse)) return false;
        final RetentionPlanSegmentResponse other = (RetentionPlanSegmentResponse) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        if (this.getDau() != other.getDau()) return false;
        if (this.getWau() != other.getWau()) return false;
        if (this.getMau() != other.getMau()) return false;
        return true;
    }

    @java.lang.SuppressWarnings("all")
    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof RetentionPlanSegmentResponse;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final long $dau = this.getDau();
        result = result * PRIME + (int) ($dau >>> 32 ^ $dau);
        final long $wau = this.getWau();
        result = result * PRIME + (int) ($wau >>> 32 ^ $wau);
        final long $mau = this.getMau();
        result = result * PRIME + (int) ($mau >>> 32 ^ $mau);
        return result;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public java.lang.String toString() {
        return "RetentionPlanSegmentResponse(dau=" + this.getDau() + ", wau=" + this.getWau() + ", mau=" + this.getMau() + ")";
    }
}
