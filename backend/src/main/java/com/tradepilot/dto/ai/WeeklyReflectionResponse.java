package com.tradepilot.dto.ai;

import java.util.List;

public class WeeklyReflectionResponse {
    private Long reflectionId;
    private String summary;
    private List<String> strengths;
    private List<String> mistakes;
    private List<String> psychologyPatterns;
    private List<String> actionItems;
    private Integer coachingScore;
    private String rawResponseText;

    @java.lang.SuppressWarnings("all")
    WeeklyReflectionResponse(final Long reflectionId, final String summary, final List<String> strengths, final List<String> mistakes, final List<String> psychologyPatterns, final List<String> actionItems, final Integer coachingScore, final String rawResponseText) {
        this.reflectionId = reflectionId;
        this.summary = summary;
        this.strengths = strengths;
        this.mistakes = mistakes;
        this.psychologyPatterns = psychologyPatterns;
        this.actionItems = actionItems;
        this.coachingScore = coachingScore;
        this.rawResponseText = rawResponseText;
    }


    @java.lang.SuppressWarnings("all")
    public static class WeeklyReflectionResponseBuilder {
        @java.lang.SuppressWarnings("all")
        private Long reflectionId;
        @java.lang.SuppressWarnings("all")
        private String summary;
        @java.lang.SuppressWarnings("all")
        private List<String> strengths;
        @java.lang.SuppressWarnings("all")
        private List<String> mistakes;
        @java.lang.SuppressWarnings("all")
        private List<String> psychologyPatterns;
        @java.lang.SuppressWarnings("all")
        private List<String> actionItems;
        @java.lang.SuppressWarnings("all")
        private Integer coachingScore;
        @java.lang.SuppressWarnings("all")
        private String rawResponseText;

        @java.lang.SuppressWarnings("all")
        WeeklyReflectionResponseBuilder() {
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public WeeklyReflectionResponse.WeeklyReflectionResponseBuilder reflectionId(final Long reflectionId) {
            this.reflectionId = reflectionId;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public WeeklyReflectionResponse.WeeklyReflectionResponseBuilder summary(final String summary) {
            this.summary = summary;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public WeeklyReflectionResponse.WeeklyReflectionResponseBuilder strengths(final List<String> strengths) {
            this.strengths = strengths;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public WeeklyReflectionResponse.WeeklyReflectionResponseBuilder mistakes(final List<String> mistakes) {
            this.mistakes = mistakes;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public WeeklyReflectionResponse.WeeklyReflectionResponseBuilder psychologyPatterns(final List<String> psychologyPatterns) {
            this.psychologyPatterns = psychologyPatterns;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public WeeklyReflectionResponse.WeeklyReflectionResponseBuilder actionItems(final List<String> actionItems) {
            this.actionItems = actionItems;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public WeeklyReflectionResponse.WeeklyReflectionResponseBuilder coachingScore(final Integer coachingScore) {
            this.coachingScore = coachingScore;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public WeeklyReflectionResponse.WeeklyReflectionResponseBuilder rawResponseText(final String rawResponseText) {
            this.rawResponseText = rawResponseText;
            return this;
        }

        @java.lang.SuppressWarnings("all")
        public WeeklyReflectionResponse build() {
            return new WeeklyReflectionResponse(this.reflectionId, this.summary, this.strengths, this.mistakes, this.psychologyPatterns, this.actionItems, this.coachingScore, this.rawResponseText);
        }

        @java.lang.Override
        @java.lang.SuppressWarnings("all")
        public java.lang.String toString() {
            return "WeeklyReflectionResponse.WeeklyReflectionResponseBuilder(reflectionId=" + this.reflectionId + ", summary=" + this.summary + ", strengths=" + this.strengths + ", mistakes=" + this.mistakes + ", psychologyPatterns=" + this.psychologyPatterns + ", actionItems=" + this.actionItems + ", coachingScore=" + this.coachingScore + ", rawResponseText=" + this.rawResponseText + ")";
        }
    }

    @java.lang.SuppressWarnings("all")
    public static WeeklyReflectionResponse.WeeklyReflectionResponseBuilder builder() {
        return new WeeklyReflectionResponse.WeeklyReflectionResponseBuilder();
    }

    @java.lang.SuppressWarnings("all")
    public Long getReflectionId() {
        return this.reflectionId;
    }

    @java.lang.SuppressWarnings("all")
    public String getSummary() {
        return this.summary;
    }

    @java.lang.SuppressWarnings("all")
    public List<String> getStrengths() {
        return this.strengths;
    }

    @java.lang.SuppressWarnings("all")
    public List<String> getMistakes() {
        return this.mistakes;
    }

    @java.lang.SuppressWarnings("all")
    public List<String> getPsychologyPatterns() {
        return this.psychologyPatterns;
    }

    @java.lang.SuppressWarnings("all")
    public List<String> getActionItems() {
        return this.actionItems;
    }

    @java.lang.SuppressWarnings("all")
    public Integer getCoachingScore() {
        return this.coachingScore;
    }

    @java.lang.SuppressWarnings("all")
    public String getRawResponseText() {
        return this.rawResponseText;
    }

    @java.lang.SuppressWarnings("all")
    public void setReflectionId(final Long reflectionId) {
        this.reflectionId = reflectionId;
    }

    @java.lang.SuppressWarnings("all")
    public void setSummary(final String summary) {
        this.summary = summary;
    }

    @java.lang.SuppressWarnings("all")
    public void setStrengths(final List<String> strengths) {
        this.strengths = strengths;
    }

    @java.lang.SuppressWarnings("all")
    public void setMistakes(final List<String> mistakes) {
        this.mistakes = mistakes;
    }

    @java.lang.SuppressWarnings("all")
    public void setPsychologyPatterns(final List<String> psychologyPatterns) {
        this.psychologyPatterns = psychologyPatterns;
    }

    @java.lang.SuppressWarnings("all")
    public void setActionItems(final List<String> actionItems) {
        this.actionItems = actionItems;
    }

    @java.lang.SuppressWarnings("all")
    public void setCoachingScore(final Integer coachingScore) {
        this.coachingScore = coachingScore;
    }

    @java.lang.SuppressWarnings("all")
    public void setRawResponseText(final String rawResponseText) {
        this.rawResponseText = rawResponseText;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof WeeklyReflectionResponse)) return false;
        final WeeklyReflectionResponse other = (WeeklyReflectionResponse) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$reflectionId = this.getReflectionId();
        final java.lang.Object other$reflectionId = other.getReflectionId();
        if (this$reflectionId == null ? other$reflectionId != null : !this$reflectionId.equals(other$reflectionId)) return false;
        final java.lang.Object this$coachingScore = this.getCoachingScore();
        final java.lang.Object other$coachingScore = other.getCoachingScore();
        if (this$coachingScore == null ? other$coachingScore != null : !this$coachingScore.equals(other$coachingScore)) return false;
        final java.lang.Object this$summary = this.getSummary();
        final java.lang.Object other$summary = other.getSummary();
        if (this$summary == null ? other$summary != null : !this$summary.equals(other$summary)) return false;
        final java.lang.Object this$strengths = this.getStrengths();
        final java.lang.Object other$strengths = other.getStrengths();
        if (this$strengths == null ? other$strengths != null : !this$strengths.equals(other$strengths)) return false;
        final java.lang.Object this$mistakes = this.getMistakes();
        final java.lang.Object other$mistakes = other.getMistakes();
        if (this$mistakes == null ? other$mistakes != null : !this$mistakes.equals(other$mistakes)) return false;
        final java.lang.Object this$psychologyPatterns = this.getPsychologyPatterns();
        final java.lang.Object other$psychologyPatterns = other.getPsychologyPatterns();
        if (this$psychologyPatterns == null ? other$psychologyPatterns != null : !this$psychologyPatterns.equals(other$psychologyPatterns)) return false;
        final java.lang.Object this$actionItems = this.getActionItems();
        final java.lang.Object other$actionItems = other.getActionItems();
        if (this$actionItems == null ? other$actionItems != null : !this$actionItems.equals(other$actionItems)) return false;
        final java.lang.Object this$rawResponseText = this.getRawResponseText();
        final java.lang.Object other$rawResponseText = other.getRawResponseText();
        if (this$rawResponseText == null ? other$rawResponseText != null : !this$rawResponseText.equals(other$rawResponseText)) return false;
        return true;
    }

    @java.lang.SuppressWarnings("all")
    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof WeeklyReflectionResponse;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $reflectionId = this.getReflectionId();
        result = result * PRIME + ($reflectionId == null ? 43 : $reflectionId.hashCode());
        final java.lang.Object $coachingScore = this.getCoachingScore();
        result = result * PRIME + ($coachingScore == null ? 43 : $coachingScore.hashCode());
        final java.lang.Object $summary = this.getSummary();
        result = result * PRIME + ($summary == null ? 43 : $summary.hashCode());
        final java.lang.Object $strengths = this.getStrengths();
        result = result * PRIME + ($strengths == null ? 43 : $strengths.hashCode());
        final java.lang.Object $mistakes = this.getMistakes();
        result = result * PRIME + ($mistakes == null ? 43 : $mistakes.hashCode());
        final java.lang.Object $psychologyPatterns = this.getPsychologyPatterns();
        result = result * PRIME + ($psychologyPatterns == null ? 43 : $psychologyPatterns.hashCode());
        final java.lang.Object $actionItems = this.getActionItems();
        result = result * PRIME + ($actionItems == null ? 43 : $actionItems.hashCode());
        final java.lang.Object $rawResponseText = this.getRawResponseText();
        result = result * PRIME + ($rawResponseText == null ? 43 : $rawResponseText.hashCode());
        return result;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public java.lang.String toString() {
        return "WeeklyReflectionResponse(reflectionId=" + this.getReflectionId() + ", summary=" + this.getSummary() + ", strengths=" + this.getStrengths() + ", mistakes=" + this.getMistakes() + ", psychologyPatterns=" + this.getPsychologyPatterns() + ", actionItems=" + this.getActionItems() + ", coachingScore=" + this.getCoachingScore() + ", rawResponseText=" + this.getRawResponseText() + ")";
    }
}
