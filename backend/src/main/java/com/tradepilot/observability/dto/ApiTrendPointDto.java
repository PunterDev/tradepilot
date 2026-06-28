package com.tradepilot.observability.dto;

import java.time.LocalDate;

public class ApiTrendPointDto {
    private LocalDate date;
    private long requests;
    private double avgDurationMs;
    private double errorRate;

    @java.lang.SuppressWarnings("all")
    ApiTrendPointDto(final LocalDate date, final long requests, final double avgDurationMs, final double errorRate) {
        this.date = date;
        this.requests = requests;
        this.avgDurationMs = avgDurationMs;
        this.errorRate = errorRate;
    }


    @java.lang.SuppressWarnings("all")
    public static class ApiTrendPointDtoBuilder {
        @java.lang.SuppressWarnings("all")
        private LocalDate date;
        @java.lang.SuppressWarnings("all")
        private long requests;
        @java.lang.SuppressWarnings("all")
        private double avgDurationMs;
        @java.lang.SuppressWarnings("all")
        private double errorRate;

        @java.lang.SuppressWarnings("all")
        ApiTrendPointDtoBuilder() {
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public ApiTrendPointDto.ApiTrendPointDtoBuilder date(final LocalDate date) {
            this.date = date;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public ApiTrendPointDto.ApiTrendPointDtoBuilder requests(final long requests) {
            this.requests = requests;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public ApiTrendPointDto.ApiTrendPointDtoBuilder avgDurationMs(final double avgDurationMs) {
            this.avgDurationMs = avgDurationMs;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public ApiTrendPointDto.ApiTrendPointDtoBuilder errorRate(final double errorRate) {
            this.errorRate = errorRate;
            return this;
        }

        @java.lang.SuppressWarnings("all")
        public ApiTrendPointDto build() {
            return new ApiTrendPointDto(this.date, this.requests, this.avgDurationMs, this.errorRate);
        }

        @java.lang.Override
        @java.lang.SuppressWarnings("all")
        public java.lang.String toString() {
            return "ApiTrendPointDto.ApiTrendPointDtoBuilder(date=" + this.date + ", requests=" + this.requests + ", avgDurationMs=" + this.avgDurationMs + ", errorRate=" + this.errorRate + ")";
        }
    }

    @java.lang.SuppressWarnings("all")
    public static ApiTrendPointDto.ApiTrendPointDtoBuilder builder() {
        return new ApiTrendPointDto.ApiTrendPointDtoBuilder();
    }

    @java.lang.SuppressWarnings("all")
    public LocalDate getDate() {
        return this.date;
    }

    @java.lang.SuppressWarnings("all")
    public long getRequests() {
        return this.requests;
    }

    @java.lang.SuppressWarnings("all")
    public double getAvgDurationMs() {
        return this.avgDurationMs;
    }

    @java.lang.SuppressWarnings("all")
    public double getErrorRate() {
        return this.errorRate;
    }

    @java.lang.SuppressWarnings("all")
    public void setDate(final LocalDate date) {
        this.date = date;
    }

    @java.lang.SuppressWarnings("all")
    public void setRequests(final long requests) {
        this.requests = requests;
    }

    @java.lang.SuppressWarnings("all")
    public void setAvgDurationMs(final double avgDurationMs) {
        this.avgDurationMs = avgDurationMs;
    }

    @java.lang.SuppressWarnings("all")
    public void setErrorRate(final double errorRate) {
        this.errorRate = errorRate;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof ApiTrendPointDto)) return false;
        final ApiTrendPointDto other = (ApiTrendPointDto) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        if (this.getRequests() != other.getRequests()) return false;
        if (java.lang.Double.compare(this.getAvgDurationMs(), other.getAvgDurationMs()) != 0) return false;
        if (java.lang.Double.compare(this.getErrorRate(), other.getErrorRate()) != 0) return false;
        final java.lang.Object this$date = this.getDate();
        final java.lang.Object other$date = other.getDate();
        if (this$date == null ? other$date != null : !this$date.equals(other$date)) return false;
        return true;
    }

    @java.lang.SuppressWarnings("all")
    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof ApiTrendPointDto;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final long $requests = this.getRequests();
        result = result * PRIME + (int) ($requests >>> 32 ^ $requests);
        final long $avgDurationMs = java.lang.Double.doubleToLongBits(this.getAvgDurationMs());
        result = result * PRIME + (int) ($avgDurationMs >>> 32 ^ $avgDurationMs);
        final long $errorRate = java.lang.Double.doubleToLongBits(this.getErrorRate());
        result = result * PRIME + (int) ($errorRate >>> 32 ^ $errorRate);
        final java.lang.Object $date = this.getDate();
        result = result * PRIME + ($date == null ? 43 : $date.hashCode());
        return result;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public java.lang.String toString() {
        return "ApiTrendPointDto(date=" + this.getDate() + ", requests=" + this.getRequests() + ", avgDurationMs=" + this.getAvgDurationMs() + ", errorRate=" + this.getErrorRate() + ")";
    }
}
