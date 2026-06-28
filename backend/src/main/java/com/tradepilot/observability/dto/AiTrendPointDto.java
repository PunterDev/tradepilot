package com.tradepilot.observability.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class AiTrendPointDto {
    private LocalDate date;
    private long tokens;
    private BigDecimal estimatedCost;

    @java.lang.SuppressWarnings("all")
    AiTrendPointDto(final LocalDate date, final long tokens, final BigDecimal estimatedCost) {
        this.date = date;
        this.tokens = tokens;
        this.estimatedCost = estimatedCost;
    }


    @java.lang.SuppressWarnings("all")
    public static class AiTrendPointDtoBuilder {
        @java.lang.SuppressWarnings("all")
        private LocalDate date;
        @java.lang.SuppressWarnings("all")
        private long tokens;
        @java.lang.SuppressWarnings("all")
        private BigDecimal estimatedCost;

        @java.lang.SuppressWarnings("all")
        AiTrendPointDtoBuilder() {
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public AiTrendPointDto.AiTrendPointDtoBuilder date(final LocalDate date) {
            this.date = date;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public AiTrendPointDto.AiTrendPointDtoBuilder tokens(final long tokens) {
            this.tokens = tokens;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public AiTrendPointDto.AiTrendPointDtoBuilder estimatedCost(final BigDecimal estimatedCost) {
            this.estimatedCost = estimatedCost;
            return this;
        }

        @java.lang.SuppressWarnings("all")
        public AiTrendPointDto build() {
            return new AiTrendPointDto(this.date, this.tokens, this.estimatedCost);
        }

        @java.lang.Override
        @java.lang.SuppressWarnings("all")
        public java.lang.String toString() {
            return "AiTrendPointDto.AiTrendPointDtoBuilder(date=" + this.date + ", tokens=" + this.tokens + ", estimatedCost=" + this.estimatedCost + ")";
        }
    }

    @java.lang.SuppressWarnings("all")
    public static AiTrendPointDto.AiTrendPointDtoBuilder builder() {
        return new AiTrendPointDto.AiTrendPointDtoBuilder();
    }

    @java.lang.SuppressWarnings("all")
    public LocalDate getDate() {
        return this.date;
    }

    @java.lang.SuppressWarnings("all")
    public long getTokens() {
        return this.tokens;
    }

    @java.lang.SuppressWarnings("all")
    public BigDecimal getEstimatedCost() {
        return this.estimatedCost;
    }

    @java.lang.SuppressWarnings("all")
    public void setDate(final LocalDate date) {
        this.date = date;
    }

    @java.lang.SuppressWarnings("all")
    public void setTokens(final long tokens) {
        this.tokens = tokens;
    }

    @java.lang.SuppressWarnings("all")
    public void setEstimatedCost(final BigDecimal estimatedCost) {
        this.estimatedCost = estimatedCost;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof AiTrendPointDto)) return false;
        final AiTrendPointDto other = (AiTrendPointDto) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        if (this.getTokens() != other.getTokens()) return false;
        final java.lang.Object this$date = this.getDate();
        final java.lang.Object other$date = other.getDate();
        if (this$date == null ? other$date != null : !this$date.equals(other$date)) return false;
        final java.lang.Object this$estimatedCost = this.getEstimatedCost();
        final java.lang.Object other$estimatedCost = other.getEstimatedCost();
        if (this$estimatedCost == null ? other$estimatedCost != null : !this$estimatedCost.equals(other$estimatedCost)) return false;
        return true;
    }

    @java.lang.SuppressWarnings("all")
    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof AiTrendPointDto;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final long $tokens = this.getTokens();
        result = result * PRIME + (int) ($tokens >>> 32 ^ $tokens);
        final java.lang.Object $date = this.getDate();
        result = result * PRIME + ($date == null ? 43 : $date.hashCode());
        final java.lang.Object $estimatedCost = this.getEstimatedCost();
        result = result * PRIME + ($estimatedCost == null ? 43 : $estimatedCost.hashCode());
        return result;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public java.lang.String toString() {
        return "AiTrendPointDto(date=" + this.getDate() + ", tokens=" + this.getTokens() + ", estimatedCost=" + this.getEstimatedCost() + ")";
    }
}
