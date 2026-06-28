package com.tradepilot.dto.discipline;

import java.util.List;

public class DisciplineEvaluationResponse {
    private Long tradeId;
    private String tradeDate;
    private Integer disciplineScore;
    private Boolean cleanTrade;
    private Integer violationCount;
    private List<RuleViolationDto> violations;


    @java.lang.SuppressWarnings("all")
    public static class DisciplineEvaluationResponseBuilder {
        @java.lang.SuppressWarnings("all")
        private Long tradeId;
        @java.lang.SuppressWarnings("all")
        private String tradeDate;
        @java.lang.SuppressWarnings("all")
        private Integer disciplineScore;
        @java.lang.SuppressWarnings("all")
        private Boolean cleanTrade;
        @java.lang.SuppressWarnings("all")
        private Integer violationCount;
        @java.lang.SuppressWarnings("all")
        private List<RuleViolationDto> violations;

        @java.lang.SuppressWarnings("all")
        DisciplineEvaluationResponseBuilder() {
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public DisciplineEvaluationResponse.DisciplineEvaluationResponseBuilder tradeId(final Long tradeId) {
            this.tradeId = tradeId;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public DisciplineEvaluationResponse.DisciplineEvaluationResponseBuilder tradeDate(final String tradeDate) {
            this.tradeDate = tradeDate;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public DisciplineEvaluationResponse.DisciplineEvaluationResponseBuilder disciplineScore(final Integer disciplineScore) {
            this.disciplineScore = disciplineScore;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public DisciplineEvaluationResponse.DisciplineEvaluationResponseBuilder cleanTrade(final Boolean cleanTrade) {
            this.cleanTrade = cleanTrade;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public DisciplineEvaluationResponse.DisciplineEvaluationResponseBuilder violationCount(final Integer violationCount) {
            this.violationCount = violationCount;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public DisciplineEvaluationResponse.DisciplineEvaluationResponseBuilder violations(final List<RuleViolationDto> violations) {
            this.violations = violations;
            return this;
        }

        @java.lang.SuppressWarnings("all")
        public DisciplineEvaluationResponse build() {
            return new DisciplineEvaluationResponse(this.tradeId, this.tradeDate, this.disciplineScore, this.cleanTrade, this.violationCount, this.violations);
        }

        @java.lang.Override
        @java.lang.SuppressWarnings("all")
        public java.lang.String toString() {
            return "DisciplineEvaluationResponse.DisciplineEvaluationResponseBuilder(tradeId=" + this.tradeId + ", tradeDate=" + this.tradeDate + ", disciplineScore=" + this.disciplineScore + ", cleanTrade=" + this.cleanTrade + ", violationCount=" + this.violationCount + ", violations=" + this.violations + ")";
        }
    }

    @java.lang.SuppressWarnings("all")
    public static DisciplineEvaluationResponse.DisciplineEvaluationResponseBuilder builder() {
        return new DisciplineEvaluationResponse.DisciplineEvaluationResponseBuilder();
    }

    @java.lang.SuppressWarnings("all")
    public Long getTradeId() {
        return this.tradeId;
    }

    @java.lang.SuppressWarnings("all")
    public String getTradeDate() {
        return this.tradeDate;
    }

    @java.lang.SuppressWarnings("all")
    public Integer getDisciplineScore() {
        return this.disciplineScore;
    }

    @java.lang.SuppressWarnings("all")
    public Boolean getCleanTrade() {
        return this.cleanTrade;
    }

    @java.lang.SuppressWarnings("all")
    public Integer getViolationCount() {
        return this.violationCount;
    }

    @java.lang.SuppressWarnings("all")
    public List<RuleViolationDto> getViolations() {
        return this.violations;
    }

    @java.lang.SuppressWarnings("all")
    public void setTradeId(final Long tradeId) {
        this.tradeId = tradeId;
    }

    @java.lang.SuppressWarnings("all")
    public void setTradeDate(final String tradeDate) {
        this.tradeDate = tradeDate;
    }

    @java.lang.SuppressWarnings("all")
    public void setDisciplineScore(final Integer disciplineScore) {
        this.disciplineScore = disciplineScore;
    }

    @java.lang.SuppressWarnings("all")
    public void setCleanTrade(final Boolean cleanTrade) {
        this.cleanTrade = cleanTrade;
    }

    @java.lang.SuppressWarnings("all")
    public void setViolationCount(final Integer violationCount) {
        this.violationCount = violationCount;
    }

    @java.lang.SuppressWarnings("all")
    public void setViolations(final List<RuleViolationDto> violations) {
        this.violations = violations;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof DisciplineEvaluationResponse)) return false;
        final DisciplineEvaluationResponse other = (DisciplineEvaluationResponse) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$tradeId = this.getTradeId();
        final java.lang.Object other$tradeId = other.getTradeId();
        if (this$tradeId == null ? other$tradeId != null : !this$tradeId.equals(other$tradeId)) return false;
        final java.lang.Object this$disciplineScore = this.getDisciplineScore();
        final java.lang.Object other$disciplineScore = other.getDisciplineScore();
        if (this$disciplineScore == null ? other$disciplineScore != null : !this$disciplineScore.equals(other$disciplineScore)) return false;
        final java.lang.Object this$cleanTrade = this.getCleanTrade();
        final java.lang.Object other$cleanTrade = other.getCleanTrade();
        if (this$cleanTrade == null ? other$cleanTrade != null : !this$cleanTrade.equals(other$cleanTrade)) return false;
        final java.lang.Object this$violationCount = this.getViolationCount();
        final java.lang.Object other$violationCount = other.getViolationCount();
        if (this$violationCount == null ? other$violationCount != null : !this$violationCount.equals(other$violationCount)) return false;
        final java.lang.Object this$tradeDate = this.getTradeDate();
        final java.lang.Object other$tradeDate = other.getTradeDate();
        if (this$tradeDate == null ? other$tradeDate != null : !this$tradeDate.equals(other$tradeDate)) return false;
        final java.lang.Object this$violations = this.getViolations();
        final java.lang.Object other$violations = other.getViolations();
        if (this$violations == null ? other$violations != null : !this$violations.equals(other$violations)) return false;
        return true;
    }

    @java.lang.SuppressWarnings("all")
    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof DisciplineEvaluationResponse;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $tradeId = this.getTradeId();
        result = result * PRIME + ($tradeId == null ? 43 : $tradeId.hashCode());
        final java.lang.Object $disciplineScore = this.getDisciplineScore();
        result = result * PRIME + ($disciplineScore == null ? 43 : $disciplineScore.hashCode());
        final java.lang.Object $cleanTrade = this.getCleanTrade();
        result = result * PRIME + ($cleanTrade == null ? 43 : $cleanTrade.hashCode());
        final java.lang.Object $violationCount = this.getViolationCount();
        result = result * PRIME + ($violationCount == null ? 43 : $violationCount.hashCode());
        final java.lang.Object $tradeDate = this.getTradeDate();
        result = result * PRIME + ($tradeDate == null ? 43 : $tradeDate.hashCode());
        final java.lang.Object $violations = this.getViolations();
        result = result * PRIME + ($violations == null ? 43 : $violations.hashCode());
        return result;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public java.lang.String toString() {
        return "DisciplineEvaluationResponse(tradeId=" + this.getTradeId() + ", tradeDate=" + this.getTradeDate() + ", disciplineScore=" + this.getDisciplineScore() + ", cleanTrade=" + this.getCleanTrade() + ", violationCount=" + this.getViolationCount() + ", violations=" + this.getViolations() + ")";
    }

    @java.lang.SuppressWarnings("all")
    public DisciplineEvaluationResponse() {
    }

    @java.lang.SuppressWarnings("all")
    public DisciplineEvaluationResponse(final Long tradeId, final String tradeDate, final Integer disciplineScore, final Boolean cleanTrade, final Integer violationCount, final List<RuleViolationDto> violations) {
        this.tradeId = tradeId;
        this.tradeDate = tradeDate;
        this.disciplineScore = disciplineScore;
        this.cleanTrade = cleanTrade;
        this.violationCount = violationCount;
        this.violations = violations;
    }
}
