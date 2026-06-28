package com.tradepilot.retention.dto;

public class RetentionSummaryResponse {
    private long totalUsers;
    private long dau;
    private long wau;
    private long mau;
    private double dauMauRatio;
    private long newUsers30d;
    private double retention7d;
    private double retention30d;
    private long powerUsers;
    private long atRiskUsers;
    private long inactiveUsers;
    private RetentionPlanSegmentResponse freeUsers;
    private RetentionPlanSegmentResponse proUsers;

    @java.lang.SuppressWarnings("all")
    RetentionSummaryResponse(final long totalUsers, final long dau, final long wau, final long mau, final double dauMauRatio, final long newUsers30d, final double retention7d, final double retention30d, final long powerUsers, final long atRiskUsers, final long inactiveUsers, final RetentionPlanSegmentResponse freeUsers, final RetentionPlanSegmentResponse proUsers) {
        this.totalUsers = totalUsers;
        this.dau = dau;
        this.wau = wau;
        this.mau = mau;
        this.dauMauRatio = dauMauRatio;
        this.newUsers30d = newUsers30d;
        this.retention7d = retention7d;
        this.retention30d = retention30d;
        this.powerUsers = powerUsers;
        this.atRiskUsers = atRiskUsers;
        this.inactiveUsers = inactiveUsers;
        this.freeUsers = freeUsers;
        this.proUsers = proUsers;
    }


    @java.lang.SuppressWarnings("all")
    public static class RetentionSummaryResponseBuilder {
        @java.lang.SuppressWarnings("all")
        private long totalUsers;
        @java.lang.SuppressWarnings("all")
        private long dau;
        @java.lang.SuppressWarnings("all")
        private long wau;
        @java.lang.SuppressWarnings("all")
        private long mau;
        @java.lang.SuppressWarnings("all")
        private double dauMauRatio;
        @java.lang.SuppressWarnings("all")
        private long newUsers30d;
        @java.lang.SuppressWarnings("all")
        private double retention7d;
        @java.lang.SuppressWarnings("all")
        private double retention30d;
        @java.lang.SuppressWarnings("all")
        private long powerUsers;
        @java.lang.SuppressWarnings("all")
        private long atRiskUsers;
        @java.lang.SuppressWarnings("all")
        private long inactiveUsers;
        @java.lang.SuppressWarnings("all")
        private RetentionPlanSegmentResponse freeUsers;
        @java.lang.SuppressWarnings("all")
        private RetentionPlanSegmentResponse proUsers;

        @java.lang.SuppressWarnings("all")
        RetentionSummaryResponseBuilder() {
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public RetentionSummaryResponse.RetentionSummaryResponseBuilder totalUsers(final long totalUsers) {
            this.totalUsers = totalUsers;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public RetentionSummaryResponse.RetentionSummaryResponseBuilder dau(final long dau) {
            this.dau = dau;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public RetentionSummaryResponse.RetentionSummaryResponseBuilder wau(final long wau) {
            this.wau = wau;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public RetentionSummaryResponse.RetentionSummaryResponseBuilder mau(final long mau) {
            this.mau = mau;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public RetentionSummaryResponse.RetentionSummaryResponseBuilder dauMauRatio(final double dauMauRatio) {
            this.dauMauRatio = dauMauRatio;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public RetentionSummaryResponse.RetentionSummaryResponseBuilder newUsers30d(final long newUsers30d) {
            this.newUsers30d = newUsers30d;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public RetentionSummaryResponse.RetentionSummaryResponseBuilder retention7d(final double retention7d) {
            this.retention7d = retention7d;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public RetentionSummaryResponse.RetentionSummaryResponseBuilder retention30d(final double retention30d) {
            this.retention30d = retention30d;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public RetentionSummaryResponse.RetentionSummaryResponseBuilder powerUsers(final long powerUsers) {
            this.powerUsers = powerUsers;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public RetentionSummaryResponse.RetentionSummaryResponseBuilder atRiskUsers(final long atRiskUsers) {
            this.atRiskUsers = atRiskUsers;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public RetentionSummaryResponse.RetentionSummaryResponseBuilder inactiveUsers(final long inactiveUsers) {
            this.inactiveUsers = inactiveUsers;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public RetentionSummaryResponse.RetentionSummaryResponseBuilder freeUsers(final RetentionPlanSegmentResponse freeUsers) {
            this.freeUsers = freeUsers;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public RetentionSummaryResponse.RetentionSummaryResponseBuilder proUsers(final RetentionPlanSegmentResponse proUsers) {
            this.proUsers = proUsers;
            return this;
        }

        @java.lang.SuppressWarnings("all")
        public RetentionSummaryResponse build() {
            return new RetentionSummaryResponse(this.totalUsers, this.dau, this.wau, this.mau, this.dauMauRatio, this.newUsers30d, this.retention7d, this.retention30d, this.powerUsers, this.atRiskUsers, this.inactiveUsers, this.freeUsers, this.proUsers);
        }

        @java.lang.Override
        @java.lang.SuppressWarnings("all")
        public java.lang.String toString() {
            return "RetentionSummaryResponse.RetentionSummaryResponseBuilder(totalUsers=" + this.totalUsers + ", dau=" + this.dau + ", wau=" + this.wau + ", mau=" + this.mau + ", dauMauRatio=" + this.dauMauRatio + ", newUsers30d=" + this.newUsers30d + ", retention7d=" + this.retention7d + ", retention30d=" + this.retention30d + ", powerUsers=" + this.powerUsers + ", atRiskUsers=" + this.atRiskUsers + ", inactiveUsers=" + this.inactiveUsers + ", freeUsers=" + this.freeUsers + ", proUsers=" + this.proUsers + ")";
        }
    }

    @java.lang.SuppressWarnings("all")
    public static RetentionSummaryResponse.RetentionSummaryResponseBuilder builder() {
        return new RetentionSummaryResponse.RetentionSummaryResponseBuilder();
    }

    @java.lang.SuppressWarnings("all")
    public long getTotalUsers() {
        return this.totalUsers;
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
    public double getDauMauRatio() {
        return this.dauMauRatio;
    }

    @java.lang.SuppressWarnings("all")
    public long getNewUsers30d() {
        return this.newUsers30d;
    }

    @java.lang.SuppressWarnings("all")
    public double getRetention7d() {
        return this.retention7d;
    }

    @java.lang.SuppressWarnings("all")
    public double getRetention30d() {
        return this.retention30d;
    }

    @java.lang.SuppressWarnings("all")
    public long getPowerUsers() {
        return this.powerUsers;
    }

    @java.lang.SuppressWarnings("all")
    public long getAtRiskUsers() {
        return this.atRiskUsers;
    }

    @java.lang.SuppressWarnings("all")
    public long getInactiveUsers() {
        return this.inactiveUsers;
    }

    @java.lang.SuppressWarnings("all")
    public RetentionPlanSegmentResponse getFreeUsers() {
        return this.freeUsers;
    }

    @java.lang.SuppressWarnings("all")
    public RetentionPlanSegmentResponse getProUsers() {
        return this.proUsers;
    }

    @java.lang.SuppressWarnings("all")
    public void setTotalUsers(final long totalUsers) {
        this.totalUsers = totalUsers;
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

    @java.lang.SuppressWarnings("all")
    public void setDauMauRatio(final double dauMauRatio) {
        this.dauMauRatio = dauMauRatio;
    }

    @java.lang.SuppressWarnings("all")
    public void setNewUsers30d(final long newUsers30d) {
        this.newUsers30d = newUsers30d;
    }

    @java.lang.SuppressWarnings("all")
    public void setRetention7d(final double retention7d) {
        this.retention7d = retention7d;
    }

    @java.lang.SuppressWarnings("all")
    public void setRetention30d(final double retention30d) {
        this.retention30d = retention30d;
    }

    @java.lang.SuppressWarnings("all")
    public void setPowerUsers(final long powerUsers) {
        this.powerUsers = powerUsers;
    }

    @java.lang.SuppressWarnings("all")
    public void setAtRiskUsers(final long atRiskUsers) {
        this.atRiskUsers = atRiskUsers;
    }

    @java.lang.SuppressWarnings("all")
    public void setInactiveUsers(final long inactiveUsers) {
        this.inactiveUsers = inactiveUsers;
    }

    @java.lang.SuppressWarnings("all")
    public void setFreeUsers(final RetentionPlanSegmentResponse freeUsers) {
        this.freeUsers = freeUsers;
    }

    @java.lang.SuppressWarnings("all")
    public void setProUsers(final RetentionPlanSegmentResponse proUsers) {
        this.proUsers = proUsers;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof RetentionSummaryResponse)) return false;
        final RetentionSummaryResponse other = (RetentionSummaryResponse) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        if (this.getTotalUsers() != other.getTotalUsers()) return false;
        if (this.getDau() != other.getDau()) return false;
        if (this.getWau() != other.getWau()) return false;
        if (this.getMau() != other.getMau()) return false;
        if (java.lang.Double.compare(this.getDauMauRatio(), other.getDauMauRatio()) != 0) return false;
        if (this.getNewUsers30d() != other.getNewUsers30d()) return false;
        if (java.lang.Double.compare(this.getRetention7d(), other.getRetention7d()) != 0) return false;
        if (java.lang.Double.compare(this.getRetention30d(), other.getRetention30d()) != 0) return false;
        if (this.getPowerUsers() != other.getPowerUsers()) return false;
        if (this.getAtRiskUsers() != other.getAtRiskUsers()) return false;
        if (this.getInactiveUsers() != other.getInactiveUsers()) return false;
        final java.lang.Object this$freeUsers = this.getFreeUsers();
        final java.lang.Object other$freeUsers = other.getFreeUsers();
        if (this$freeUsers == null ? other$freeUsers != null : !this$freeUsers.equals(other$freeUsers)) return false;
        final java.lang.Object this$proUsers = this.getProUsers();
        final java.lang.Object other$proUsers = other.getProUsers();
        if (this$proUsers == null ? other$proUsers != null : !this$proUsers.equals(other$proUsers)) return false;
        return true;
    }

    @java.lang.SuppressWarnings("all")
    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof RetentionSummaryResponse;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final long $totalUsers = this.getTotalUsers();
        result = result * PRIME + (int) ($totalUsers >>> 32 ^ $totalUsers);
        final long $dau = this.getDau();
        result = result * PRIME + (int) ($dau >>> 32 ^ $dau);
        final long $wau = this.getWau();
        result = result * PRIME + (int) ($wau >>> 32 ^ $wau);
        final long $mau = this.getMau();
        result = result * PRIME + (int) ($mau >>> 32 ^ $mau);
        final long $dauMauRatio = java.lang.Double.doubleToLongBits(this.getDauMauRatio());
        result = result * PRIME + (int) ($dauMauRatio >>> 32 ^ $dauMauRatio);
        final long $newUsers30d = this.getNewUsers30d();
        result = result * PRIME + (int) ($newUsers30d >>> 32 ^ $newUsers30d);
        final long $retention7d = java.lang.Double.doubleToLongBits(this.getRetention7d());
        result = result * PRIME + (int) ($retention7d >>> 32 ^ $retention7d);
        final long $retention30d = java.lang.Double.doubleToLongBits(this.getRetention30d());
        result = result * PRIME + (int) ($retention30d >>> 32 ^ $retention30d);
        final long $powerUsers = this.getPowerUsers();
        result = result * PRIME + (int) ($powerUsers >>> 32 ^ $powerUsers);
        final long $atRiskUsers = this.getAtRiskUsers();
        result = result * PRIME + (int) ($atRiskUsers >>> 32 ^ $atRiskUsers);
        final long $inactiveUsers = this.getInactiveUsers();
        result = result * PRIME + (int) ($inactiveUsers >>> 32 ^ $inactiveUsers);
        final java.lang.Object $freeUsers = this.getFreeUsers();
        result = result * PRIME + ($freeUsers == null ? 43 : $freeUsers.hashCode());
        final java.lang.Object $proUsers = this.getProUsers();
        result = result * PRIME + ($proUsers == null ? 43 : $proUsers.hashCode());
        return result;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public java.lang.String toString() {
        return "RetentionSummaryResponse(totalUsers=" + this.getTotalUsers() + ", dau=" + this.getDau() + ", wau=" + this.getWau() + ", mau=" + this.getMau() + ", dauMauRatio=" + this.getDauMauRatio() + ", newUsers30d=" + this.getNewUsers30d() + ", retention7d=" + this.getRetention7d() + ", retention30d=" + this.getRetention30d() + ", powerUsers=" + this.getPowerUsers() + ", atRiskUsers=" + this.getAtRiskUsers() + ", inactiveUsers=" + this.getInactiveUsers() + ", freeUsers=" + this.getFreeUsers() + ", proUsers=" + this.getProUsers() + ")";
    }
}
