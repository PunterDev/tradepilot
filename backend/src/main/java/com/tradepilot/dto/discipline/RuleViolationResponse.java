package com.tradepilot.dto.discipline;

public class RuleViolationResponse {
    private String ruleCode;
    private String ruleTitle;
    private String severity;
    private String reason;
    private String source;

    @java.lang.SuppressWarnings("all")
    RuleViolationResponse(final String ruleCode, final String ruleTitle, final String severity, final String reason, final String source) {
        this.ruleCode = ruleCode;
        this.ruleTitle = ruleTitle;
        this.severity = severity;
        this.reason = reason;
        this.source = source;
    }


    @java.lang.SuppressWarnings("all")
    public static class RuleViolationResponseBuilder {
        @java.lang.SuppressWarnings("all")
        private String ruleCode;
        @java.lang.SuppressWarnings("all")
        private String ruleTitle;
        @java.lang.SuppressWarnings("all")
        private String severity;
        @java.lang.SuppressWarnings("all")
        private String reason;
        @java.lang.SuppressWarnings("all")
        private String source;

        @java.lang.SuppressWarnings("all")
        RuleViolationResponseBuilder() {
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public RuleViolationResponse.RuleViolationResponseBuilder ruleCode(final String ruleCode) {
            this.ruleCode = ruleCode;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public RuleViolationResponse.RuleViolationResponseBuilder ruleTitle(final String ruleTitle) {
            this.ruleTitle = ruleTitle;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public RuleViolationResponse.RuleViolationResponseBuilder severity(final String severity) {
            this.severity = severity;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public RuleViolationResponse.RuleViolationResponseBuilder reason(final String reason) {
            this.reason = reason;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public RuleViolationResponse.RuleViolationResponseBuilder source(final String source) {
            this.source = source;
            return this;
        }

        @java.lang.SuppressWarnings("all")
        public RuleViolationResponse build() {
            return new RuleViolationResponse(this.ruleCode, this.ruleTitle, this.severity, this.reason, this.source);
        }

        @java.lang.Override
        @java.lang.SuppressWarnings("all")
        public java.lang.String toString() {
            return "RuleViolationResponse.RuleViolationResponseBuilder(ruleCode=" + this.ruleCode + ", ruleTitle=" + this.ruleTitle + ", severity=" + this.severity + ", reason=" + this.reason + ", source=" + this.source + ")";
        }
    }

    @java.lang.SuppressWarnings("all")
    public static RuleViolationResponse.RuleViolationResponseBuilder builder() {
        return new RuleViolationResponse.RuleViolationResponseBuilder();
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
    public void setRuleCode(final String ruleCode) {
        this.ruleCode = ruleCode;
    }

    @java.lang.SuppressWarnings("all")
    public void setRuleTitle(final String ruleTitle) {
        this.ruleTitle = ruleTitle;
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

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof RuleViolationResponse)) return false;
        final RuleViolationResponse other = (RuleViolationResponse) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$ruleCode = this.getRuleCode();
        final java.lang.Object other$ruleCode = other.getRuleCode();
        if (this$ruleCode == null ? other$ruleCode != null : !this$ruleCode.equals(other$ruleCode)) return false;
        final java.lang.Object this$ruleTitle = this.getRuleTitle();
        final java.lang.Object other$ruleTitle = other.getRuleTitle();
        if (this$ruleTitle == null ? other$ruleTitle != null : !this$ruleTitle.equals(other$ruleTitle)) return false;
        final java.lang.Object this$severity = this.getSeverity();
        final java.lang.Object other$severity = other.getSeverity();
        if (this$severity == null ? other$severity != null : !this$severity.equals(other$severity)) return false;
        final java.lang.Object this$reason = this.getReason();
        final java.lang.Object other$reason = other.getReason();
        if (this$reason == null ? other$reason != null : !this$reason.equals(other$reason)) return false;
        final java.lang.Object this$source = this.getSource();
        final java.lang.Object other$source = other.getSource();
        if (this$source == null ? other$source != null : !this$source.equals(other$source)) return false;
        return true;
    }

    @java.lang.SuppressWarnings("all")
    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof RuleViolationResponse;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $ruleCode = this.getRuleCode();
        result = result * PRIME + ($ruleCode == null ? 43 : $ruleCode.hashCode());
        final java.lang.Object $ruleTitle = this.getRuleTitle();
        result = result * PRIME + ($ruleTitle == null ? 43 : $ruleTitle.hashCode());
        final java.lang.Object $severity = this.getSeverity();
        result = result * PRIME + ($severity == null ? 43 : $severity.hashCode());
        final java.lang.Object $reason = this.getReason();
        result = result * PRIME + ($reason == null ? 43 : $reason.hashCode());
        final java.lang.Object $source = this.getSource();
        result = result * PRIME + ($source == null ? 43 : $source.hashCode());
        return result;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public java.lang.String toString() {
        return "RuleViolationResponse(ruleCode=" + this.getRuleCode() + ", ruleTitle=" + this.getRuleTitle() + ", severity=" + this.getSeverity() + ", reason=" + this.getReason() + ", source=" + this.getSource() + ")";
    }
}
