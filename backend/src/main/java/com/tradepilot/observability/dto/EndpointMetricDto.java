package com.tradepilot.observability.dto;

public class EndpointMetricDto {
    private String endpoint;
    private long requestCount;
    private double avgDurationMs;
    private long maxDurationMs;

    @java.lang.SuppressWarnings("all")
    EndpointMetricDto(final String endpoint, final long requestCount, final double avgDurationMs, final long maxDurationMs) {
        this.endpoint = endpoint;
        this.requestCount = requestCount;
        this.avgDurationMs = avgDurationMs;
        this.maxDurationMs = maxDurationMs;
    }


    @java.lang.SuppressWarnings("all")
    public static class EndpointMetricDtoBuilder {
        @java.lang.SuppressWarnings("all")
        private String endpoint;
        @java.lang.SuppressWarnings("all")
        private long requestCount;
        @java.lang.SuppressWarnings("all")
        private double avgDurationMs;
        @java.lang.SuppressWarnings("all")
        private long maxDurationMs;

        @java.lang.SuppressWarnings("all")
        EndpointMetricDtoBuilder() {
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public EndpointMetricDto.EndpointMetricDtoBuilder endpoint(final String endpoint) {
            this.endpoint = endpoint;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public EndpointMetricDto.EndpointMetricDtoBuilder requestCount(final long requestCount) {
            this.requestCount = requestCount;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public EndpointMetricDto.EndpointMetricDtoBuilder avgDurationMs(final double avgDurationMs) {
            this.avgDurationMs = avgDurationMs;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public EndpointMetricDto.EndpointMetricDtoBuilder maxDurationMs(final long maxDurationMs) {
            this.maxDurationMs = maxDurationMs;
            return this;
        }

        @java.lang.SuppressWarnings("all")
        public EndpointMetricDto build() {
            return new EndpointMetricDto(this.endpoint, this.requestCount, this.avgDurationMs, this.maxDurationMs);
        }

        @java.lang.Override
        @java.lang.SuppressWarnings("all")
        public java.lang.String toString() {
            return "EndpointMetricDto.EndpointMetricDtoBuilder(endpoint=" + this.endpoint + ", requestCount=" + this.requestCount + ", avgDurationMs=" + this.avgDurationMs + ", maxDurationMs=" + this.maxDurationMs + ")";
        }
    }

    @java.lang.SuppressWarnings("all")
    public static EndpointMetricDto.EndpointMetricDtoBuilder builder() {
        return new EndpointMetricDto.EndpointMetricDtoBuilder();
    }

    @java.lang.SuppressWarnings("all")
    public String getEndpoint() {
        return this.endpoint;
    }

    @java.lang.SuppressWarnings("all")
    public long getRequestCount() {
        return this.requestCount;
    }

    @java.lang.SuppressWarnings("all")
    public double getAvgDurationMs() {
        return this.avgDurationMs;
    }

    @java.lang.SuppressWarnings("all")
    public long getMaxDurationMs() {
        return this.maxDurationMs;
    }

    @java.lang.SuppressWarnings("all")
    public void setEndpoint(final String endpoint) {
        this.endpoint = endpoint;
    }

    @java.lang.SuppressWarnings("all")
    public void setRequestCount(final long requestCount) {
        this.requestCount = requestCount;
    }

    @java.lang.SuppressWarnings("all")
    public void setAvgDurationMs(final double avgDurationMs) {
        this.avgDurationMs = avgDurationMs;
    }

    @java.lang.SuppressWarnings("all")
    public void setMaxDurationMs(final long maxDurationMs) {
        this.maxDurationMs = maxDurationMs;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof EndpointMetricDto)) return false;
        final EndpointMetricDto other = (EndpointMetricDto) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        if (this.getRequestCount() != other.getRequestCount()) return false;
        if (java.lang.Double.compare(this.getAvgDurationMs(), other.getAvgDurationMs()) != 0) return false;
        if (this.getMaxDurationMs() != other.getMaxDurationMs()) return false;
        final java.lang.Object this$endpoint = this.getEndpoint();
        final java.lang.Object other$endpoint = other.getEndpoint();
        if (this$endpoint == null ? other$endpoint != null : !this$endpoint.equals(other$endpoint)) return false;
        return true;
    }

    @java.lang.SuppressWarnings("all")
    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof EndpointMetricDto;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final long $requestCount = this.getRequestCount();
        result = result * PRIME + (int) ($requestCount >>> 32 ^ $requestCount);
        final long $avgDurationMs = java.lang.Double.doubleToLongBits(this.getAvgDurationMs());
        result = result * PRIME + (int) ($avgDurationMs >>> 32 ^ $avgDurationMs);
        final long $maxDurationMs = this.getMaxDurationMs();
        result = result * PRIME + (int) ($maxDurationMs >>> 32 ^ $maxDurationMs);
        final java.lang.Object $endpoint = this.getEndpoint();
        result = result * PRIME + ($endpoint == null ? 43 : $endpoint.hashCode());
        return result;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public java.lang.String toString() {
        return "EndpointMetricDto(endpoint=" + this.getEndpoint() + ", requestCount=" + this.getRequestCount() + ", avgDurationMs=" + this.getAvgDurationMs() + ", maxDurationMs=" + this.getMaxDurationMs() + ")";
    }
}
