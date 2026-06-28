package com.tradepilot.retention.dto;

import com.tradepilot.billing.PlanType;
import java.time.LocalDateTime;

public class RetentionUserResponse {
    private Long userId;
    private String email;
    private String name;
    private PlanType plan;
    private LocalDateTime lastActive;
    private long activityCount30d;
    private long daysSinceActivity;
    private RetentionStatus status;

    public static RetentionUserResponse of(Long userId, String email, String name, PlanType plan, LocalDateTime lastActive, long activityCount30d, long daysSinceActivity, RetentionStatus status) {
        RetentionUserResponse response = new RetentionUserResponse();
        response.setUserId(userId);
        response.setEmail(email);
        response.setName(name);
        response.setPlan(plan);
        response.setLastActive(lastActive);
        response.setActivityCount30d(activityCount30d);
        response.setDaysSinceActivity(daysSinceActivity);
        response.setStatus(status);
        return response;
    }

    @java.lang.SuppressWarnings("all")
    public RetentionUserResponse() {
    }

    @java.lang.SuppressWarnings("all")
    public Long getUserId() {
        return this.userId;
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
    public LocalDateTime getLastActive() {
        return this.lastActive;
    }

    @java.lang.SuppressWarnings("all")
    public long getActivityCount30d() {
        return this.activityCount30d;
    }

    @java.lang.SuppressWarnings("all")
    public long getDaysSinceActivity() {
        return this.daysSinceActivity;
    }

    @java.lang.SuppressWarnings("all")
    public RetentionStatus getStatus() {
        return this.status;
    }

    @java.lang.SuppressWarnings("all")
    public void setUserId(final Long userId) {
        this.userId = userId;
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
    public void setLastActive(final LocalDateTime lastActive) {
        this.lastActive = lastActive;
    }

    @java.lang.SuppressWarnings("all")
    public void setActivityCount30d(final long activityCount30d) {
        this.activityCount30d = activityCount30d;
    }

    @java.lang.SuppressWarnings("all")
    public void setDaysSinceActivity(final long daysSinceActivity) {
        this.daysSinceActivity = daysSinceActivity;
    }

    @java.lang.SuppressWarnings("all")
    public void setStatus(final RetentionStatus status) {
        this.status = status;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof RetentionUserResponse)) return false;
        final RetentionUserResponse other = (RetentionUserResponse) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        if (this.getActivityCount30d() != other.getActivityCount30d()) return false;
        if (this.getDaysSinceActivity() != other.getDaysSinceActivity()) return false;
        final java.lang.Object this$userId = this.getUserId();
        final java.lang.Object other$userId = other.getUserId();
        if (this$userId == null ? other$userId != null : !this$userId.equals(other$userId)) return false;
        final java.lang.Object this$email = this.getEmail();
        final java.lang.Object other$email = other.getEmail();
        if (this$email == null ? other$email != null : !this$email.equals(other$email)) return false;
        final java.lang.Object this$name = this.getName();
        final java.lang.Object other$name = other.getName();
        if (this$name == null ? other$name != null : !this$name.equals(other$name)) return false;
        final java.lang.Object this$plan = this.getPlan();
        final java.lang.Object other$plan = other.getPlan();
        if (this$plan == null ? other$plan != null : !this$plan.equals(other$plan)) return false;
        final java.lang.Object this$lastActive = this.getLastActive();
        final java.lang.Object other$lastActive = other.getLastActive();
        if (this$lastActive == null ? other$lastActive != null : !this$lastActive.equals(other$lastActive)) return false;
        final java.lang.Object this$status = this.getStatus();
        final java.lang.Object other$status = other.getStatus();
        if (this$status == null ? other$status != null : !this$status.equals(other$status)) return false;
        return true;
    }

    @java.lang.SuppressWarnings("all")
    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof RetentionUserResponse;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final long $activityCount30d = this.getActivityCount30d();
        result = result * PRIME + (int) ($activityCount30d >>> 32 ^ $activityCount30d);
        final long $daysSinceActivity = this.getDaysSinceActivity();
        result = result * PRIME + (int) ($daysSinceActivity >>> 32 ^ $daysSinceActivity);
        final java.lang.Object $userId = this.getUserId();
        result = result * PRIME + ($userId == null ? 43 : $userId.hashCode());
        final java.lang.Object $email = this.getEmail();
        result = result * PRIME + ($email == null ? 43 : $email.hashCode());
        final java.lang.Object $name = this.getName();
        result = result * PRIME + ($name == null ? 43 : $name.hashCode());
        final java.lang.Object $plan = this.getPlan();
        result = result * PRIME + ($plan == null ? 43 : $plan.hashCode());
        final java.lang.Object $lastActive = this.getLastActive();
        result = result * PRIME + ($lastActive == null ? 43 : $lastActive.hashCode());
        final java.lang.Object $status = this.getStatus();
        result = result * PRIME + ($status == null ? 43 : $status.hashCode());
        return result;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public java.lang.String toString() {
        return "RetentionUserResponse(userId=" + this.getUserId() + ", email=" + this.getEmail() + ", name=" + this.getName() + ", plan=" + this.getPlan() + ", lastActive=" + this.getLastActive() + ", activityCount30d=" + this.getActivityCount30d() + ", daysSinceActivity=" + this.getDaysSinceActivity() + ", status=" + this.getStatus() + ")";
    }
}
