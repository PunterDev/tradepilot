package com.tradepilot.dto.ai;

import java.util.List;

public class AiTradeReviewResponse {
    private String summary;
    private List<String> mistakes;
    private List<String> strengths;
    private List<String> improvementPlan;
    private List<String> tags;
    private String grade;

    @java.lang.SuppressWarnings("all")
    AiTradeReviewResponse(final String summary, final List<String> mistakes, final List<String> strengths, final List<String> improvementPlan, final List<String> tags, final String grade) {
        this.summary = summary;
        this.mistakes = mistakes;
        this.strengths = strengths;
        this.improvementPlan = improvementPlan;
        this.tags = tags;
        this.grade = grade;
    }


    @java.lang.SuppressWarnings("all")
    public static class AiTradeReviewResponseBuilder {
        @java.lang.SuppressWarnings("all")
        private String summary;
        @java.lang.SuppressWarnings("all")
        private List<String> mistakes;
        @java.lang.SuppressWarnings("all")
        private List<String> strengths;
        @java.lang.SuppressWarnings("all")
        private List<String> improvementPlan;
        @java.lang.SuppressWarnings("all")
        private List<String> tags;
        @java.lang.SuppressWarnings("all")
        private String grade;

        @java.lang.SuppressWarnings("all")
        AiTradeReviewResponseBuilder() {
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public AiTradeReviewResponse.AiTradeReviewResponseBuilder summary(final String summary) {
            this.summary = summary;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public AiTradeReviewResponse.AiTradeReviewResponseBuilder mistakes(final List<String> mistakes) {
            this.mistakes = mistakes;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public AiTradeReviewResponse.AiTradeReviewResponseBuilder strengths(final List<String> strengths) {
            this.strengths = strengths;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public AiTradeReviewResponse.AiTradeReviewResponseBuilder improvementPlan(final List<String> improvementPlan) {
            this.improvementPlan = improvementPlan;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public AiTradeReviewResponse.AiTradeReviewResponseBuilder tags(final List<String> tags) {
            this.tags = tags;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public AiTradeReviewResponse.AiTradeReviewResponseBuilder grade(final String grade) {
            this.grade = grade;
            return this;
        }

        @java.lang.SuppressWarnings("all")
        public AiTradeReviewResponse build() {
            return new AiTradeReviewResponse(this.summary, this.mistakes, this.strengths, this.improvementPlan, this.tags, this.grade);
        }

        @java.lang.Override
        @java.lang.SuppressWarnings("all")
        public java.lang.String toString() {
            return "AiTradeReviewResponse.AiTradeReviewResponseBuilder(summary=" + this.summary + ", mistakes=" + this.mistakes + ", strengths=" + this.strengths + ", improvementPlan=" + this.improvementPlan + ", tags=" + this.tags + ", grade=" + this.grade + ")";
        }
    }

    @java.lang.SuppressWarnings("all")
    public static AiTradeReviewResponse.AiTradeReviewResponseBuilder builder() {
        return new AiTradeReviewResponse.AiTradeReviewResponseBuilder();
    }

    @java.lang.SuppressWarnings("all")
    public String getSummary() {
        return this.summary;
    }

    @java.lang.SuppressWarnings("all")
    public List<String> getMistakes() {
        return this.mistakes;
    }

    @java.lang.SuppressWarnings("all")
    public List<String> getStrengths() {
        return this.strengths;
    }

    @java.lang.SuppressWarnings("all")
    public List<String> getImprovementPlan() {
        return this.improvementPlan;
    }

    @java.lang.SuppressWarnings("all")
    public List<String> getTags() {
        return this.tags;
    }

    @java.lang.SuppressWarnings("all")
    public String getGrade() {
        return this.grade;
    }

    @java.lang.SuppressWarnings("all")
    public void setSummary(final String summary) {
        this.summary = summary;
    }

    @java.lang.SuppressWarnings("all")
    public void setMistakes(final List<String> mistakes) {
        this.mistakes = mistakes;
    }

    @java.lang.SuppressWarnings("all")
    public void setStrengths(final List<String> strengths) {
        this.strengths = strengths;
    }

    @java.lang.SuppressWarnings("all")
    public void setImprovementPlan(final List<String> improvementPlan) {
        this.improvementPlan = improvementPlan;
    }

    @java.lang.SuppressWarnings("all")
    public void setTags(final List<String> tags) {
        this.tags = tags;
    }

    @java.lang.SuppressWarnings("all")
    public void setGrade(final String grade) {
        this.grade = grade;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof AiTradeReviewResponse)) return false;
        final AiTradeReviewResponse other = (AiTradeReviewResponse) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$summary = this.getSummary();
        final java.lang.Object other$summary = other.getSummary();
        if (this$summary == null ? other$summary != null : !this$summary.equals(other$summary)) return false;
        final java.lang.Object this$mistakes = this.getMistakes();
        final java.lang.Object other$mistakes = other.getMistakes();
        if (this$mistakes == null ? other$mistakes != null : !this$mistakes.equals(other$mistakes)) return false;
        final java.lang.Object this$strengths = this.getStrengths();
        final java.lang.Object other$strengths = other.getStrengths();
        if (this$strengths == null ? other$strengths != null : !this$strengths.equals(other$strengths)) return false;
        final java.lang.Object this$improvementPlan = this.getImprovementPlan();
        final java.lang.Object other$improvementPlan = other.getImprovementPlan();
        if (this$improvementPlan == null ? other$improvementPlan != null : !this$improvementPlan.equals(other$improvementPlan)) return false;
        final java.lang.Object this$tags = this.getTags();
        final java.lang.Object other$tags = other.getTags();
        if (this$tags == null ? other$tags != null : !this$tags.equals(other$tags)) return false;
        final java.lang.Object this$grade = this.getGrade();
        final java.lang.Object other$grade = other.getGrade();
        if (this$grade == null ? other$grade != null : !this$grade.equals(other$grade)) return false;
        return true;
    }

    @java.lang.SuppressWarnings("all")
    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof AiTradeReviewResponse;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $summary = this.getSummary();
        result = result * PRIME + ($summary == null ? 43 : $summary.hashCode());
        final java.lang.Object $mistakes = this.getMistakes();
        result = result * PRIME + ($mistakes == null ? 43 : $mistakes.hashCode());
        final java.lang.Object $strengths = this.getStrengths();
        result = result * PRIME + ($strengths == null ? 43 : $strengths.hashCode());
        final java.lang.Object $improvementPlan = this.getImprovementPlan();
        result = result * PRIME + ($improvementPlan == null ? 43 : $improvementPlan.hashCode());
        final java.lang.Object $tags = this.getTags();
        result = result * PRIME + ($tags == null ? 43 : $tags.hashCode());
        final java.lang.Object $grade = this.getGrade();
        result = result * PRIME + ($grade == null ? 43 : $grade.hashCode());
        return result;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public java.lang.String toString() {
        return "AiTradeReviewResponse(summary=" + this.getSummary() + ", mistakes=" + this.getMistakes() + ", strengths=" + this.getStrengths() + ", improvementPlan=" + this.getImprovementPlan() + ", tags=" + this.getTags() + ", grade=" + this.getGrade() + ")";
    }
}
