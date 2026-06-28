package com.tradepilot.dto.session;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public class GenerateSessionReviewRequest {
    @NotNull
    private Long accountId;
    @NotNull
    private LocalDate date;
    private Boolean forceRefresh = false;

    @java.lang.SuppressWarnings("all")
    public GenerateSessionReviewRequest() {
    }

    @java.lang.SuppressWarnings("all")
    public Long getAccountId() {
        return this.accountId;
    }

    @java.lang.SuppressWarnings("all")
    public LocalDate getDate() {
        return this.date;
    }

    @java.lang.SuppressWarnings("all")
    public Boolean getForceRefresh() {
        return this.forceRefresh;
    }

    @java.lang.SuppressWarnings("all")
    public void setAccountId(final Long accountId) {
        this.accountId = accountId;
    }

    @java.lang.SuppressWarnings("all")
    public void setDate(final LocalDate date) {
        this.date = date;
    }

    @java.lang.SuppressWarnings("all")
    public void setForceRefresh(final Boolean forceRefresh) {
        this.forceRefresh = forceRefresh;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof GenerateSessionReviewRequest)) return false;
        final GenerateSessionReviewRequest other = (GenerateSessionReviewRequest) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$accountId = this.getAccountId();
        final java.lang.Object other$accountId = other.getAccountId();
        if (this$accountId == null ? other$accountId != null : !this$accountId.equals(other$accountId)) return false;
        final java.lang.Object this$forceRefresh = this.getForceRefresh();
        final java.lang.Object other$forceRefresh = other.getForceRefresh();
        if (this$forceRefresh == null ? other$forceRefresh != null : !this$forceRefresh.equals(other$forceRefresh)) return false;
        final java.lang.Object this$date = this.getDate();
        final java.lang.Object other$date = other.getDate();
        if (this$date == null ? other$date != null : !this$date.equals(other$date)) return false;
        return true;
    }

    @java.lang.SuppressWarnings("all")
    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof GenerateSessionReviewRequest;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $accountId = this.getAccountId();
        result = result * PRIME + ($accountId == null ? 43 : $accountId.hashCode());
        final java.lang.Object $forceRefresh = this.getForceRefresh();
        result = result * PRIME + ($forceRefresh == null ? 43 : $forceRefresh.hashCode());
        final java.lang.Object $date = this.getDate();
        result = result * PRIME + ($date == null ? 43 : $date.hashCode());
        return result;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public java.lang.String toString() {
        return "GenerateSessionReviewRequest(accountId=" + this.getAccountId() + ", date=" + this.getDate() + ", forceRefresh=" + this.getForceRefresh() + ")";
    }
}
