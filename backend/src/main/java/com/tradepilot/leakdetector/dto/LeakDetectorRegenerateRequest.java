package com.tradepilot.leakdetector.dto;

import jakarta.validation.constraints.NotNull;

public class LeakDetectorRegenerateRequest {
    @NotNull
    private Long accountId;

    @java.lang.SuppressWarnings("all")
    public LeakDetectorRegenerateRequest() {
    }

    @java.lang.SuppressWarnings("all")
    public Long getAccountId() {
        return this.accountId;
    }

    @java.lang.SuppressWarnings("all")
    public void setAccountId(final Long accountId) {
        this.accountId = accountId;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof LeakDetectorRegenerateRequest)) return false;
        final LeakDetectorRegenerateRequest other = (LeakDetectorRegenerateRequest) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$accountId = this.getAccountId();
        final java.lang.Object other$accountId = other.getAccountId();
        if (this$accountId == null ? other$accountId != null : !this$accountId.equals(other$accountId)) return false;
        return true;
    }

    @java.lang.SuppressWarnings("all")
    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof LeakDetectorRegenerateRequest;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $accountId = this.getAccountId();
        result = result * PRIME + ($accountId == null ? 43 : $accountId.hashCode());
        return result;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public java.lang.String toString() {
        return "LeakDetectorRegenerateRequest(accountId=" + this.getAccountId() + ")";
    }
}
