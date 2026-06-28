package com.tradepilot.dto.session;

public class SessionMistakeResponse {
    private String type;
    private String title;
    private String description;
    private String severity;

    @java.lang.SuppressWarnings("all")
    SessionMistakeResponse(final String type, final String title, final String description, final String severity) {
        this.type = type;
        this.title = title;
        this.description = description;
        this.severity = severity;
    }


    @java.lang.SuppressWarnings("all")
    public static class SessionMistakeResponseBuilder {
        @java.lang.SuppressWarnings("all")
        private String type;
        @java.lang.SuppressWarnings("all")
        private String title;
        @java.lang.SuppressWarnings("all")
        private String description;
        @java.lang.SuppressWarnings("all")
        private String severity;

        @java.lang.SuppressWarnings("all")
        SessionMistakeResponseBuilder() {
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public SessionMistakeResponse.SessionMistakeResponseBuilder type(final String type) {
            this.type = type;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public SessionMistakeResponse.SessionMistakeResponseBuilder title(final String title) {
            this.title = title;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public SessionMistakeResponse.SessionMistakeResponseBuilder description(final String description) {
            this.description = description;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public SessionMistakeResponse.SessionMistakeResponseBuilder severity(final String severity) {
            this.severity = severity;
            return this;
        }

        @java.lang.SuppressWarnings("all")
        public SessionMistakeResponse build() {
            return new SessionMistakeResponse(this.type, this.title, this.description, this.severity);
        }

        @java.lang.Override
        @java.lang.SuppressWarnings("all")
        public java.lang.String toString() {
            return "SessionMistakeResponse.SessionMistakeResponseBuilder(type=" + this.type + ", title=" + this.title + ", description=" + this.description + ", severity=" + this.severity + ")";
        }
    }

    @java.lang.SuppressWarnings("all")
    public static SessionMistakeResponse.SessionMistakeResponseBuilder builder() {
        return new SessionMistakeResponse.SessionMistakeResponseBuilder();
    }

    @java.lang.SuppressWarnings("all")
    public String getType() {
        return this.type;
    }

    @java.lang.SuppressWarnings("all")
    public String getTitle() {
        return this.title;
    }

    @java.lang.SuppressWarnings("all")
    public String getDescription() {
        return this.description;
    }

    @java.lang.SuppressWarnings("all")
    public String getSeverity() {
        return this.severity;
    }

    @java.lang.SuppressWarnings("all")
    public void setType(final String type) {
        this.type = type;
    }

    @java.lang.SuppressWarnings("all")
    public void setTitle(final String title) {
        this.title = title;
    }

    @java.lang.SuppressWarnings("all")
    public void setDescription(final String description) {
        this.description = description;
    }

    @java.lang.SuppressWarnings("all")
    public void setSeverity(final String severity) {
        this.severity = severity;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof SessionMistakeResponse)) return false;
        final SessionMistakeResponse other = (SessionMistakeResponse) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$type = this.getType();
        final java.lang.Object other$type = other.getType();
        if (this$type == null ? other$type != null : !this$type.equals(other$type)) return false;
        final java.lang.Object this$title = this.getTitle();
        final java.lang.Object other$title = other.getTitle();
        if (this$title == null ? other$title != null : !this$title.equals(other$title)) return false;
        final java.lang.Object this$description = this.getDescription();
        final java.lang.Object other$description = other.getDescription();
        if (this$description == null ? other$description != null : !this$description.equals(other$description)) return false;
        final java.lang.Object this$severity = this.getSeverity();
        final java.lang.Object other$severity = other.getSeverity();
        if (this$severity == null ? other$severity != null : !this$severity.equals(other$severity)) return false;
        return true;
    }

    @java.lang.SuppressWarnings("all")
    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof SessionMistakeResponse;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $type = this.getType();
        result = result * PRIME + ($type == null ? 43 : $type.hashCode());
        final java.lang.Object $title = this.getTitle();
        result = result * PRIME + ($title == null ? 43 : $title.hashCode());
        final java.lang.Object $description = this.getDescription();
        result = result * PRIME + ($description == null ? 43 : $description.hashCode());
        final java.lang.Object $severity = this.getSeverity();
        result = result * PRIME + ($severity == null ? 43 : $severity.hashCode());
        return result;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public java.lang.String toString() {
        return "SessionMistakeResponse(type=" + this.getType() + ", title=" + this.getTitle() + ", description=" + this.getDescription() + ", severity=" + this.getSeverity() + ")";
    }
}
