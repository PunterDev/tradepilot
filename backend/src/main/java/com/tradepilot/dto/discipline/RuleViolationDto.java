package com.tradepilot.dto.discipline;


public class RuleViolationDto {
    private Long id;
    private Long tradeId;
    private String ruleCode;
    private String ruleTitle;
    private String category;
    private String severity;
    private String reason;
    private String source;


    @java.lang.SuppressWarnings("all")
    public static class RuleViolationDtoBuilder {
        @java.lang.SuppressWarnings("all")
        private Long id;
        @java.lang.SuppressWarnings("all")
        private Long tradeId;
        @java.lang.SuppressWarnings("all")
        private String ruleCode;
        @java.lang.SuppressWarnings("all")
        private String ruleTitle;
        @java.lang.SuppressWarnings("all")
        private String category;
        @java.lang.SuppressWarnings("all")
        private String severity;
        @java.lang.SuppressWarnings("all")
        private String reason;
        @java.lang.SuppressWarnings("all")
        private String source;

        @java.lang.SuppressWarnings("all")
        RuleViolationDtoBuilder() {
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public RuleViolationDto.RuleViolationDtoBuilder id(final Long id) {
            this.id = id;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public RuleViolationDto.RuleViolationDtoBuilder tradeId(final Long tradeId) {
            this.tradeId = tradeId;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public RuleViolationDto.RuleViolationDtoBuilder ruleCode(final String ruleCode) {
            this.ruleCode = ruleCode;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public RuleViolationDto.RuleViolationDtoBuilder ruleTitle(final String ruleTitle) {
            this.ruleTitle = ruleTitle;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public RuleViolationDto.RuleViolationDtoBuilder category(final String category) {
            this.category = category;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public RuleViolationDto.RuleViolationDtoBuilder severity(final String severity) {
            this.severity = severity;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public RuleViolationDto.RuleViolationDtoBuilder reason(final String reason) {
            this.reason = reason;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public RuleViolationDto.RuleViolationDtoBuilder source(final String source) {
            this.source = source;
            return this;
        }

        @java.lang.SuppressWarnings("all")
        public RuleViolationDto build() {
            return new RuleViolationDto(this.id, this.tradeId, this.ruleCode, this.ruleTitle, this.category, this.severity, this.reason, this.source);
        }

        @java.lang.Override
        @java.lang.SuppressWarnings("all")
        public java.lang.String toString() {
            return "RuleViolationDto.RuleViolationDtoBuilder(id=" + this.id + ", tradeId=" + this.tradeId + ", ruleCode=" + this.ruleCode + ", ruleTitle=" + this.ruleTitle + ", category=" + this.category + ", severity=" + this.severity + ", reason=" + this.reason + ", source=" + this.source + ")";
        }
    }

    @java.lang.SuppressWarnings("all")
    public static RuleViolationDto.RuleViolationDtoBuilder builder() {
        return new RuleViolationDto.RuleViolationDtoBuilder();
    }

    @java.lang.SuppressWarnings("all")
    public Long getId() {
        return this.id;
    }

    @java.lang.SuppressWarnings("all")
    public Long getTradeId() {
        return this.tradeId;
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
    public void setId(final Long id) {
        this.id = id;
    }

    @java.lang.SuppressWarnings("all")
    public void setTradeId(final Long tradeId) {
        this.tradeId = tradeId;
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

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof RuleViolationDto)) return false;
        final RuleViolationDto other = (RuleViolationDto) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$id = this.getId();
        final java.lang.Object other$id = other.getId();
        if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
        final java.lang.Object this$tradeId = this.getTradeId();
        final java.lang.Object other$tradeId = other.getTradeId();
        if (this$tradeId == null ? other$tradeId != null : !this$tradeId.equals(other$tradeId)) return false;
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
        return true;
    }

    @java.lang.SuppressWarnings("all")
    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof RuleViolationDto;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $id = this.getId();
        result = result * PRIME + ($id == null ? 43 : $id.hashCode());
        final java.lang.Object $tradeId = this.getTradeId();
        result = result * PRIME + ($tradeId == null ? 43 : $tradeId.hashCode());
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
        return result;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public java.lang.String toString() {
        return "RuleViolationDto(id=" + this.getId() + ", tradeId=" + this.getTradeId() + ", ruleCode=" + this.getRuleCode() + ", ruleTitle=" + this.getRuleTitle() + ", category=" + this.getCategory() + ", severity=" + this.getSeverity() + ", reason=" + this.getReason() + ", source=" + this.getSource() + ")";
    }

    @java.lang.SuppressWarnings("all")
    public RuleViolationDto() {
    }

    @java.lang.SuppressWarnings("all")
    public RuleViolationDto(final Long id, final Long tradeId, final String ruleCode, final String ruleTitle, final String category, final String severity, final String reason, final String source) {
        this.id = id;
        this.tradeId = tradeId;
        this.ruleCode = ruleCode;
        this.ruleTitle = ruleTitle;
        this.category = category;
        this.severity = severity;
        this.reason = reason;
        this.source = source;
    }
}
