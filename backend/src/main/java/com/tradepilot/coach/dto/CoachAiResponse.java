package com.tradepilot.coach.dto;


public class CoachAiResponse {
    private String headline;
    private String emotionalPattern;
    private String bestBehavior;
    private String biggestMistake;
    private String bestSetup;
    private String avoidNextSession;
    private String nextSessionFocus;
    private String riskNote;
    private String disciplineNote;
    private String oneRuleForNextSession;


    @java.lang.SuppressWarnings("all")
    public static class CoachAiResponseBuilder {
        @java.lang.SuppressWarnings("all")
        private String headline;
        @java.lang.SuppressWarnings("all")
        private String emotionalPattern;
        @java.lang.SuppressWarnings("all")
        private String bestBehavior;
        @java.lang.SuppressWarnings("all")
        private String biggestMistake;
        @java.lang.SuppressWarnings("all")
        private String bestSetup;
        @java.lang.SuppressWarnings("all")
        private String avoidNextSession;
        @java.lang.SuppressWarnings("all")
        private String nextSessionFocus;
        @java.lang.SuppressWarnings("all")
        private String riskNote;
        @java.lang.SuppressWarnings("all")
        private String disciplineNote;
        @java.lang.SuppressWarnings("all")
        private String oneRuleForNextSession;

        @java.lang.SuppressWarnings("all")
        CoachAiResponseBuilder() {
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public CoachAiResponse.CoachAiResponseBuilder headline(final String headline) {
            this.headline = headline;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public CoachAiResponse.CoachAiResponseBuilder emotionalPattern(final String emotionalPattern) {
            this.emotionalPattern = emotionalPattern;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public CoachAiResponse.CoachAiResponseBuilder bestBehavior(final String bestBehavior) {
            this.bestBehavior = bestBehavior;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public CoachAiResponse.CoachAiResponseBuilder biggestMistake(final String biggestMistake) {
            this.biggestMistake = biggestMistake;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public CoachAiResponse.CoachAiResponseBuilder bestSetup(final String bestSetup) {
            this.bestSetup = bestSetup;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public CoachAiResponse.CoachAiResponseBuilder avoidNextSession(final String avoidNextSession) {
            this.avoidNextSession = avoidNextSession;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public CoachAiResponse.CoachAiResponseBuilder nextSessionFocus(final String nextSessionFocus) {
            this.nextSessionFocus = nextSessionFocus;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public CoachAiResponse.CoachAiResponseBuilder riskNote(final String riskNote) {
            this.riskNote = riskNote;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public CoachAiResponse.CoachAiResponseBuilder disciplineNote(final String disciplineNote) {
            this.disciplineNote = disciplineNote;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public CoachAiResponse.CoachAiResponseBuilder oneRuleForNextSession(final String oneRuleForNextSession) {
            this.oneRuleForNextSession = oneRuleForNextSession;
            return this;
        }

        @java.lang.SuppressWarnings("all")
        public CoachAiResponse build() {
            return new CoachAiResponse(this.headline, this.emotionalPattern, this.bestBehavior, this.biggestMistake, this.bestSetup, this.avoidNextSession, this.nextSessionFocus, this.riskNote, this.disciplineNote, this.oneRuleForNextSession);
        }

        @java.lang.Override
        @java.lang.SuppressWarnings("all")
        public java.lang.String toString() {
            return "CoachAiResponse.CoachAiResponseBuilder(headline=" + this.headline + ", emotionalPattern=" + this.emotionalPattern + ", bestBehavior=" + this.bestBehavior + ", biggestMistake=" + this.biggestMistake + ", bestSetup=" + this.bestSetup + ", avoidNextSession=" + this.avoidNextSession + ", nextSessionFocus=" + this.nextSessionFocus + ", riskNote=" + this.riskNote + ", disciplineNote=" + this.disciplineNote + ", oneRuleForNextSession=" + this.oneRuleForNextSession + ")";
        }
    }

    @java.lang.SuppressWarnings("all")
    public static CoachAiResponse.CoachAiResponseBuilder builder() {
        return new CoachAiResponse.CoachAiResponseBuilder();
    }

    @java.lang.SuppressWarnings("all")
    public String getHeadline() {
        return this.headline;
    }

    @java.lang.SuppressWarnings("all")
    public String getEmotionalPattern() {
        return this.emotionalPattern;
    }

    @java.lang.SuppressWarnings("all")
    public String getBestBehavior() {
        return this.bestBehavior;
    }

    @java.lang.SuppressWarnings("all")
    public String getBiggestMistake() {
        return this.biggestMistake;
    }

    @java.lang.SuppressWarnings("all")
    public String getBestSetup() {
        return this.bestSetup;
    }

    @java.lang.SuppressWarnings("all")
    public String getAvoidNextSession() {
        return this.avoidNextSession;
    }

    @java.lang.SuppressWarnings("all")
    public String getNextSessionFocus() {
        return this.nextSessionFocus;
    }

    @java.lang.SuppressWarnings("all")
    public String getRiskNote() {
        return this.riskNote;
    }

    @java.lang.SuppressWarnings("all")
    public String getDisciplineNote() {
        return this.disciplineNote;
    }

    @java.lang.SuppressWarnings("all")
    public String getOneRuleForNextSession() {
        return this.oneRuleForNextSession;
    }

    @java.lang.SuppressWarnings("all")
    public void setHeadline(final String headline) {
        this.headline = headline;
    }

    @java.lang.SuppressWarnings("all")
    public void setEmotionalPattern(final String emotionalPattern) {
        this.emotionalPattern = emotionalPattern;
    }

    @java.lang.SuppressWarnings("all")
    public void setBestBehavior(final String bestBehavior) {
        this.bestBehavior = bestBehavior;
    }

    @java.lang.SuppressWarnings("all")
    public void setBiggestMistake(final String biggestMistake) {
        this.biggestMistake = biggestMistake;
    }

    @java.lang.SuppressWarnings("all")
    public void setBestSetup(final String bestSetup) {
        this.bestSetup = bestSetup;
    }

    @java.lang.SuppressWarnings("all")
    public void setAvoidNextSession(final String avoidNextSession) {
        this.avoidNextSession = avoidNextSession;
    }

    @java.lang.SuppressWarnings("all")
    public void setNextSessionFocus(final String nextSessionFocus) {
        this.nextSessionFocus = nextSessionFocus;
    }

    @java.lang.SuppressWarnings("all")
    public void setRiskNote(final String riskNote) {
        this.riskNote = riskNote;
    }

    @java.lang.SuppressWarnings("all")
    public void setDisciplineNote(final String disciplineNote) {
        this.disciplineNote = disciplineNote;
    }

    @java.lang.SuppressWarnings("all")
    public void setOneRuleForNextSession(final String oneRuleForNextSession) {
        this.oneRuleForNextSession = oneRuleForNextSession;
    }

    @java.lang.SuppressWarnings("all")
    public CoachAiResponse() {
    }

    @java.lang.SuppressWarnings("all")
    public CoachAiResponse(final String headline, final String emotionalPattern, final String bestBehavior, final String biggestMistake, final String bestSetup, final String avoidNextSession, final String nextSessionFocus, final String riskNote, final String disciplineNote, final String oneRuleForNextSession) {
        this.headline = headline;
        this.emotionalPattern = emotionalPattern;
        this.bestBehavior = bestBehavior;
        this.biggestMistake = biggestMistake;
        this.bestSetup = bestSetup;
        this.avoidNextSession = avoidNextSession;
        this.nextSessionFocus = nextSessionFocus;
        this.riskNote = riskNote;
        this.disciplineNote = disciplineNote;
        this.oneRuleForNextSession = oneRuleForNextSession;
    }
}
