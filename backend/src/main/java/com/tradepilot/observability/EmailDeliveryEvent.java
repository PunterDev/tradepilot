package com.tradepilot.observability;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "email_delivery_events", indexes = {@Index(name = "idx_email_delivery_events_created_at", columnList = "created_at"), @Index(name = "idx_email_delivery_events_success_created_at", columnList = "success, created_at")})
public class EmailDeliveryEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 240)
    private String recipient;
    @Column(nullable = false, length = 120)
    private String template;
    @Column(nullable = false)
    private Boolean success;
    @Column(name = "duration_ms", nullable = false)
    private Long durationMs;
    @Column(name = "error_message", length = 700)
    private String errorMessage;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;


    @java.lang.SuppressWarnings("all")
    public static class EmailDeliveryEventBuilder {
        @java.lang.SuppressWarnings("all")
        private Long id;
        @java.lang.SuppressWarnings("all")
        private String recipient;
        @java.lang.SuppressWarnings("all")
        private String template;
        @java.lang.SuppressWarnings("all")
        private Boolean success;
        @java.lang.SuppressWarnings("all")
        private Long durationMs;
        @java.lang.SuppressWarnings("all")
        private String errorMessage;
        @java.lang.SuppressWarnings("all")
        private LocalDateTime createdAt;

        @java.lang.SuppressWarnings("all")
        EmailDeliveryEventBuilder() {
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public EmailDeliveryEvent.EmailDeliveryEventBuilder id(final Long id) {
            this.id = id;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public EmailDeliveryEvent.EmailDeliveryEventBuilder recipient(final String recipient) {
            this.recipient = recipient;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public EmailDeliveryEvent.EmailDeliveryEventBuilder template(final String template) {
            this.template = template;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public EmailDeliveryEvent.EmailDeliveryEventBuilder success(final Boolean success) {
            this.success = success;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public EmailDeliveryEvent.EmailDeliveryEventBuilder durationMs(final Long durationMs) {
            this.durationMs = durationMs;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public EmailDeliveryEvent.EmailDeliveryEventBuilder errorMessage(final String errorMessage) {
            this.errorMessage = errorMessage;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public EmailDeliveryEvent.EmailDeliveryEventBuilder createdAt(final LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        @java.lang.SuppressWarnings("all")
        public EmailDeliveryEvent build() {
            return new EmailDeliveryEvent(this.id, this.recipient, this.template, this.success, this.durationMs, this.errorMessage, this.createdAt);
        }

        @java.lang.Override
        @java.lang.SuppressWarnings("all")
        public java.lang.String toString() {
            return "EmailDeliveryEvent.EmailDeliveryEventBuilder(id=" + this.id + ", recipient=" + this.recipient + ", template=" + this.template + ", success=" + this.success + ", durationMs=" + this.durationMs + ", errorMessage=" + this.errorMessage + ", createdAt=" + this.createdAt + ")";
        }
    }

    @java.lang.SuppressWarnings("all")
    public static EmailDeliveryEvent.EmailDeliveryEventBuilder builder() {
        return new EmailDeliveryEvent.EmailDeliveryEventBuilder();
    }

    @java.lang.SuppressWarnings("all")
    public Long getId() {
        return this.id;
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
    public Boolean getSuccess() {
        return this.success;
    }

    @java.lang.SuppressWarnings("all")
    public Long getDurationMs() {
        return this.durationMs;
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
    public void setId(final Long id) {
        this.id = id;
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
    public void setSuccess(final Boolean success) {
        this.success = success;
    }

    @java.lang.SuppressWarnings("all")
    public void setDurationMs(final Long durationMs) {
        this.durationMs = durationMs;
    }

    @java.lang.SuppressWarnings("all")
    public void setErrorMessage(final String errorMessage) {
        this.errorMessage = errorMessage;
    }

    @java.lang.SuppressWarnings("all")
    public void setCreatedAt(final LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @java.lang.SuppressWarnings("all")
    public EmailDeliveryEvent() {
    }

    @java.lang.SuppressWarnings("all")
    public EmailDeliveryEvent(final Long id, final String recipient, final String template, final Boolean success, final Long durationMs, final String errorMessage, final LocalDateTime createdAt) {
        this.id = id;
        this.recipient = recipient;
        this.template = template;
        this.success = success;
        this.durationMs = durationMs;
        this.errorMessage = errorMessage;
        this.createdAt = createdAt;
    }
}
