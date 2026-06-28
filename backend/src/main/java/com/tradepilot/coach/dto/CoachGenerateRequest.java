package com.tradepilot.coach.dto;

import java.time.LocalDate;

public class CoachGenerateRequest {
    private Long accountId;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private Boolean forceRefresh = false;

    @java.lang.SuppressWarnings("all")
    public Long getAccountId() {
        return this.accountId;
    }

    @java.lang.SuppressWarnings("all")
    public LocalDate getPeriodStart() {
        return this.periodStart;
    }

    @java.lang.SuppressWarnings("all")
    public LocalDate getPeriodEnd() {
        return this.periodEnd;
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
    public void setPeriodStart(final LocalDate periodStart) {
        this.periodStart = periodStart;
    }

    @java.lang.SuppressWarnings("all")
    public void setPeriodEnd(final LocalDate periodEnd) {
        this.periodEnd = periodEnd;
    }

    @java.lang.SuppressWarnings("all")
    public void setForceRefresh(final Boolean forceRefresh) {
        this.forceRefresh = forceRefresh;
    }
}
