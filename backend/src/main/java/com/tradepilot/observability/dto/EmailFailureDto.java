package com.tradepilot.observability.dto;

import java.time.LocalDateTime;

public class EmailFailureDto {
    private String recipient;
    private String template;
    private String errorMessage;
    private LocalDateTime createdAt;

    @java.lang.SuppressWarnings("all")
    EmailFailureDto(final String recipient, final String template, final String errorMessage, final LocalDateTime createdAt) {
        this.recipient = recipient;
        this.template = template;
        this.errorMessage = errorMessage;
        this.createdAt = createdAt;
    }


    @java.lang.SuppressWarnings("all")
    public static class EmailFailureDtoBuilder {
        @java.lang.SuppressWarnings("all")
        private String recipient;
        @java.lang.SuppressWarnings("all")
        private String template;
        @java.lang.SuppressWarnings("all")
        private String errorMessage;
        @java.lang.SuppressWarnings("all")
        private LocalDateTime createdAt;

        @java.lang.SuppressWarnings("all")
        EmailFailureDtoBuilder() {
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public EmailFailureDto.EmailFailureDtoBuilder recipient(final String recipient) {
            this.recipient = recipient;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public EmailFailureDto.EmailFailureDtoBuilder template(final String template) {
            this.template = template;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public EmailFailureDto.EmailFailureDtoBuilder errorMessage(final String errorMessage) {
            this.errorMessage = errorMessage;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public EmailFailureDto.EmailFailureDtoBuilder createdAt(final LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        @java.lang.SuppressWarnings("all")
        public EmailFailureDto build() {
            return new EmailFailureDto(this.recipient, this.template, this.errorMessage, this.createdAt);
        }

        @java.lang.Override
        @java.lang.SuppressWarnings("all")
        public java.lang.String toString() {
            return "EmailFailureDto.EmailFailureDtoBuilder(recipient=" + this.recipient + ", template=" + this.template + ", errorMessage=" + this.errorMessage + ", createdAt=" + this.createdAt + ")";
        }
    }

    @java.lang.SuppressWarnings("all")
    public static EmailFailureDto.EmailFailureDtoBuilder builder() {
        return new EmailFailureDto.EmailFailureDtoBuilder();
    }

    @java.lang.SuppressWarnings("all")
    public String getRecipient() {
        return this.recipient;
    }

    @java.lang.SuppressWarnings("all")
    public String getTemplate() {
        return this.template;
    }

    @java.lang.SuppressWarnings("all")
    public String getErrorMessage() {
        return this.errorMessage;
    }

    @java.lang.SuppressWarnings("all")
    public LocalDateTime getCreatedAt() {
        return this.createdAt;
    }

    @java.lang.SuppressWarnings("all")
    public void setRecipient(final String recipient) {
        this.recipient = recipient;
    }

    @java.lang.SuppressWarnings("all")
    public void setTemplate(final String template) {
        this.template = template;
    }

    @java.lang.SuppressWarnings("all")
    public void setErrorMessage(final String errorMessage) {
        this.errorMessage = errorMessage;
    }

    @java.lang.SuppressWarnings("all")
    public void setCreatedAt(final LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof EmailFailureDto)) return false;
        final EmailFailureDto other = (EmailFailureDto) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$recipient = this.getRecipient();
        final java.lang.Object other$recipient = other.getRecipient();
        if (this$recipient == null ? other$recipient != null : !this$recipient.equals(other$recipient)) return false;
        final java.lang.Object this$template = this.getTemplate();
        final java.lang.Object other$template = other.getTemplate();
        if (this$template == null ? other$template != null : !this$template.equals(other$template)) return false;
        final java.lang.Object this$errorMessage = this.getErrorMessage();
        final java.lang.Object other$errorMessage = other.getErrorMessage();
        if (this$errorMessage == null ? other$errorMessage != null : !this$errorMessage.equals(other$errorMessage)) return false;
        final java.lang.Object this$createdAt = this.getCreatedAt();
        final java.lang.Object other$createdAt = other.getCreatedAt();
        if (this$createdAt == null ? other$createdAt != null : !this$createdAt.equals(other$createdAt)) return false;
        return true;
    }

    @java.lang.SuppressWarnings("all")
    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof EmailFailureDto;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $recipient = this.getRecipient();
        result = result * PRIME + ($recipient == null ? 43 : $recipient.hashCode());
        final java.lang.Object $template = this.getTemplate();
        result = result * PRIME + ($template == null ? 43 : $template.hashCode());
        final java.lang.Object $errorMessage = this.getErrorMessage();
        result = result * PRIME + ($errorMessage == null ? 43 : $errorMessage.hashCode());
        final java.lang.Object $createdAt = this.getCreatedAt();
        result = result * PRIME + ($createdAt == null ? 43 : $createdAt.hashCode());
        return result;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public java.lang.String toString() {
        return "EmailFailureDto(recipient=" + this.getRecipient() + ", template=" + this.getTemplate() + ", errorMessage=" + this.getErrorMessage() + ", createdAt=" + this.getCreatedAt() + ")";
    }
}
