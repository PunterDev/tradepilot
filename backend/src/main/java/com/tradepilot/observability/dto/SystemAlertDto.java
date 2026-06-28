package com.tradepilot.observability.dto;

import java.time.LocalDateTime;

public class SystemAlertDto {
    private Long id;
    private String type;
    private String severity;
    private String message;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime resolvedAt;
    private Long resolvedBy;

    @java.lang.SuppressWarnings("all")
    SystemAlertDto(final Long id, final String type, final String severity, final String message, final String status, final LocalDateTime createdAt, final LocalDateTime resolvedAt, final Long resolvedBy) {
        this.id = id;
        this.type = type;
        this.severity = severity;
        this.message = message;
        this.status = status;
        this.createdAt = createdAt;
        this.resolvedAt = resolvedAt;
        this.resolvedBy = resolvedBy;
    }


    @java.lang.SuppressWarnings("all")
    public static class SystemAlertDtoBuilder {
        @java.lang.SuppressWarnings("all")
        private Long id;
        @java.lang.SuppressWarnings("all")
        private String type;
        @java.lang.SuppressWarnings("all")
        private String severity;
        @java.lang.SuppressWarnings("all")
        private String message;
        @java.lang.SuppressWarnings("all")
        private String status;
        @java.lang.SuppressWarnings("all")
        private LocalDateTime createdAt;
        @java.lang.SuppressWarnings("all")
        private LocalDateTime resolvedAt;
        @java.lang.SuppressWarnings("all")
        private Long resolvedBy;

        @java.lang.SuppressWarnings("all")
        SystemAlertDtoBuilder() {
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public SystemAlertDto.SystemAlertDtoBuilder id(final Long id) {
            this.id = id;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public SystemAlertDto.SystemAlertDtoBuilder type(final String type) {
            this.type = type;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public SystemAlertDto.SystemAlertDtoBuilder severity(final String severity) {
            this.severity = severity;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public SystemAlertDto.SystemAlertDtoBuilder message(final String message) {
            this.message = message;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public SystemAlertDto.SystemAlertDtoBuilder status(final String status) {
            this.status = status;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public SystemAlertDto.SystemAlertDtoBuilder createdAt(final LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public SystemAlertDto.SystemAlertDtoBuilder resolvedAt(final LocalDateTime resolvedAt) {
            this.resolvedAt = resolvedAt;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public SystemAlertDto.SystemAlertDtoBuilder resolvedBy(final Long resolvedBy) {
            this.resolvedBy = resolvedBy;
            return this;
        }

        @java.lang.SuppressWarnings("all")
        public SystemAlertDto build() {
            return new SystemAlertDto(this.id, this.type, this.severity, this.message, this.status, this.createdAt, this.resolvedAt, this.resolvedBy);
        }

        @java.lang.Override
        @java.lang.SuppressWarnings("all")
        public java.lang.String toString() {
            return "SystemAlertDto.SystemAlertDtoBuilder(id=" + this.id + ", type=" + this.type + ", severity=" + this.severity + ", message=" + this.message + ", status=" + this.status + ", createdAt=" + this.createdAt + ", resolvedAt=" + this.resolvedAt + ", resolvedBy=" + this.resolvedBy + ")";
        }
    }

    @java.lang.SuppressWarnings("all")
    public static SystemAlertDto.SystemAlertDtoBuilder builder() {
        return new SystemAlertDto.SystemAlertDtoBuilder();
    }

    @java.lang.SuppressWarnings("all")
    public Long getId() {
        return this.id;
    }

    @java.lang.SuppressWarnings("all")
    public String getType() {
        return this.type;
    }

    @java.lang.SuppressWarnings("all")
    public String getSeverity() {
        return this.severity;
    }

    @java.lang.SuppressWarnings("all")
    public String getMessage() {
        return this.message;
    }

    @java.lang.SuppressWarnings("all")
    public String getStatus() {
        return this.status;
    }

    @java.lang.SuppressWarnings("all")
    public LocalDateTime getCreatedAt() {
        return this.createdAt;
    }

    @java.lang.SuppressWarnings("all")
    public LocalDateTime getResolvedAt() {
        return this.resolvedAt;
    }

    @java.lang.SuppressWarnings("all")
    public Long getResolvedBy() {
        return this.resolvedBy;
    }

    @java.lang.SuppressWarnings("all")
    public void setId(final Long id) {
        this.id = id;
    }

    @java.lang.SuppressWarnings("all")
    public void setType(final String type) {
        this.type = type;
    }

    @java.lang.SuppressWarnings("all")
    public void setSeverity(final String severity) {
        this.severity = severity;
    }

    @java.lang.SuppressWarnings("all")
    public void setMessage(final String message) {
        this.message = message;
    }

    @java.lang.SuppressWarnings("all")
    public void setStatus(final String status) {
        this.status = status;
    }

    @java.lang.SuppressWarnings("all")
    public void setCreatedAt(final LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @java.lang.SuppressWarnings("all")
    public void setResolvedAt(final LocalDateTime resolvedAt) {
        this.resolvedAt = resolvedAt;
    }

    @java.lang.SuppressWarnings("all")
    public void setResolvedBy(final Long resolvedBy) {
        this.resolvedBy = resolvedBy;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof SystemAlertDto)) return false;
        final SystemAlertDto other = (SystemAlertDto) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$id = this.getId();
        final java.lang.Object other$id = other.getId();
        if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
        final java.lang.Object this$resolvedBy = this.getResolvedBy();
        final java.lang.Object other$resolvedBy = other.getResolvedBy();
        if (this$resolvedBy == null ? other$resolvedBy != null : !this$resolvedBy.equals(other$resolvedBy)) return false;
        final java.lang.Object this$type = this.getType();
        final java.lang.Object other$type = other.getType();
        if (this$type == null ? other$type != null : !this$type.equals(other$type)) return false;
        final java.lang.Object this$severity = this.getSeverity();
        final java.lang.Object other$severity = other.getSeverity();
        if (this$severity == null ? other$severity != null : !this$severity.equals(other$severity)) return false;
        final java.lang.Object this$message = this.getMessage();
        final java.lang.Object other$message = other.getMessage();
        if (this$message == null ? other$message != null : !this$message.equals(other$message)) return false;
        final java.lang.Object this$status = this.getStatus();
        final java.lang.Object other$status = other.getStatus();
        if (this$status == null ? other$status != null : !this$status.equals(other$status)) return false;
        final java.lang.Object this$createdAt = this.getCreatedAt();
        final java.lang.Object other$createdAt = other.getCreatedAt();
        if (this$createdAt == null ? other$createdAt != null : !this$createdAt.equals(other$createdAt)) return false;
        final java.lang.Object this$resolvedAt = this.getResolvedAt();
        final java.lang.Object other$resolvedAt = other.getResolvedAt();
        if (this$resolvedAt == null ? other$resolvedAt != null : !this$resolvedAt.equals(other$resolvedAt)) return false;
        return true;
    }

    @java.lang.SuppressWarnings("all")
    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof SystemAlertDto;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $id = this.getId();
        result = result * PRIME + ($id == null ? 43 : $id.hashCode());
        final java.lang.Object $resolvedBy = this.getResolvedBy();
        result = result * PRIME + ($resolvedBy == null ? 43 : $resolvedBy.hashCode());
        final java.lang.Object $type = this.getType();
        result = result * PRIME + ($type == null ? 43 : $type.hashCode());
        final java.lang.Object $severity = this.getSeverity();
        result = result * PRIME + ($severity == null ? 43 : $severity.hashCode());
        final java.lang.Object $message = this.getMessage();
        result = result * PRIME + ($message == null ? 43 : $message.hashCode());
        final java.lang.Object $status = this.getStatus();
        result = result * PRIME + ($status == null ? 43 : $status.hashCode());
        final java.lang.Object $createdAt = this.getCreatedAt();
        result = result * PRIME + ($createdAt == null ? 43 : $createdAt.hashCode());
        final java.lang.Object $resolvedAt = this.getResolvedAt();
        result = result * PRIME + ($resolvedAt == null ? 43 : $resolvedAt.hashCode());
        return result;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public java.lang.String toString() {
        return "SystemAlertDto(id=" + this.getId() + ", type=" + this.getType() + ", severity=" + this.getSeverity() + ", message=" + this.getMessage() + ", status=" + this.getStatus() + ", createdAt=" + this.getCreatedAt() + ", resolvedAt=" + this.getResolvedAt() + ", resolvedBy=" + this.getResolvedBy() + ")";
    }
}
