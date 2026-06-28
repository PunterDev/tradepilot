package com.tradepilot.dto.ai;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public class WeeklyReflectionRequest {
    @NotNull
    private LocalDate startDate;
    @NotNull
    private LocalDate endDate;

    @java.lang.SuppressWarnings("all")
    public WeeklyReflectionRequest() {
    }

    @java.lang.SuppressWarnings("all")
    public LocalDate getStartDate() {
        return this.startDate;
    }

    @java.lang.SuppressWarnings("all")
    public LocalDate getEndDate() {
        return this.endDate;
    }

    @java.lang.SuppressWarnings("all")
    public void setStartDate(final LocalDate startDate) {
        this.startDate = startDate;
    }

    @java.lang.SuppressWarnings("all")
    public void setEndDate(final LocalDate endDate) {
        this.endDate = endDate;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof WeeklyReflectionRequest)) return false;
        final WeeklyReflectionRequest other = (WeeklyReflectionRequest) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$startDate = this.getStartDate();
        final java.lang.Object other$startDate = other.getStartDate();
        if (this$startDate == null ? other$startDate != null : !this$startDate.equals(other$startDate)) return false;
        final java.lang.Object this$endDate = this.getEndDate();
        final java.lang.Object other$endDate = other.getEndDate();
        if (this$endDate == null ? other$endDate != null : !this$endDate.equals(other$endDate)) return false;
        return true;
    }

    @java.lang.SuppressWarnings("all")
    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof WeeklyReflectionRequest;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $startDate = this.getStartDate();
        result = result * PRIME + ($startDate == null ? 43 : $startDate.hashCode());
        final java.lang.Object $endDate = this.getEndDate();
        result = result * PRIME + ($endDate == null ? 43 : $endDate.hashCode());
        return result;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public java.lang.String toString() {
        return "WeeklyReflectionRequest(startDate=" + this.getStartDate() + ", endDate=" + this.getEndDate() + ")";
    }
}
