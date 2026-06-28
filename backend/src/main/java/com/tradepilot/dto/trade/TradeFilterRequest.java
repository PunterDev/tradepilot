package com.tradepilot.dto.trade;

import com.tradepilot.enums.*;
import java.time.LocalDate;

public class TradeFilterRequest {
    private String symbol;
    private SetupType setup;
    private SessionType session;
    private OutcomeType outcome;
    private LocalDate startDate;
    private LocalDate endDate;
    private Long accountId;

    @java.lang.SuppressWarnings("all")
    public TradeFilterRequest() {
    }

    @java.lang.SuppressWarnings("all")
    public String getSymbol() {
        return this.symbol;
    }

    @java.lang.SuppressWarnings("all")
    public SetupType getSetup() {
        return this.setup;
    }

    @java.lang.SuppressWarnings("all")
    public SessionType getSession() {
        return this.session;
    }

    @java.lang.SuppressWarnings("all")
    public OutcomeType getOutcome() {
        return this.outcome;
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
    public Long getAccountId() {
        return this.accountId;
    }

    @java.lang.SuppressWarnings("all")
    public void setSymbol(final String symbol) {
        this.symbol = symbol;
    }

    @java.lang.SuppressWarnings("all")
    public void setSetup(final SetupType setup) {
        this.setup = setup;
    }

    @java.lang.SuppressWarnings("all")
    public void setSession(final SessionType session) {
        this.session = session;
    }

    @java.lang.SuppressWarnings("all")
    public void setOutcome(final OutcomeType outcome) {
        this.outcome = outcome;
    }

    @java.lang.SuppressWarnings("all")
    public void setStartDate(final LocalDate startDate) {
        this.startDate = startDate;
    }

    @java.lang.SuppressWarnings("all")
    public void setEndDate(final LocalDate endDate) {
        this.endDate = endDate;
    }

    @java.lang.SuppressWarnings("all")
    public void setAccountId(final Long accountId) {
        this.accountId = accountId;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof TradeFilterRequest)) return false;
        final TradeFilterRequest other = (TradeFilterRequest) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$accountId = this.getAccountId();
        final java.lang.Object other$accountId = other.getAccountId();
        if (this$accountId == null ? other$accountId != null : !this$accountId.equals(other$accountId)) return false;
        final java.lang.Object this$symbol = this.getSymbol();
        final java.lang.Object other$symbol = other.getSymbol();
        if (this$symbol == null ? other$symbol != null : !this$symbol.equals(other$symbol)) return false;
        final java.lang.Object this$setup = this.getSetup();
        final java.lang.Object other$setup = other.getSetup();
        if (this$setup == null ? other$setup != null : !this$setup.equals(other$setup)) return false;
        final java.lang.Object this$session = this.getSession();
        final java.lang.Object other$session = other.getSession();
        if (this$session == null ? other$session != null : !this$session.equals(other$session)) return false;
        final java.lang.Object this$outcome = this.getOutcome();
        final java.lang.Object other$outcome = other.getOutcome();
        if (this$outcome == null ? other$outcome != null : !this$outcome.equals(other$outcome)) return false;
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
        return other instanceof TradeFilterRequest;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $accountId = this.getAccountId();
        result = result * PRIME + ($accountId == null ? 43 : $accountId.hashCode());
        final java.lang.Object $symbol = this.getSymbol();
        result = result * PRIME + ($symbol == null ? 43 : $symbol.hashCode());
        final java.lang.Object $setup = this.getSetup();
        result = result * PRIME + ($setup == null ? 43 : $setup.hashCode());
        final java.lang.Object $session = this.getSession();
        result = result * PRIME + ($session == null ? 43 : $session.hashCode());
        final java.lang.Object $outcome = this.getOutcome();
        result = result * PRIME + ($outcome == null ? 43 : $outcome.hashCode());
        final java.lang.Object $startDate = this.getStartDate();
        result = result * PRIME + ($startDate == null ? 43 : $startDate.hashCode());
        final java.lang.Object $endDate = this.getEndDate();
        result = result * PRIME + ($endDate == null ? 43 : $endDate.hashCode());
        return result;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public java.lang.String toString() {
        return "TradeFilterRequest(symbol=" + this.getSymbol() + ", setup=" + this.getSetup() + ", session=" + this.getSession() + ", outcome=" + this.getOutcome() + ", startDate=" + this.getStartDate() + ", endDate=" + this.getEndDate() + ", accountId=" + this.getAccountId() + ")";
    }
}
