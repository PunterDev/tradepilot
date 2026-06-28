package com.tradepilot.dto.admin;

import java.math.BigDecimal;
import java.util.List;

public class AdminBillingSummaryResponse {
    private long totalUsers;
    private long freeUsers;
    private long proUsers;
    private long stripePayingSubscribers;
    private long manualProUsers;
    private double conversionRate;
    private double paidConversionRate;
    private BigDecimal estimatedMrr;
    private BigDecimal estimatedArr;
    private BigDecimal potentialMrr;
    private BigDecimal potentialArr;
    private long manualUpgrades;
    private long stripeSubscriptions;
    private boolean stripeCheckoutConfigured;
    private boolean stripeWebhookConfigured;
    private boolean monthlyPriceIdConfigured;
    private boolean yearlyPriceIdConfigured;
    private List<AdminRecentUpgradeResponse> recentPlanChanges;

    @java.lang.SuppressWarnings("all")
    AdminBillingSummaryResponse(final long totalUsers, final long freeUsers, final long proUsers, final long stripePayingSubscribers, final long manualProUsers, final double conversionRate, final double paidConversionRate, final BigDecimal estimatedMrr, final BigDecimal estimatedArr, final BigDecimal potentialMrr, final BigDecimal potentialArr, final long manualUpgrades, final long stripeSubscriptions, final boolean stripeCheckoutConfigured, final boolean stripeWebhookConfigured, final boolean monthlyPriceIdConfigured, final boolean yearlyPriceIdConfigured, final List<AdminRecentUpgradeResponse> recentPlanChanges) {
        this.totalUsers = totalUsers;
        this.freeUsers = freeUsers;
        this.proUsers = proUsers;
        this.stripePayingSubscribers = stripePayingSubscribers;
        this.manualProUsers = manualProUsers;
        this.conversionRate = conversionRate;
        this.paidConversionRate = paidConversionRate;
        this.estimatedMrr = estimatedMrr;
        this.estimatedArr = estimatedArr;
        this.potentialMrr = potentialMrr;
        this.potentialArr = potentialArr;
        this.manualUpgrades = manualUpgrades;
        this.stripeSubscriptions = stripeSubscriptions;
        this.stripeCheckoutConfigured = stripeCheckoutConfigured;
        this.stripeWebhookConfigured = stripeWebhookConfigured;
        this.monthlyPriceIdConfigured = monthlyPriceIdConfigured;
        this.yearlyPriceIdConfigured = yearlyPriceIdConfigured;
        this.recentPlanChanges = recentPlanChanges;
    }


    @java.lang.SuppressWarnings("all")
    public static class AdminBillingSummaryResponseBuilder {
        @java.lang.SuppressWarnings("all")
        private long totalUsers;
        @java.lang.SuppressWarnings("all")
        private long freeUsers;
        @java.lang.SuppressWarnings("all")
        private long proUsers;
        @java.lang.SuppressWarnings("all")
        private long stripePayingSubscribers;
        @java.lang.SuppressWarnings("all")
        private long manualProUsers;
        @java.lang.SuppressWarnings("all")
        private double conversionRate;
        @java.lang.SuppressWarnings("all")
        private double paidConversionRate;
        @java.lang.SuppressWarnings("all")
        private BigDecimal estimatedMrr;
        @java.lang.SuppressWarnings("all")
        private BigDecimal estimatedArr;
        @java.lang.SuppressWarnings("all")
        private BigDecimal potentialMrr;
        @java.lang.SuppressWarnings("all")
        private BigDecimal potentialArr;
        @java.lang.SuppressWarnings("all")
        private long manualUpgrades;
        @java.lang.SuppressWarnings("all")
        private long stripeSubscriptions;
        @java.lang.SuppressWarnings("all")
        private boolean stripeCheckoutConfigured;
        @java.lang.SuppressWarnings("all")
        private boolean stripeWebhookConfigured;
        @java.lang.SuppressWarnings("all")
        private boolean monthlyPriceIdConfigured;
        @java.lang.SuppressWarnings("all")
        private boolean yearlyPriceIdConfigured;
        @java.lang.SuppressWarnings("all")
        private List<AdminRecentUpgradeResponse> recentPlanChanges;

        @java.lang.SuppressWarnings("all")
        AdminBillingSummaryResponseBuilder() {
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public AdminBillingSummaryResponse.AdminBillingSummaryResponseBuilder totalUsers(final long totalUsers) {
            this.totalUsers = totalUsers;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public AdminBillingSummaryResponse.AdminBillingSummaryResponseBuilder freeUsers(final long freeUsers) {
            this.freeUsers = freeUsers;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public AdminBillingSummaryResponse.AdminBillingSummaryResponseBuilder proUsers(final long proUsers) {
            this.proUsers = proUsers;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public AdminBillingSummaryResponse.AdminBillingSummaryResponseBuilder stripePayingSubscribers(final long stripePayingSubscribers) {
            this.stripePayingSubscribers = stripePayingSubscribers;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public AdminBillingSummaryResponse.AdminBillingSummaryResponseBuilder manualProUsers(final long manualProUsers) {
            this.manualProUsers = manualProUsers;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public AdminBillingSummaryResponse.AdminBillingSummaryResponseBuilder conversionRate(final double conversionRate) {
            this.conversionRate = conversionRate;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public AdminBillingSummaryResponse.AdminBillingSummaryResponseBuilder paidConversionRate(final double paidConversionRate) {
            this.paidConversionRate = paidConversionRate;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public AdminBillingSummaryResponse.AdminBillingSummaryResponseBuilder estimatedMrr(final BigDecimal estimatedMrr) {
            this.estimatedMrr = estimatedMrr;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public AdminBillingSummaryResponse.AdminBillingSummaryResponseBuilder estimatedArr(final BigDecimal estimatedArr) {
            this.estimatedArr = estimatedArr;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public AdminBillingSummaryResponse.AdminBillingSummaryResponseBuilder potentialMrr(final BigDecimal potentialMrr) {
            this.potentialMrr = potentialMrr;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public AdminBillingSummaryResponse.AdminBillingSummaryResponseBuilder potentialArr(final BigDecimal potentialArr) {
            this.potentialArr = potentialArr;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public AdminBillingSummaryResponse.AdminBillingSummaryResponseBuilder manualUpgrades(final long manualUpgrades) {
            this.manualUpgrades = manualUpgrades;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public AdminBillingSummaryResponse.AdminBillingSummaryResponseBuilder stripeSubscriptions(final long stripeSubscriptions) {
            this.stripeSubscriptions = stripeSubscriptions;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public AdminBillingSummaryResponse.AdminBillingSummaryResponseBuilder stripeCheckoutConfigured(final boolean stripeCheckoutConfigured) {
            this.stripeCheckoutConfigured = stripeCheckoutConfigured;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public AdminBillingSummaryResponse.AdminBillingSummaryResponseBuilder stripeWebhookConfigured(final boolean stripeWebhookConfigured) {
            this.stripeWebhookConfigured = stripeWebhookConfigured;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public AdminBillingSummaryResponse.AdminBillingSummaryResponseBuilder monthlyPriceIdConfigured(final boolean monthlyPriceIdConfigured) {
            this.monthlyPriceIdConfigured = monthlyPriceIdConfigured;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public AdminBillingSummaryResponse.AdminBillingSummaryResponseBuilder yearlyPriceIdConfigured(final boolean yearlyPriceIdConfigured) {
            this.yearlyPriceIdConfigured = yearlyPriceIdConfigured;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public AdminBillingSummaryResponse.AdminBillingSummaryResponseBuilder recentPlanChanges(final List<AdminRecentUpgradeResponse> recentPlanChanges) {
            this.recentPlanChanges = recentPlanChanges;
            return this;
        }

        @java.lang.SuppressWarnings("all")
        public AdminBillingSummaryResponse build() {
            return new AdminBillingSummaryResponse(this.totalUsers, this.freeUsers, this.proUsers, this.stripePayingSubscribers, this.manualProUsers, this.conversionRate, this.paidConversionRate, this.estimatedMrr, this.estimatedArr, this.potentialMrr, this.potentialArr, this.manualUpgrades, this.stripeSubscriptions, this.stripeCheckoutConfigured, this.stripeWebhookConfigured, this.monthlyPriceIdConfigured, this.yearlyPriceIdConfigured, this.recentPlanChanges);
        }

        @java.lang.Override
        @java.lang.SuppressWarnings("all")
        public java.lang.String toString() {
            return "AdminBillingSummaryResponse.AdminBillingSummaryResponseBuilder(totalUsers=" + this.totalUsers + ", freeUsers=" + this.freeUsers + ", proUsers=" + this.proUsers + ", stripePayingSubscribers=" + this.stripePayingSubscribers + ", manualProUsers=" + this.manualProUsers + ", conversionRate=" + this.conversionRate + ", paidConversionRate=" + this.paidConversionRate + ", estimatedMrr=" + this.estimatedMrr + ", estimatedArr=" + this.estimatedArr + ", potentialMrr=" + this.potentialMrr + ", potentialArr=" + this.potentialArr + ", manualUpgrades=" + this.manualUpgrades + ", stripeSubscriptions=" + this.stripeSubscriptions + ", stripeCheckoutConfigured=" + this.stripeCheckoutConfigured + ", stripeWebhookConfigured=" + this.stripeWebhookConfigured + ", monthlyPriceIdConfigured=" + this.monthlyPriceIdConfigured + ", yearlyPriceIdConfigured=" + this.yearlyPriceIdConfigured + ", recentPlanChanges=" + this.recentPlanChanges + ")";
        }
    }

    @java.lang.SuppressWarnings("all")
    public static AdminBillingSummaryResponse.AdminBillingSummaryResponseBuilder builder() {
        return new AdminBillingSummaryResponse.AdminBillingSummaryResponseBuilder();
    }

    @java.lang.SuppressWarnings("all")
    public long getTotalUsers() {
        return this.totalUsers;
    }

    @java.lang.SuppressWarnings("all")
    public long getFreeUsers() {
        return this.freeUsers;
    }

    @java.lang.SuppressWarnings("all")
    public long getProUsers() {
        return this.proUsers;
    }

    @java.lang.SuppressWarnings("all")
    public long getStripePayingSubscribers() {
        return this.stripePayingSubscribers;
    }

    @java.lang.SuppressWarnings("all")
    public long getManualProUsers() {
        return this.manualProUsers;
    }

    @java.lang.SuppressWarnings("all")
    public double getConversionRate() {
        return this.conversionRate;
    }

    @java.lang.SuppressWarnings("all")
    public double getPaidConversionRate() {
        return this.paidConversionRate;
    }

    @java.lang.SuppressWarnings("all")
    public BigDecimal getEstimatedMrr() {
        return this.estimatedMrr;
    }

    @java.lang.SuppressWarnings("all")
    public BigDecimal getEstimatedArr() {
        return this.estimatedArr;
    }

    @java.lang.SuppressWarnings("all")
    public BigDecimal getPotentialMrr() {
        return this.potentialMrr;
    }

    @java.lang.SuppressWarnings("all")
    public BigDecimal getPotentialArr() {
        return this.potentialArr;
    }

    @java.lang.SuppressWarnings("all")
    public long getManualUpgrades() {
        return this.manualUpgrades;
    }

    @java.lang.SuppressWarnings("all")
    public long getStripeSubscriptions() {
        return this.stripeSubscriptions;
    }

    @java.lang.SuppressWarnings("all")
    public boolean isStripeCheckoutConfigured() {
        return this.stripeCheckoutConfigured;
    }

    @java.lang.SuppressWarnings("all")
    public boolean isStripeWebhookConfigured() {
        return this.stripeWebhookConfigured;
    }

    @java.lang.SuppressWarnings("all")
    public boolean isMonthlyPriceIdConfigured() {
        return this.monthlyPriceIdConfigured;
    }

    @java.lang.SuppressWarnings("all")
    public boolean isYearlyPriceIdConfigured() {
        return this.yearlyPriceIdConfigured;
    }

    @java.lang.SuppressWarnings("all")
    public List<AdminRecentUpgradeResponse> getRecentPlanChanges() {
        return this.recentPlanChanges;
    }

    @java.lang.SuppressWarnings("all")
    public void setTotalUsers(final long totalUsers) {
        this.totalUsers = totalUsers;
    }

    @java.lang.SuppressWarnings("all")
    public void setFreeUsers(final long freeUsers) {
        this.freeUsers = freeUsers;
    }

    @java.lang.SuppressWarnings("all")
    public void setProUsers(final long proUsers) {
        this.proUsers = proUsers;
    }

    @java.lang.SuppressWarnings("all")
    public void setStripePayingSubscribers(final long stripePayingSubscribers) {
        this.stripePayingSubscribers = stripePayingSubscribers;
    }

    @java.lang.SuppressWarnings("all")
    public void setManualProUsers(final long manualProUsers) {
        this.manualProUsers = manualProUsers;
    }

    @java.lang.SuppressWarnings("all")
    public void setConversionRate(final double conversionRate) {
        this.conversionRate = conversionRate;
    }

    @java.lang.SuppressWarnings("all")
    public void setPaidConversionRate(final double paidConversionRate) {
        this.paidConversionRate = paidConversionRate;
    }

    @java.lang.SuppressWarnings("all")
    public void setEstimatedMrr(final BigDecimal estimatedMrr) {
        this.estimatedMrr = estimatedMrr;
    }

    @java.lang.SuppressWarnings("all")
    public void setEstimatedArr(final BigDecimal estimatedArr) {
        this.estimatedArr = estimatedArr;
    }

    @java.lang.SuppressWarnings("all")
    public void setPotentialMrr(final BigDecimal potentialMrr) {
        this.potentialMrr = potentialMrr;
    }

    @java.lang.SuppressWarnings("all")
    public void setPotentialArr(final BigDecimal potentialArr) {
        this.potentialArr = potentialArr;
    }

    @java.lang.SuppressWarnings("all")
    public void setManualUpgrades(final long manualUpgrades) {
        this.manualUpgrades = manualUpgrades;
    }

    @java.lang.SuppressWarnings("all")
    public void setStripeSubscriptions(final long stripeSubscriptions) {
        this.stripeSubscriptions = stripeSubscriptions;
    }

    @java.lang.SuppressWarnings("all")
    public void setStripeCheckoutConfigured(final boolean stripeCheckoutConfigured) {
        this.stripeCheckoutConfigured = stripeCheckoutConfigured;
    }

    @java.lang.SuppressWarnings("all")
    public void setStripeWebhookConfigured(final boolean stripeWebhookConfigured) {
        this.stripeWebhookConfigured = stripeWebhookConfigured;
    }

    @java.lang.SuppressWarnings("all")
    public void setMonthlyPriceIdConfigured(final boolean monthlyPriceIdConfigured) {
        this.monthlyPriceIdConfigured = monthlyPriceIdConfigured;
    }

    @java.lang.SuppressWarnings("all")
    public void setYearlyPriceIdConfigured(final boolean yearlyPriceIdConfigured) {
        this.yearlyPriceIdConfigured = yearlyPriceIdConfigured;
    }

    @java.lang.SuppressWarnings("all")
    public void setRecentPlanChanges(final List<AdminRecentUpgradeResponse> recentPlanChanges) {
        this.recentPlanChanges = recentPlanChanges;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof AdminBillingSummaryResponse)) return false;
        final AdminBillingSummaryResponse other = (AdminBillingSummaryResponse) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        if (this.getTotalUsers() != other.getTotalUsers()) return false;
        if (this.getFreeUsers() != other.getFreeUsers()) return false;
        if (this.getProUsers() != other.getProUsers()) return false;
        if (this.getStripePayingSubscribers() != other.getStripePayingSubscribers()) return false;
        if (this.getManualProUsers() != other.getManualProUsers()) return false;
        if (java.lang.Double.compare(this.getConversionRate(), other.getConversionRate()) != 0) return false;
        if (java.lang.Double.compare(this.getPaidConversionRate(), other.getPaidConversionRate()) != 0) return false;
        if (this.getManualUpgrades() != other.getManualUpgrades()) return false;
        if (this.getStripeSubscriptions() != other.getStripeSubscriptions()) return false;
        if (this.isStripeCheckoutConfigured() != other.isStripeCheckoutConfigured()) return false;
        if (this.isStripeWebhookConfigured() != other.isStripeWebhookConfigured()) return false;
        if (this.isMonthlyPriceIdConfigured() != other.isMonthlyPriceIdConfigured()) return false;
        if (this.isYearlyPriceIdConfigured() != other.isYearlyPriceIdConfigured()) return false;
        final java.lang.Object this$estimatedMrr = this.getEstimatedMrr();
        final java.lang.Object other$estimatedMrr = other.getEstimatedMrr();
        if (this$estimatedMrr == null ? other$estimatedMrr != null : !this$estimatedMrr.equals(other$estimatedMrr)) return false;
        final java.lang.Object this$estimatedArr = this.getEstimatedArr();
        final java.lang.Object other$estimatedArr = other.getEstimatedArr();
        if (this$estimatedArr == null ? other$estimatedArr != null : !this$estimatedArr.equals(other$estimatedArr)) return false;
        final java.lang.Object this$potentialMrr = this.getPotentialMrr();
        final java.lang.Object other$potentialMrr = other.getPotentialMrr();
        if (this$potentialMrr == null ? other$potentialMrr != null : !this$potentialMrr.equals(other$potentialMrr)) return false;
        final java.lang.Object this$potentialArr = this.getPotentialArr();
        final java.lang.Object other$potentialArr = other.getPotentialArr();
        if (this$potentialArr == null ? other$potentialArr != null : !this$potentialArr.equals(other$potentialArr)) return false;
        final java.lang.Object this$recentPlanChanges = this.getRecentPlanChanges();
        final java.lang.Object other$recentPlanChanges = other.getRecentPlanChanges();
        if (this$recentPlanChanges == null ? other$recentPlanChanges != null : !this$recentPlanChanges.equals(other$recentPlanChanges)) return false;
        return true;
    }

    @java.lang.SuppressWarnings("all")
    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof AdminBillingSummaryResponse;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final long $totalUsers = this.getTotalUsers();
        result = result * PRIME + (int) ($totalUsers >>> 32 ^ $totalUsers);
        final long $freeUsers = this.getFreeUsers();
        result = result * PRIME + (int) ($freeUsers >>> 32 ^ $freeUsers);
        final long $proUsers = this.getProUsers();
        result = result * PRIME + (int) ($proUsers >>> 32 ^ $proUsers);
        final long $stripePayingSubscribers = this.getStripePayingSubscribers();
        result = result * PRIME + (int) ($stripePayingSubscribers >>> 32 ^ $stripePayingSubscribers);
        final long $manualProUsers = this.getManualProUsers();
        result = result * PRIME + (int) ($manualProUsers >>> 32 ^ $manualProUsers);
        final long $conversionRate = java.lang.Double.doubleToLongBits(this.getConversionRate());
        result = result * PRIME + (int) ($conversionRate >>> 32 ^ $conversionRate);
        final long $paidConversionRate = java.lang.Double.doubleToLongBits(this.getPaidConversionRate());
        result = result * PRIME + (int) ($paidConversionRate >>> 32 ^ $paidConversionRate);
        final long $manualUpgrades = this.getManualUpgrades();
        result = result * PRIME + (int) ($manualUpgrades >>> 32 ^ $manualUpgrades);
        final long $stripeSubscriptions = this.getStripeSubscriptions();
        result = result * PRIME + (int) ($stripeSubscriptions >>> 32 ^ $stripeSubscriptions);
        result = result * PRIME + (this.isStripeCheckoutConfigured() ? 79 : 97);
        result = result * PRIME + (this.isStripeWebhookConfigured() ? 79 : 97);
        result = result * PRIME + (this.isMonthlyPriceIdConfigured() ? 79 : 97);
        result = result * PRIME + (this.isYearlyPriceIdConfigured() ? 79 : 97);
        final java.lang.Object $estimatedMrr = this.getEstimatedMrr();
        result = result * PRIME + ($estimatedMrr == null ? 43 : $estimatedMrr.hashCode());
        final java.lang.Object $estimatedArr = this.getEstimatedArr();
        result = result * PRIME + ($estimatedArr == null ? 43 : $estimatedArr.hashCode());
        final java.lang.Object $potentialMrr = this.getPotentialMrr();
        result = result * PRIME + ($potentialMrr == null ? 43 : $potentialMrr.hashCode());
        final java.lang.Object $potentialArr = this.getPotentialArr();
        result = result * PRIME + ($potentialArr == null ? 43 : $potentialArr.hashCode());
        final java.lang.Object $recentPlanChanges = this.getRecentPlanChanges();
        result = result * PRIME + ($recentPlanChanges == null ? 43 : $recentPlanChanges.hashCode());
        return result;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public java.lang.String toString() {
        return "AdminBillingSummaryResponse(totalUsers=" + this.getTotalUsers() + ", freeUsers=" + this.getFreeUsers() + ", proUsers=" + this.getProUsers() + ", stripePayingSubscribers=" + this.getStripePayingSubscribers() + ", manualProUsers=" + this.getManualProUsers() + ", conversionRate=" + this.getConversionRate() + ", paidConversionRate=" + this.getPaidConversionRate() + ", estimatedMrr=" + this.getEstimatedMrr() + ", estimatedArr=" + this.getEstimatedArr() + ", potentialMrr=" + this.getPotentialMrr() + ", potentialArr=" + this.getPotentialArr() + ", manualUpgrades=" + this.getManualUpgrades() + ", stripeSubscriptions=" + this.getStripeSubscriptions() + ", stripeCheckoutConfigured=" + this.isStripeCheckoutConfigured() + ", stripeWebhookConfigured=" + this.isStripeWebhookConfigured() + ", monthlyPriceIdConfigured=" + this.isMonthlyPriceIdConfigured() + ", yearlyPriceIdConfigured=" + this.isYearlyPriceIdConfigured() + ", recentPlanChanges=" + this.getRecentPlanChanges() + ")";
    }
}
