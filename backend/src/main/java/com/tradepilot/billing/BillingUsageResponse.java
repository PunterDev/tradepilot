package com.tradepilot.billing;

import java.util.Map;

public class BillingUsageResponse {
    private PlanType plan;
    private SubscriptionStatus subscriptionStatus;
    private Map<String, Integer> limits;
    private Map<String, Integer> usageThisMonth;
    private Map<String, Integer> remaining;
    private String resetDate;
    private Integer monthlyAiCredits;
    private Integer monthlyAiCreditsUsed;
    private Integer monthlyAiCreditsRemaining;
    private Integer purchasedAiCreditsBalance;
    private Integer totalAiCreditsAvailable;
    private Map<String, Integer> featureCosts;

    @java.lang.SuppressWarnings("all")
    public BillingUsageResponse() {
    }

    @java.lang.SuppressWarnings("all")
    public PlanType getPlan() {
        return this.plan;
    }

    @java.lang.SuppressWarnings("all")
    public SubscriptionStatus getSubscriptionStatus() {
        return this.subscriptionStatus;
    }

    @java.lang.SuppressWarnings("all")
    public Map<String, Integer> getLimits() {
        return this.limits;
    }

    @java.lang.SuppressWarnings("all")
    public Map<String, Integer> getUsageThisMonth() {
        return this.usageThisMonth;
    }

    @java.lang.SuppressWarnings("all")
    public Map<String, Integer> getRemaining() {
        return this.remaining;
    }

    @java.lang.SuppressWarnings("all")
    public String getResetDate() {
        return this.resetDate;
    }

    @java.lang.SuppressWarnings("all")
    public Integer getMonthlyAiCredits() {
        return this.monthlyAiCredits;
    }

    @java.lang.SuppressWarnings("all")
    public Integer getMonthlyAiCreditsUsed() {
        return this.monthlyAiCreditsUsed;
    }

    @java.lang.SuppressWarnings("all")
    public Integer getMonthlyAiCreditsRemaining() {
        return this.monthlyAiCreditsRemaining;
    }

    @java.lang.SuppressWarnings("all")
    public Integer getPurchasedAiCreditsBalance() {
        return this.purchasedAiCreditsBalance;
    }

    @java.lang.SuppressWarnings("all")
    public Integer getTotalAiCreditsAvailable() {
        return this.totalAiCreditsAvailable;
    }

    @java.lang.SuppressWarnings("all")
    public Map<String, Integer> getFeatureCosts() {
        return this.featureCosts;
    }

    @java.lang.SuppressWarnings("all")
    public void setPlan(final PlanType plan) {
        this.plan = plan;
    }

    @java.lang.SuppressWarnings("all")
    public void setSubscriptionStatus(final SubscriptionStatus subscriptionStatus) {
        this.subscriptionStatus = subscriptionStatus;
    }

    @java.lang.SuppressWarnings("all")
    public void setLimits(final Map<String, Integer> limits) {
        this.limits = limits;
    }

    @java.lang.SuppressWarnings("all")
    public void setUsageThisMonth(final Map<String, Integer> usageThisMonth) {
        this.usageThisMonth = usageThisMonth;
    }

    @java.lang.SuppressWarnings("all")
    public void setRemaining(final Map<String, Integer> remaining) {
        this.remaining = remaining;
    }

    @java.lang.SuppressWarnings("all")
    public void setResetDate(final String resetDate) {
        this.resetDate = resetDate;
    }

    @java.lang.SuppressWarnings("all")
    public void setMonthlyAiCredits(final Integer monthlyAiCredits) {
        this.monthlyAiCredits = monthlyAiCredits;
    }

    @java.lang.SuppressWarnings("all")
    public void setMonthlyAiCreditsUsed(final Integer monthlyAiCreditsUsed) {
        this.monthlyAiCreditsUsed = monthlyAiCreditsUsed;
    }

    @java.lang.SuppressWarnings("all")
    public void setMonthlyAiCreditsRemaining(final Integer monthlyAiCreditsRemaining) {
        this.monthlyAiCreditsRemaining = monthlyAiCreditsRemaining;
    }

    @java.lang.SuppressWarnings("all")
    public void setPurchasedAiCreditsBalance(final Integer purchasedAiCreditsBalance) {
        this.purchasedAiCreditsBalance = purchasedAiCreditsBalance;
    }

    @java.lang.SuppressWarnings("all")
    public void setTotalAiCreditsAvailable(final Integer totalAiCreditsAvailable) {
        this.totalAiCreditsAvailable = totalAiCreditsAvailable;
    }

    @java.lang.SuppressWarnings("all")
    public void setFeatureCosts(final Map<String, Integer> featureCosts) {
        this.featureCosts = featureCosts;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof BillingUsageResponse)) return false;
        final BillingUsageResponse other = (BillingUsageResponse) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$monthlyAiCredits = this.getMonthlyAiCredits();
        final java.lang.Object other$monthlyAiCredits = other.getMonthlyAiCredits();
        if (this$monthlyAiCredits == null ? other$monthlyAiCredits != null : !this$monthlyAiCredits.equals(other$monthlyAiCredits)) return false;
        final java.lang.Object this$monthlyAiCreditsUsed = this.getMonthlyAiCreditsUsed();
        final java.lang.Object other$monthlyAiCreditsUsed = other.getMonthlyAiCreditsUsed();
        if (this$monthlyAiCreditsUsed == null ? other$monthlyAiCreditsUsed != null : !this$monthlyAiCreditsUsed.equals(other$monthlyAiCreditsUsed)) return false;
        final java.lang.Object this$monthlyAiCreditsRemaining = this.getMonthlyAiCreditsRemaining();
        final java.lang.Object other$monthlyAiCreditsRemaining = other.getMonthlyAiCreditsRemaining();
        if (this$monthlyAiCreditsRemaining == null ? other$monthlyAiCreditsRemaining != null : !this$monthlyAiCreditsRemaining.equals(other$monthlyAiCreditsRemaining)) return false;
        final java.lang.Object this$purchasedAiCreditsBalance = this.getPurchasedAiCreditsBalance();
        final java.lang.Object other$purchasedAiCreditsBalance = other.getPurchasedAiCreditsBalance();
        if (this$purchasedAiCreditsBalance == null ? other$purchasedAiCreditsBalance != null : !this$purchasedAiCreditsBalance.equals(other$purchasedAiCreditsBalance)) return false;
        final java.lang.Object this$totalAiCreditsAvailable = this.getTotalAiCreditsAvailable();
        final java.lang.Object other$totalAiCreditsAvailable = other.getTotalAiCreditsAvailable();
        if (this$totalAiCreditsAvailable == null ? other$totalAiCreditsAvailable != null : !this$totalAiCreditsAvailable.equals(other$totalAiCreditsAvailable)) return false;
        final java.lang.Object this$plan = this.getPlan();
        final java.lang.Object other$plan = other.getPlan();
        if (this$plan == null ? other$plan != null : !this$plan.equals(other$plan)) return false;
        final java.lang.Object this$subscriptionStatus = this.getSubscriptionStatus();
        final java.lang.Object other$subscriptionStatus = other.getSubscriptionStatus();
        if (this$subscriptionStatus == null ? other$subscriptionStatus != null : !this$subscriptionStatus.equals(other$subscriptionStatus)) return false;
        final java.lang.Object this$limits = this.getLimits();
        final java.lang.Object other$limits = other.getLimits();
        if (this$limits == null ? other$limits != null : !this$limits.equals(other$limits)) return false;
        final java.lang.Object this$usageThisMonth = this.getUsageThisMonth();
        final java.lang.Object other$usageThisMonth = other.getUsageThisMonth();
        if (this$usageThisMonth == null ? other$usageThisMonth != null : !this$usageThisMonth.equals(other$usageThisMonth)) return false;
        final java.lang.Object this$remaining = this.getRemaining();
        final java.lang.Object other$remaining = other.getRemaining();
        if (this$remaining == null ? other$remaining != null : !this$remaining.equals(other$remaining)) return false;
        final java.lang.Object this$resetDate = this.getResetDate();
        final java.lang.Object other$resetDate = other.getResetDate();
        if (this$resetDate == null ? other$resetDate != null : !this$resetDate.equals(other$resetDate)) return false;
        return true;
    }

    @java.lang.SuppressWarnings("all")
    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof BillingUsageResponse;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $monthlyAiCredits = this.getMonthlyAiCredits();
        result = result * PRIME + ($monthlyAiCredits == null ? 43 : $monthlyAiCredits.hashCode());
        final java.lang.Object $monthlyAiCreditsUsed = this.getMonthlyAiCreditsUsed();
        result = result * PRIME + ($monthlyAiCreditsUsed == null ? 43 : $monthlyAiCreditsUsed.hashCode());
        final java.lang.Object $monthlyAiCreditsRemaining = this.getMonthlyAiCreditsRemaining();
        result = result * PRIME + ($monthlyAiCreditsRemaining == null ? 43 : $monthlyAiCreditsRemaining.hashCode());
        final java.lang.Object $purchasedAiCreditsBalance = this.getPurchasedAiCreditsBalance();
        result = result * PRIME + ($purchasedAiCreditsBalance == null ? 43 : $purchasedAiCreditsBalance.hashCode());
        final java.lang.Object $totalAiCreditsAvailable = this.getTotalAiCreditsAvailable();
        result = result * PRIME + ($totalAiCreditsAvailable == null ? 43 : $totalAiCreditsAvailable.hashCode());
        final java.lang.Object $plan = this.getPlan();
        result = result * PRIME + ($plan == null ? 43 : $plan.hashCode());
        final java.lang.Object $subscriptionStatus = this.getSubscriptionStatus();
        result = result * PRIME + ($subscriptionStatus == null ? 43 : $subscriptionStatus.hashCode());
        final java.lang.Object $limits = this.getLimits();
        result = result * PRIME + ($limits == null ? 43 : $limits.hashCode());
        final java.lang.Object $usageThisMonth = this.getUsageThisMonth();
        result = result * PRIME + ($usageThisMonth == null ? 43 : $usageThisMonth.hashCode());
        final java.lang.Object $remaining = this.getRemaining();
        result = result * PRIME + ($remaining == null ? 43 : $remaining.hashCode());
        final java.lang.Object $resetDate = this.getResetDate();
        result = result * PRIME + ($resetDate == null ? 43 : $resetDate.hashCode());
        return result;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public java.lang.String toString() {
        return "BillingUsageResponse(plan=" + this.getPlan() + ", subscriptionStatus=" + this.getSubscriptionStatus() + ", limits=" + this.getLimits() + ", usageThisMonth=" + this.getUsageThisMonth() + ", remaining=" + this.getRemaining() + ", resetDate=" + this.getResetDate() + ", monthlyAiCredits=" + this.getMonthlyAiCredits() + ", monthlyAiCreditsUsed=" + this.getMonthlyAiCreditsUsed() + ", monthlyAiCreditsRemaining=" + this.getMonthlyAiCreditsRemaining() + ", purchasedAiCreditsBalance=" + this.getPurchasedAiCreditsBalance() + ", totalAiCreditsAvailable=" + this.getTotalAiCreditsAvailable() + ")";
    }
}
