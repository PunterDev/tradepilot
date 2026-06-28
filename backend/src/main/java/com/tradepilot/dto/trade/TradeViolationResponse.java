package com.tradepilot.dto.trade;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class TradeViolationResponse {
    private Long id;
    private String ruleCode;
    private String ruleTitle;
    private String category;
    private String severity;
    private String reason;
    private String source;
    private LocalDate tradeDate;
    private LocalDateTime createdAt;

    public static TradeViolationResponse of(Long id, String ruleCode, String ruleTitle, String category, String severity, String reason, String source, LocalDate tradeDate, LocalDateTime createdAt) {
        TradeViolationResponse response = new TradeViolationResponse();
        response.setId(id);
        response.setRuleCode(ruleCode);
        response.setRuleTitle(ruleTitle);
        response.setCategory(category);
        response.setSeverity(severity);
        response.setReason(reason);
        response.setSource(source);
        response.setTradeDate(tradeDate);
        response.setCreatedAt(createdAt);
        return response;
    }

    @java.lang.SuppressWarnings("all")
    public TradeViolationResponse() {
    }

    @java.lang.SuppressWarnings("all")
    public Long getId() {
        return this.id;
    }

    @java.lang.SuppressWarnings("all")
    public String getRuleCode() {
        return this.ruleCode;
    }

    @java.lang.SuppressWarnings("all")
    public String getRuleTitle() {
        return this.ruleTitle;
    }

    @java.lang.SuppressWarnings("all")
    public String getCategory() {
        return this.category;
    }

    @java.lang.SuppressWarnings("all")
    public String getSeverity() {
        return this.severity;
    }

    @java.lang.SuppressWarnings("all")
    public String getReason() {
        return this.reason;
    }

    @java.lang.SuppressWarnings("all")
    public String getSource() {
        return this.source;
    }

    @java.lang.SuppressWarnings("all")
    public LocalDate getTradeDate() {
        return this.tradeDate;
    }

    @java.lang.SuppressWarnings("all")
    public LocalDateTime getCreatedAt() {
        return this.createdAt;
    }

    @java.lang.SuppressWarnings("all")
    public void setId(final Long id) {
        this.id = id;
    }

    @java.lang.SuppressWarnings("all")
    public void setRuleCode(final String ruleCode) {
        this.ruleCode = ruleCode;
    }

    @java.lang.SuppressWarnings("all")
    public void setRuleTitle(final String ruleTitle) {
        this.ruleTitle = ruleTitle;
    }

    @java.lang.SuppressWarnings("all")
    public void setCategory(final String category) {
        this.category = category;
    }

    @java.lang.SuppressWarnings("all")
    public void setSeverity(final String severity) {
        this.severity = severity;
    }

    @java.lang.SuppressWarnings("all")
    public void setReason(final String reason) {
        this.reason = reason;
    }

    @java.lang.SuppressWarnings("all")
    public void setSource(final String source) {
        this.source = source;
    }

    @java.lang.SuppressWarnings("all")
    public void setTradeDate(final LocalDate tradeDate) {
        this.tradeDate = tradeDate;
    }

    @java.lang.SuppressWarnings("all")
    public void setCreatedAt(final LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof TradeViolationResponse)) return false;
        final TradeViolationResponse other = (TradeViolationResponse) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$id = this.getId();
        final java.lang.Object other$id = other.getId();
        if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
        final java.lang.Object this$ruleCode = this.getRuleCode();
        final java.lang.Object other$ruleCode = other.getRuleCode();
        if (this$ruleCode == null ? other$ruleCode != null : !this$ruleCode.equals(other$ruleCode)) return false;
        final java.lang.Object this$ruleTitle = this.getRuleTitle();
        final java.lang.Object other$ruleTitle = other.getRuleTitle();
        if (this$ruleTitle == null ? other$ruleTitle != null : !this$ruleTitle.equals(other$ruleTitle)) return false;
        final java.lang.Object this$category = this.getCategory();
        final java.lang.Object other$category = other.getCategory();
        if (this$category == null ? other$category != null : !this$category.equals(other$category)) return false;
        final java.lang.Object this$severity = this.getSeverity();
        final java.lang.Object other$severity = other.getSeverity();
        if (this$severity == null ? other$severity != null : !this$severity.equals(other$severity)) return false;
        final java.lang.Object this$reason = this.getReason();
        final java.lang.Object other$reason = other.getReason();
        if (this$reason == null ? other$reason != null : !this$reason.equals(other$reason)) return false;
        final java.lang.Object this$source = this.getSource();
        final java.lang.Object other$source = other.getSource();
        if (this$source == null ? other$source != null : !this$source.equals(other$source)) return false;
        final java.lang.Object this$tradeDate = this.getTradeDate();
        final java.lang.Object other$tradeDate = other.getTradeDate();
        if (this$tradeDate == null ? other$tradeDate != null : !this$tradeDate.equals(other$tradeDate)) return false;
        final java.lang.Object this$createdAt = this.getCreatedAt();
        final java.lang.Object other$createdAt = other.getCreatedAt();
        if (this$createdAt == null ? other$createdAt != null : !this$createdAt.equals(other$createdAt)) return false;
        return true;
    }

    @java.lang.SuppressWarnings("all")
    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof TradeViolationResponse;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $id = this.getId();
        result = result * PRIME + ($id == null ? 43 : $id.hashCode());
        final java.lang.Object $ruleCode = this.getRuleCode();
        result = result * PRIME + ($ruleCode == null ? 43 : $ruleCode.hashCode());
        final java.lang.Object $ruleTitle = this.getRuleTitle();
        result = result * PRIME + ($ruleTitle == null ? 43 : $ruleTitle.hashCode());
        final java.lang.Object $category = this.getCategory();
        result = result * PRIME + ($category == null ? 43 : $category.hashCode());
        final java.lang.Object $severity = this.getSeverity();
        result = result * PRIME + ($severity == null ? 43 : $severity.hashCode());
        final java.lang.Object $reason = this.getReason();
        result = result * PRIME + ($reason == null ? 43 : $reason.hashCode());
        final java.lang.Object $source = this.getSource();
        result = result * PRIME + ($source == null ? 43 : $source.hashCode());
        final java.lang.Object $tradeDate = this.getTradeDate();
        result = result * PRIME + ($tradeDate == null ? 43 : $tradeDate.hashCode());
        final java.lang.Object $createdAt = this.getCreatedAt();
        result = result * PRIME + ($createdAt == null ? 43 : $createdAt.hashCode());
        return result;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public java.lang.String toString() {
        return "TradeViolationResponse(id=" + this.getId() + ", ruleCode=" + this.getRuleCode() + ", ruleTitle=" + this.getRuleTitle() + ", category=" + this.getCategory() + ", severity=" + this.getSeverity() + ", reason=" + this.getReason() + ", source=" + this.getSource() + ", tradeDate=" + this.getTradeDate() + ", createdAt=" + this.getCreatedAt() + ")";
    }
}
