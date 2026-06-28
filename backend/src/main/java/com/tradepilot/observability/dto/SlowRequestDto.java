package com.tradepilot.observability.dto;

import java.time.LocalDateTime;

public class SlowRequestDto {
    private String endpoint;
    private long durationMs;
    private int status;
    private Long userId;
    private String severity;
    private LocalDateTime createdAt;

    @java.lang.SuppressWarnings("all")
    SlowRequestDto(final String endpoint, final long durationMs, final int status, final Long userId, final String severity, final LocalDateTime createdAt) {
        this.endpoint = endpoint;
        this.durationMs = durationMs;
        this.status = status;
        this.userId = userId;
        this.severity = severity;
        this.createdAt = createdAt;
    }


    @java.lang.SuppressWarnings("all")
    public static class SlowRequestDtoBuilder {
        @java.lang.SuppressWarnings("all")
        private String endpoint;
        @java.lang.SuppressWarnings("all")
        private long durationMs;
        @java.lang.SuppressWarnings("all")
        private int status;
        @java.lang.SuppressWarnings("all")
        private Long userId;
        @java.lang.SuppressWarnings("all")
        private String severity;
        @java.lang.SuppressWarnings("all")
        private LocalDateTime createdAt;

        @java.lang.SuppressWarnings("all")
        SlowRequestDtoBuilder() {
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public SlowRequestDto.SlowRequestDtoBuilder endpoint(final String endpoint) {
            this.endpoint = endpoint;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public SlowRequestDto.SlowRequestDtoBuilder durationMs(final long durationMs) {
            this.durationMs = durationMs;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public SlowRequestDto.SlowRequestDtoBuilder status(final int status) {
            this.status = status;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public SlowRequestDto.SlowRequestDtoBuilder userId(final Long userId) {
            this.userId = userId;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public SlowRequestDto.SlowRequestDtoBuilder severity(final String severity) {
            this.severity = severity;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public SlowRequestDto.SlowRequestDtoBuilder createdAt(final LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        @java.lang.SuppressWarnings("all")
        public SlowRequestDto build() {
            return new SlowRequestDto(this.endpoint, this.durationMs, this.status, this.userId, this.severity, this.createdAt);
        }

        @java.lang.Override
        @java.lang.SuppressWarnings("all")
        public java.lang.String toString() {
            return "SlowRequestDto.SlowRequestDtoBuilder(endpoint=" + this.endpoint + ", durationMs=" + this.durationMs + ", status=" + this.status + ", userId=" + this.userId + ", severity=" + this.severity + ", createdAt=" + this.createdAt + ")";
        }
    }

    @java.lang.SuppressWarnings("all")
    public static SlowRequestDto.SlowRequestDtoBuilder builder() {
        return new SlowRequestDto.SlowRequestDtoBuilder();
    }

    @java.lang.SuppressWarnings("all")
    public String getEndpoint() {
        return this.endpoint;
    }

    @java.lang.SuppressWarnings("all")
    public long getDurationMs() {
        return this.durationMs;
    }

    @java.lang.SuppressWarnings("all")
    public int getStatus() {
        return this.status;
    }

    @java.lang.SuppressWarnings("all")
    public Long getUserId() {
        return this.userId;
    }

    @java.lang.SuppressWarnings("all")
    public String getSeverity() {
        return this.severity;
    }

    @java.lang.SuppressWarnings("all")
    public LocalDateTime getCreatedAt() {
        return this.createdAt;
    }

    @java.lang.SuppressWarnings("all")
    public void setEndpoint(final String endpoint) {
        this.endpoint = endpoint;
    }

    @java.lang.SuppressWarnings("all")
    public void setDurationMs(final long durationMs) {
        this.durationMs = durationMs;
    }

    @java.lang.SuppressWarnings("all")
    public void setStatus(final int status) {
        this.status = status;
    }

    @java.lang.SuppressWarnings("all")
    public void setUserId(final Long userId) {
        this.userId = userId;
    }

    @java.lang.SuppressWarnings("all")
    public void setSeverity(final String severity) {
        this.severity = severity;
    }

    @java.lang.SuppressWarnings("all")
    public void setCreatedAt(final LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof SlowRequestDto)) return false;
        final SlowRequestDto other = (SlowRequestDto) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        if (this.getDurationMs() != other.getDurationMs()) return false;
        if (this.getStatus() != other.getStatus()) return false;
        final java.lang.Object this$userId = this.getUserId();
        final java.lang.Object other$userId = other.getUserId();
        if (this$userId == null ? other$userId != null : !this$userId.equals(other$userId)) return false;
        final java.lang.Object this$endpoint = this.getEndpoint();
        final java.lang.Object other$endpoint = other.getEndpoint();
        if (this$endpoint == null ? other$endpoint != null : !this$endpoint.equals(other$endpoint)) return false;
        final java.lang.Object this$severity = this.getSeverity();
        final java.lang.Object other$severity = other.getSeverity();
        if (this$severity == null ? other$severity != null : !this$severity.equals(other$severity)) return false;
        final java.lang.Object this$createdAt = this.getCreatedAt();
        final java.lang.Object other$createdAt = other.getCreatedAt();
        if (this$createdAt == null ? other$createdAt != null : !this$createdAt.equals(other$createdAt)) return false;
        return true;
    }

    @java.lang.SuppressWarnings("all")
    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof SlowRequestDto;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final long $durationMs = this.getDurationMs();
        result = result * PRIME + (int) ($durationMs >>> 32 ^ $durationMs);
        result = result * PRIME + this.getStatus();
        final java.lang.Object $userId = this.getUserId();
        result = result * PRIME + ($userId == null ? 43 : $userId.hashCode());
        final java.lang.Object $endpoint = this.getEndpoint();
        result = result * PRIME + ($endpoint == null ? 43 : $endpoint.hashCode());
        final java.lang.Object $severity = this.getSeverity();
        result = result * PRIME + ($severity == null ? 43 : $severity.hashCode());
        final java.lang.Object $createdAt = this.getCreatedAt();
        result = result * PRIME + ($createdAt == null ? 43 : $createdAt.hashCode());
        return result;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public java.lang.String toString() {
        return "SlowRequestDto(endpoint=" + this.getEndpoint() + ", durationMs=" + this.getDurationMs() + ", status=" + this.getStatus() + ", userId=" + this.getUserId() + ", severity=" + this.getSeverity() + ", createdAt=" + this.getCreatedAt() + ")";
    }
}
