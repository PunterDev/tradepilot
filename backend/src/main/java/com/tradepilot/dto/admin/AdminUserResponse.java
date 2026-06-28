package com.tradepilot.dto.admin;

import com.tradepilot.billing.PlanType;
import com.tradepilot.billing.SubscriptionStatus;
import com.tradepilot.enums.UserRole;
import java.time.LocalDateTime;
import java.util.Map;

public class AdminUserResponse {
    private Long id;
    private String email;
    private String name;
    private PlanType plan;
    private UserRole role;
    private LocalDateTime createdAt;
    private Boolean emailVerified;
    private String stripeCustomerId;
    private String stripeSubscriptionId;
    private LocalDateTime proUntil;
    private SubscriptionStatus subscriptionStatus;
    private long tradingAccountCount;
    private long tradeCount;
    private long aiReviewCount;
    private int dailyCoachUsage;
    private int weeklyCoachUsage;
    private int tradingDnaUsage;
    private String planSource;
    private Map<String, Integer> usageThisMonth;
    private Map<String, Integer> limits;
    private Map<String, Integer> remaining;

    @java.lang.SuppressWarnings("all")
    public AdminUserResponse() {
    }

    @java.lang.SuppressWarnings("all")
    public Long getId() {
        return this.id;
    }

    @java.lang.SuppressWarnings("all")
    public String getEmail() {
        return this.email;
    }

    @java.lang.SuppressWarnings("all")
    public String getName() {
        return this.name;
    }

    @java.lang.SuppressWarnings("all")
    public PlanType getPlan() {
        return this.plan;
    }

    @java.lang.SuppressWarnings("all")
    public UserRole getRole() {
        return this.role;
    }

    @java.lang.SuppressWarnings("all")
    public LocalDateTime getCreatedAt() {
        return this.createdAt;
    }

    @java.lang.SuppressWarnings("all")
    public Boolean getEmailVerified() {
        return this.emailVerified;
    }

    @java.lang.SuppressWarnings("all")
    public String getStripeCustomerId() {
        return this.stripeCustomerId;
    }

    @java.lang.SuppressWarnings("all")
    public String getStripeSubscriptionId() {
        return this.stripeSubscriptionId;
    }

    @java.lang.SuppressWarnings("all")
    public LocalDateTime getProUntil() {
        return this.proUntil;
    }

    @java.lang.SuppressWarnings("all")
    public SubscriptionStatus getSubscriptionStatus() {
        return this.subscriptionStatus;
    }

    @java.lang.SuppressWarnings("all")
    public long getTradingAccountCount() {
        return this.tradingAccountCount;
    }

    @java.lang.SuppressWarnings("all")
    public long getTradeCount() {
        return this.tradeCount;
    }

    @java.lang.SuppressWarnings("all")
    public long getAiReviewCount() {
        return this.aiReviewCount;
    }

    @java.lang.SuppressWarnings("all")
    public int getDailyCoachUsage() {
        return this.dailyCoachUsage;
    }

    @java.lang.SuppressWarnings("all")
    public int getWeeklyCoachUsage() {
        return this.weeklyCoachUsage;
    }

    @java.lang.SuppressWarnings("all")
    public int getTradingDnaUsage() {
        return this.tradingDnaUsage;
    }

    @java.lang.SuppressWarnings("all")
    public String getPlanSource() {
        return this.planSource;
    }

    @java.lang.SuppressWarnings("all")
    public Map<String, Integer> getUsageThisMonth() {
        return this.usageThisMonth;
    }

    @java.lang.SuppressWarnings("all")
    public Map<String, Integer> getLimits() {
        return this.limits;
    }

    @java.lang.SuppressWarnings("all")
    public Map<String, Integer> getRemaining() {
        return this.remaining;
    }

    @java.lang.SuppressWarnings("all")
    public void setId(final Long id) {
        this.id = id;
    }

    @java.lang.SuppressWarnings("all")
    public void setEmail(final String email) {
        this.email = email;
    }

    @java.lang.SuppressWarnings("all")
    public void setName(final String name) {
        this.name = name;
    }

    @java.lang.SuppressWarnings("all")
    public void setPlan(final PlanType plan) {
        this.plan = plan;
    }

    @java.lang.SuppressWarnings("all")
    public void setRole(final UserRole role) {
        this.role = role;
    }

    @java.lang.SuppressWarnings("all")
    public void setCreatedAt(final LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @java.lang.SuppressWarnings("all")
    public void setEmailVerified(final Boolean emailVerified) {
        this.emailVerified = emailVerified;
    }

    @java.lang.SuppressWarnings("all")
    public void setStripeCustomerId(final String stripeCustomerId) {
        this.stripeCustomerId = stripeCustomerId;
    }

    @java.lang.SuppressWarnings("all")
    public void setStripeSubscriptionId(final String stripeSubscriptionId) {
        this.stripeSubscriptionId = stripeSubscriptionId;
    }

    @java.lang.SuppressWarnings("all")
    public void setProUntil(final LocalDateTime proUntil) {
        this.proUntil = proUntil;
    }

    @java.lang.SuppressWarnings("all")
    public void setSubscriptionStatus(final SubscriptionStatus subscriptionStatus) {
        this.subscriptionStatus = subscriptionStatus;
    }

    @java.lang.SuppressWarnings("all")
    public void setTradingAccountCount(final long tradingAccountCount) {
        this.tradingAccountCount = tradingAccountCount;
    }

    @java.lang.SuppressWarnings("all")
    public void setTradeCount(final long tradeCount) {
        this.tradeCount = tradeCount;
    }

    @java.lang.SuppressWarnings("all")
    public void setAiReviewCount(final long aiReviewCount) {
        this.aiReviewCount = aiReviewCount;
    }

    @java.lang.SuppressWarnings("all")
    public void setDailyCoachUsage(final int dailyCoachUsage) {
        this.dailyCoachUsage = dailyCoachUsage;
    }

    @java.lang.SuppressWarnings("all")
    public void setWeeklyCoachUsage(final int weeklyCoachUsage) {
        this.weeklyCoachUsage = weeklyCoachUsage;
    }

    @java.lang.SuppressWarnings("all")
    public void setTradingDnaUsage(final int tradingDnaUsage) {
        this.tradingDnaUsage = tradingDnaUsage;
    }

    @java.lang.SuppressWarnings("all")
    public void setPlanSource(final String planSource) {
        this.planSource = planSource;
    }

    @java.lang.SuppressWarnings("all")
    public void setUsageThisMonth(final Map<String, Integer> usageThisMonth) {
        this.usageThisMonth = usageThisMonth;
    }

    @java.lang.SuppressWarnings("all")
    public void setLimits(final Map<String, Integer> limits) {
        this.limits = limits;
    }

    @java.lang.SuppressWarnings("all")
    public void setRemaining(final Map<String, Integer> remaining) {
        this.remaining = remaining;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof AdminUserResponse)) return false;
        final AdminUserResponse other = (AdminUserResponse) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        if (this.getTradingAccountCount() != other.getTradingAccountCount()) return false;
        if (this.getTradeCount() != other.getTradeCount()) return false;
        if (this.getAiReviewCount() != other.getAiReviewCount()) return false;
        if (this.getDailyCoachUsage() != other.getDailyCoachUsage()) return false;
        if (this.getWeeklyCoachUsage() != other.getWeeklyCoachUsage()) return false;
        if (this.getTradingDnaUsage() != other.getTradingDnaUsage()) return false;
        final java.lang.Object this$id = this.getId();
        final java.lang.Object other$id = other.getId();
        if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
        final java.lang.Object this$emailVerified = this.getEmailVerified();
        final java.lang.Object other$emailVerified = other.getEmailVerified();
        if (this$emailVerified == null ? other$emailVerified != null : !this$emailVerified.equals(other$emailVerified)) return false;
        final java.lang.Object this$email = this.getEmail();
        final java.lang.Object other$email = other.getEmail();
        if (this$email == null ? other$email != null : !this$email.equals(other$email)) return false;
        final java.lang.Object this$name = this.getName();
        final java.lang.Object other$name = other.getName();
        if (this$name == null ? other$name != null : !this$name.equals(other$name)) return false;
        final java.lang.Object this$plan = this.getPlan();
        final java.lang.Object other$plan = other.getPlan();
        if (this$plan == null ? other$plan != null : !this$plan.equals(other$plan)) return false;
        final java.lang.Object this$role = this.getRole();
        final java.lang.Object other$role = other.getRole();
        if (this$role == null ? other$role != null : !this$role.equals(other$role)) return false;
        final java.lang.Object this$createdAt = this.getCreatedAt();
        final java.lang.Object other$createdAt = other.getCreatedAt();
        if (this$createdAt == null ? other$createdAt != null : !this$createdAt.equals(other$createdAt)) return false;
        final java.lang.Object this$stripeCustomerId = this.getStripeCustomerId();
        final java.lang.Object other$stripeCustomerId = other.getStripeCustomerId();
        if (this$stripeCustomerId == null ? other$stripeCustomerId != null : !this$stripeCustomerId.equals(other$stripeCustomerId)) return false;
        final java.lang.Object this$stripeSubscriptionId = this.getStripeSubscriptionId();
        final java.lang.Object other$stripeSubscriptionId = other.getStripeSubscriptionId();
        if (this$stripeSubscriptionId == null ? other$stripeSubscriptionId != null : !this$stripeSubscriptionId.equals(other$stripeSubscriptionId)) return false;
        final java.lang.Object this$proUntil = this.getProUntil();
        final java.lang.Object other$proUntil = other.getProUntil();
        if (this$proUntil == null ? other$proUntil != null : !this$proUntil.equals(other$proUntil)) return false;
        final java.lang.Object this$subscriptionStatus = this.getSubscriptionStatus();
        final java.lang.Object other$subscriptionStatus = other.getSubscriptionStatus();
        if (this$subscriptionStatus == null ? other$subscriptionStatus != null : !this$subscriptionStatus.equals(other$subscriptionStatus)) return false;
        final java.lang.Object this$planSource = this.getPlanSource();
        final java.lang.Object other$planSource = other.getPlanSource();
        if (this$planSource == null ? other$planSource != null : !this$planSource.equals(other$planSource)) return false;
        final java.lang.Object this$usageThisMonth = this.getUsageThisMonth();
        final java.lang.Object other$usageThisMonth = other.getUsageThisMonth();
        if (this$usageThisMonth == null ? other$usageThisMonth != null : !this$usageThisMonth.equals(other$usageThisMonth)) return false;
        final java.lang.Object this$limits = this.getLimits();
        final java.lang.Object other$limits = other.getLimits();
        if (this$limits == null ? other$limits != null : !this$limits.equals(other$limits)) return false;
        final java.lang.Object this$remaining = this.getRemaining();
        final java.lang.Object other$remaining = other.getRemaining();
        if (this$remaining == null ? other$remaining != null : !this$remaining.equals(other$remaining)) return false;
        return true;
    }

    @java.lang.SuppressWarnings("all")
    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof AdminUserResponse;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final long $tradingAccountCount = this.getTradingAccountCount();
        result = result * PRIME + (int) ($tradingAccountCount >>> 32 ^ $tradingAccountCount);
        final long $tradeCount = this.getTradeCount();
        result = result * PRIME + (int) ($tradeCount >>> 32 ^ $tradeCount);
        final long $aiReviewCount = this.getAiReviewCount();
        result = result * PRIME + (int) ($aiReviewCount >>> 32 ^ $aiReviewCount);
        result = result * PRIME + this.getDailyCoachUsage();
        result = result * PRIME + this.getWeeklyCoachUsage();
        result = result * PRIME + this.getTradingDnaUsage();
        final java.lang.Object $id = this.getId();
        result = result * PRIME + ($id == null ? 43 : $id.hashCode());
        final java.lang.Object $emailVerified = this.getEmailVerified();
        result = result * PRIME + ($emailVerified == null ? 43 : $emailVerified.hashCode());
        final java.lang.Object $email = this.getEmail();
        result = result * PRIME + ($email == null ? 43 : $email.hashCode());
        final java.lang.Object $name = this.getName();
        result = result * PRIME + ($name == null ? 43 : $name.hashCode());
        final java.lang.Object $plan = this.getPlan();
        result = result * PRIME + ($plan == null ? 43 : $plan.hashCode());
        final java.lang.Object $role = this.getRole();
        result = result * PRIME + ($role == null ? 43 : $role.hashCode());
        final java.lang.Object $createdAt = this.getCreatedAt();
        result = result * PRIME + ($createdAt == null ? 43 : $createdAt.hashCode());
        final java.lang.Object $stripeCustomerId = this.getStripeCustomerId();
        result = result * PRIME + ($stripeCustomerId == null ? 43 : $stripeCustomerId.hashCode());
        final java.lang.Object $stripeSubscriptionId = this.getStripeSubscriptionId();
        result = result * PRIME + ($stripeSubscriptionId == null ? 43 : $stripeSubscriptionId.hashCode());
        final java.lang.Object $proUntil = this.getProUntil();
        result = result * PRIME + ($proUntil == null ? 43 : $proUntil.hashCode());
        final java.lang.Object $subscriptionStatus = this.getSubscriptionStatus();
        result = result * PRIME + ($subscriptionStatus == null ? 43 : $subscriptionStatus.hashCode());
        final java.lang.Object $planSource = this.getPlanSource();
        result = result * PRIME + ($planSource == null ? 43 : $planSource.hashCode());
        final java.lang.Object $usageThisMonth = this.getUsageThisMonth();
        result = result * PRIME + ($usageThisMonth == null ? 43 : $usageThisMonth.hashCode());
        final java.lang.Object $limits = this.getLimits();
        result = result * PRIME + ($limits == null ? 43 : $limits.hashCode());
        final java.lang.Object $remaining = this.getRemaining();
        result = result * PRIME + ($remaining == null ? 43 : $remaining.hashCode());
        return result;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public java.lang.String toString() {
        return "AdminUserResponse(id=" + this.getId() + ", email=" + this.getEmail() + ", name=" + this.getName() + ", plan=" + this.getPlan() + ", role=" + this.getRole() + ", createdAt=" + this.getCreatedAt() + ", emailVerified=" + this.getEmailVerified() + ", stripeCustomerId=" + this.getStripeCustomerId() + ", stripeSubscriptionId=" + this.getStripeSubscriptionId() + ", proUntil=" + this.getProUntil() + ", subscriptionStatus=" + this.getSubscriptionStatus() + ", tradingAccountCount=" + this.getTradingAccountCount() + ", tradeCount=" + this.getTradeCount() + ", aiReviewCount=" + this.getAiReviewCount() + ", dailyCoachUsage=" + this.getDailyCoachUsage() + ", weeklyCoachUsage=" + this.getWeeklyCoachUsage() + ", tradingDnaUsage=" + this.getTradingDnaUsage() + ", planSource=" + this.getPlanSource() + ", usageThisMonth=" + this.getUsageThisMonth() + ", limits=" + this.getLimits() + ", remaining=" + this.getRemaining() + ")";
    }
}
