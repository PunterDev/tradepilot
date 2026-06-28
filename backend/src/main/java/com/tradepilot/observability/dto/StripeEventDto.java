package com.tradepilot.observability.dto;

import java.time.LocalDateTime;

public class StripeEventDto {
    private String eventType;
    private boolean success;
    private long durationMs;
    private String errorMessage;
    private LocalDateTime createdAt;

    @java.lang.SuppressWarnings("all")
    StripeEventDto(final String eventType, final boolean success, final long durationMs, final String errorMessage, final LocalDateTime createdAt) {
        this.eventType = eventType;
        this.success = success;
        this.durationMs = durationMs;
        this.errorMessage = errorMessage;
        this.createdAt = createdAt;
    }


    @java.lang.SuppressWarnings("all")
    public static class StripeEventDtoBuilder {
        @java.lang.SuppressWarnings("all")
        private String eventType;
        @java.lang.SuppressWarnings("all")
        private boolean success;
        @java.lang.SuppressWarnings("all")
        private long durationMs;
        @java.lang.SuppressWarnings("all")
        private String errorMessage;
        @java.lang.SuppressWarnings("all")
        private LocalDateTime createdAt;

        @java.lang.SuppressWarnings("all")
        StripeEventDtoBuilder() {
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public StripeEventDto.StripeEventDtoBuilder eventType(final String eventType) {
            this.eventType = eventType;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public StripeEventDto.StripeEventDtoBuilder success(final boolean success) {
            this.success = success;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public StripeEventDto.StripeEventDtoBuilder durationMs(final long durationMs) {
            this.durationMs = durationMs;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public StripeEventDto.StripeEventDtoBuilder errorMessage(final String errorMessage) {
            this.errorMessage = errorMessage;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public StripeEventDto.StripeEventDtoBuilder createdAt(final LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        @java.lang.SuppressWarnings("all")
        public StripeEventDto build() {
            return new StripeEventDto(this.eventType, this.success, this.durationMs, this.errorMessage, this.createdAt);
        }

        @java.lang.Override
        @java.lang.SuppressWarnings("all")
        public java.lang.String toString() {
            return "StripeEventDto.StripeEventDtoBuilder(eventType=" + this.eventType + ", success=" + this.success + ", durationMs=" + this.durationMs + ", errorMessage=" + this.errorMessage + ", createdAt=" + this.createdAt + ")";
        }
    }

    @java.lang.SuppressWarnings("all")
    public static StripeEventDto.StripeEventDtoBuilder builder() {
        return new StripeEventDto.StripeEventDtoBuilder();
    }

    @java.lang.SuppressWarnings("all")
    public String getEventType() {
        return this.eventType;
    }

    @java.lang.SuppressWarnings("all")
    public boolean isSuccess() {
        return this.success;
    }

    @java.lang.SuppressWarnings("all")
    public long getDurationMs() {
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
    public void setEventType(final String eventType) {
        this.eventType = eventType;
    }

    @java.lang.SuppressWarnings("all")
    public void setSuccess(final boolean success) {
        this.success = success;
    }

    @java.lang.SuppressWarnings("all")
    public void setDurationMs(final long durationMs) {
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

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof StripeEventDto)) return false;
        final StripeEventDto other = (StripeEventDto) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        if (this.isSuccess() != other.isSuccess()) return false;
        if (this.getDurationMs() != other.getDurationMs()) return false;
        final java.lang.Object this$eventType = this.getEventType();
        final java.lang.Object other$eventType = other.getEventType();
        if (this$eventType == null ? other$eventType != null : !this$eventType.equals(other$eventType)) return false;
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
        return other instanceof StripeEventDto;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        result = result * PRIME + (this.isSuccess() ? 79 : 97);
        final long $durationMs = this.getDurationMs();
        result = result * PRIME + (int) ($durationMs >>> 32 ^ $durationMs);
        final java.lang.Object $eventType = this.getEventType();
        result = result * PRIME + ($eventType == null ? 43 : $eventType.hashCode());
        final java.lang.Object $errorMessage = this.getErrorMessage();
        result = result * PRIME + ($errorMessage == null ? 43 : $errorMessage.hashCode());
        final java.lang.Object $createdAt = this.getCreatedAt();
        result = result * PRIME + ($createdAt == null ? 43 : $createdAt.hashCode());
        return result;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public java.lang.String toString() {
        return "StripeEventDto(eventType=" + this.getEventType() + ", success=" + this.isSuccess() + ", durationMs=" + this.getDurationMs() + ", errorMessage=" + this.getErrorMessage() + ", createdAt=" + this.getCreatedAt() + ")";
    }
}
