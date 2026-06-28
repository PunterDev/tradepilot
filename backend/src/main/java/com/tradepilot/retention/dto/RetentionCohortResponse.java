package com.tradepilot.retention.dto;

public class RetentionCohortResponse {
    private String cohortMonth;
    private long users;
    private double retained7d;
    private double retained30d;

    @java.lang.SuppressWarnings("all")
    RetentionCohortResponse(final String cohortMonth, final long users, final double retained7d, final double retained30d) {
        this.cohortMonth = cohortMonth;
        this.users = users;
        this.retained7d = retained7d;
        this.retained30d = retained30d;
    }


    @java.lang.SuppressWarnings("all")
    public static class RetentionCohortResponseBuilder {
        @java.lang.SuppressWarnings("all")
        private String cohortMonth;
        @java.lang.SuppressWarnings("all")
        private long users;
        @java.lang.SuppressWarnings("all")
        private double retained7d;
        @java.lang.SuppressWarnings("all")
        private double retained30d;

        @java.lang.SuppressWarnings("all")
        RetentionCohortResponseBuilder() {
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public RetentionCohortResponse.RetentionCohortResponseBuilder cohortMonth(final String cohortMonth) {
            this.cohortMonth = cohortMonth;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public RetentionCohortResponse.RetentionCohortResponseBuilder users(final long users) {
            this.users = users;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public RetentionCohortResponse.RetentionCohortResponseBuilder retained7d(final double retained7d) {
            this.retained7d = retained7d;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public RetentionCohortResponse.RetentionCohortResponseBuilder retained30d(final double retained30d) {
            this.retained30d = retained30d;
            return this;
        }

        @java.lang.SuppressWarnings("all")
        public RetentionCohortResponse build() {
            return new RetentionCohortResponse(this.cohortMonth, this.users, this.retained7d, this.retained30d);
        }

        @java.lang.Override
        @java.lang.SuppressWarnings("all")
        public java.lang.String toString() {
            return "RetentionCohortResponse.RetentionCohortResponseBuilder(cohortMonth=" + this.cohortMonth + ", users=" + this.users + ", retained7d=" + this.retained7d + ", retained30d=" + this.retained30d + ")";
        }
    }

    @java.lang.SuppressWarnings("all")
    public static RetentionCohortResponse.RetentionCohortResponseBuilder builder() {
        return new RetentionCohortResponse.RetentionCohortResponseBuilder();
    }

    @java.lang.SuppressWarnings("all")
    public String getCohortMonth() {
        return this.cohortMonth;
    }

    @java.lang.SuppressWarnings("all")
    public long getUsers() {
        return this.users;
    }

    @java.lang.SuppressWarnings("all")
    public double getRetained7d() {
        return this.retained7d;
    }

    @java.lang.SuppressWarnings("all")
    public double getRetained30d() {
        return this.retained30d;
    }

    @java.lang.SuppressWarnings("all")
    public void setCohortMonth(final String cohortMonth) {
        this.cohortMonth = cohortMonth;
    }

    @java.lang.SuppressWarnings("all")
    public void setUsers(final long users) {
        this.users = users;
    }

    @java.lang.SuppressWarnings("all")
    public void setRetained7d(final double retained7d) {
        this.retained7d = retained7d;
    }

    @java.lang.SuppressWarnings("all")
    public void setRetained30d(final double retained30d) {
        this.retained30d = retained30d;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof RetentionCohortResponse)) return false;
        final RetentionCohortResponse other = (RetentionCohortResponse) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        if (this.getUsers() != other.getUsers()) return false;
        if (java.lang.Double.compare(this.getRetained7d(), other.getRetained7d()) != 0) return false;
        if (java.lang.Double.compare(this.getRetained30d(), other.getRetained30d()) != 0) return false;
        final java.lang.Object this$cohortMonth = this.getCohortMonth();
        final java.lang.Object other$cohortMonth = other.getCohortMonth();
        if (this$cohortMonth == null ? other$cohortMonth != null : !this$cohortMonth.equals(other$cohortMonth)) return false;
        return true;
    }

    @java.lang.SuppressWarnings("all")
    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof RetentionCohortResponse;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final long $users = this.getUsers();
        result = result * PRIME + (int) ($users >>> 32 ^ $users);
        final long $retained7d = java.lang.Double.doubleToLongBits(this.getRetained7d());
        result = result * PRIME + (int) ($retained7d >>> 32 ^ $retained7d);
        final long $retained30d = java.lang.Double.doubleToLongBits(this.getRetained30d());
        result = result * PRIME + (int) ($retained30d >>> 32 ^ $retained30d);
        final java.lang.Object $cohortMonth = this.getCohortMonth();
        result = result * PRIME + ($cohortMonth == null ? 43 : $cohortMonth.hashCode());
        return result;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public java.lang.String toString() {
        return "RetentionCohortResponse(cohortMonth=" + this.getCohortMonth() + ", users=" + this.getUsers() + ", retained7d=" + this.getRetained7d() + ", retained30d=" + this.getRetained30d() + ")";
    }
}
