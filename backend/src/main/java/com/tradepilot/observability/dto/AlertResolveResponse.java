package com.tradepilot.observability.dto;

import java.time.LocalDateTime;

public class AlertResolveResponse {
    private Long id;
    private String status;
    private LocalDateTime resolvedAt;
    private Long resolvedBy;

    @java.lang.SuppressWarnings("all")
    AlertResolveResponse(final Long id, final String status, final LocalDateTime resolvedAt, final Long resolvedBy) {
        this.id = id;
        this.status = status;
        this.resolvedAt = resolvedAt;
        this.resolvedBy = resolvedBy;
    }


    @java.lang.SuppressWarnings("all")
    public static class AlertResolveResponseBuilder {
        @java.lang.SuppressWarnings("all")
        private Long id;
        @java.lang.SuppressWarnings("all")
        private String status;
        @java.lang.SuppressWarnings("all")
        private LocalDateTime resolvedAt;
        @java.lang.SuppressWarnings("all")
        private Long resolvedBy;

        @java.lang.SuppressWarnings("all")
        AlertResolveResponseBuilder() {
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public AlertResolveResponse.AlertResolveResponseBuilder id(final Long id) {
            this.id = id;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public AlertResolveResponse.AlertResolveResponseBuilder status(final String status) {
            this.status = status;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public AlertResolveResponse.AlertResolveResponseBuilder resolvedAt(final LocalDateTime resolvedAt) {
            this.resolvedAt = resolvedAt;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public AlertResolveResponse.AlertResolveResponseBuilder resolvedBy(final Long resolvedBy) {
            this.resolvedBy = resolvedBy;
            return this;
        }

        @java.lang.SuppressWarnings("all")
        public AlertResolveResponse build() {
            return new AlertResolveResponse(this.id, this.status, this.resolvedAt, this.resolvedBy);
        }

        @java.lang.Override
        @java.lang.SuppressWarnings("all")
        public java.lang.String toString() {
            return "AlertResolveResponse.AlertResolveResponseBuilder(id=" + this.id + ", status=" + this.status + ", resolvedAt=" + this.resolvedAt + ", resolvedBy=" + this.resolvedBy + ")";
        }
    }

    @java.lang.SuppressWarnings("all")
    public static AlertResolveResponse.AlertResolveResponseBuilder builder() {
        return new AlertResolveResponse.AlertResolveResponseBuilder();
    }

    @java.lang.SuppressWarnings("all")
    public Long getId() {
        return this.id;
    }

    @java.lang.SuppressWarnings("all")
    public String getStatus() {
        return this.status;
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
    public void setStatus(final String status) {
        this.status = status;
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
        if (!(o instanceof AlertResolveResponse)) return false;
        final AlertResolveResponse other = (AlertResolveResponse) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$id = this.getId();
        final java.lang.Object other$id = other.getId();
        if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
        final java.lang.Object this$resolvedBy = this.getResolvedBy();
        final java.lang.Object other$resolvedBy = other.getResolvedBy();
        if (this$resolvedBy == null ? other$resolvedBy != null : !this$resolvedBy.equals(other$resolvedBy)) return false;
        final java.lang.Object this$status = this.getStatus();
        final java.lang.Object other$status = other.getStatus();
        if (this$status == null ? other$status != null : !this$status.equals(other$status)) return false;
        final java.lang.Object this$resolvedAt = this.getResolvedAt();
        final java.lang.Object other$resolvedAt = other.getResolvedAt();
        if (this$resolvedAt == null ? other$resolvedAt != null : !this$resolvedAt.equals(other$resolvedAt)) return false;
        return true;
    }

    @java.lang.SuppressWarnings("all")
    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof AlertResolveResponse;
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
        final java.lang.Object $status = this.getStatus();
        result = result * PRIME + ($status == null ? 43 : $status.hashCode());
        final java.lang.Object $resolvedAt = this.getResolvedAt();
        result = result * PRIME + ($resolvedAt == null ? 43 : $resolvedAt.hashCode());
        return result;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public java.lang.String toString() {
        return "AlertResolveResponse(id=" + this.getId() + ", status=" + this.getStatus() + ", resolvedAt=" + this.getResolvedAt() + ", resolvedBy=" + this.getResolvedBy() + ")";
    }
}
