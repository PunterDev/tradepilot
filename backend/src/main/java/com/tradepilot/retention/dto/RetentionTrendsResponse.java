package com.tradepilot.retention.dto;

import java.util.List;

public class RetentionTrendsResponse {
    private List<RetentionTrendPointResponse> dailyActiveUsers;
    private List<RetentionTrendPointResponse> weeklyActiveUsers;
    private List<RetentionTrendPointResponse> monthlyActiveUsers;

    @java.lang.SuppressWarnings("all")
    RetentionTrendsResponse(final List<RetentionTrendPointResponse> dailyActiveUsers, final List<RetentionTrendPointResponse> weeklyActiveUsers, final List<RetentionTrendPointResponse> monthlyActiveUsers) {
        this.dailyActiveUsers = dailyActiveUsers;
        this.weeklyActiveUsers = weeklyActiveUsers;
        this.monthlyActiveUsers = monthlyActiveUsers;
    }


    @java.lang.SuppressWarnings("all")
    public static class RetentionTrendsResponseBuilder {
        @java.lang.SuppressWarnings("all")
        private List<RetentionTrendPointResponse> dailyActiveUsers;
        @java.lang.SuppressWarnings("all")
        private List<RetentionTrendPointResponse> weeklyActiveUsers;
        @java.lang.SuppressWarnings("all")
        private List<RetentionTrendPointResponse> monthlyActiveUsers;

        @java.lang.SuppressWarnings("all")
        RetentionTrendsResponseBuilder() {
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public RetentionTrendsResponse.RetentionTrendsResponseBuilder dailyActiveUsers(final List<RetentionTrendPointResponse> dailyActiveUsers) {
            this.dailyActiveUsers = dailyActiveUsers;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public RetentionTrendsResponse.RetentionTrendsResponseBuilder weeklyActiveUsers(final List<RetentionTrendPointResponse> weeklyActiveUsers) {
            this.weeklyActiveUsers = weeklyActiveUsers;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public RetentionTrendsResponse.RetentionTrendsResponseBuilder monthlyActiveUsers(final List<RetentionTrendPointResponse> monthlyActiveUsers) {
            this.monthlyActiveUsers = monthlyActiveUsers;
            return this;
        }

        @java.lang.SuppressWarnings("all")
        public RetentionTrendsResponse build() {
            return new RetentionTrendsResponse(this.dailyActiveUsers, this.weeklyActiveUsers, this.monthlyActiveUsers);
        }

        @java.lang.Override
        @java.lang.SuppressWarnings("all")
        public java.lang.String toString() {
            return "RetentionTrendsResponse.RetentionTrendsResponseBuilder(dailyActiveUsers=" + this.dailyActiveUsers + ", weeklyActiveUsers=" + this.weeklyActiveUsers + ", monthlyActiveUsers=" + this.monthlyActiveUsers + ")";
        }
    }

    @java.lang.SuppressWarnings("all")
    public static RetentionTrendsResponse.RetentionTrendsResponseBuilder builder() {
        return new RetentionTrendsResponse.RetentionTrendsResponseBuilder();
    }

    @java.lang.SuppressWarnings("all")
    public List<RetentionTrendPointResponse> getDailyActiveUsers() {
        return this.dailyActiveUsers;
    }

    @java.lang.SuppressWarnings("all")
    public List<RetentionTrendPointResponse> getWeeklyActiveUsers() {
        return this.weeklyActiveUsers;
    }

    @java.lang.SuppressWarnings("all")
    public List<RetentionTrendPointResponse> getMonthlyActiveUsers() {
        return this.monthlyActiveUsers;
    }

    @java.lang.SuppressWarnings("all")
    public void setDailyActiveUsers(final List<RetentionTrendPointResponse> dailyActiveUsers) {
        this.dailyActiveUsers = dailyActiveUsers;
    }

    @java.lang.SuppressWarnings("all")
    public void setWeeklyActiveUsers(final List<RetentionTrendPointResponse> weeklyActiveUsers) {
        this.weeklyActiveUsers = weeklyActiveUsers;
    }

    @java.lang.SuppressWarnings("all")
    public void setMonthlyActiveUsers(final List<RetentionTrendPointResponse> monthlyActiveUsers) {
        this.monthlyActiveUsers = monthlyActiveUsers;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof RetentionTrendsResponse)) return false;
        final RetentionTrendsResponse other = (RetentionTrendsResponse) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$dailyActiveUsers = this.getDailyActiveUsers();
        final java.lang.Object other$dailyActiveUsers = other.getDailyActiveUsers();
        if (this$dailyActiveUsers == null ? other$dailyActiveUsers != null : !this$dailyActiveUsers.equals(other$dailyActiveUsers)) return false;
        final java.lang.Object this$weeklyActiveUsers = this.getWeeklyActiveUsers();
        final java.lang.Object other$weeklyActiveUsers = other.getWeeklyActiveUsers();
        if (this$weeklyActiveUsers == null ? other$weeklyActiveUsers != null : !this$weeklyActiveUsers.equals(other$weeklyActiveUsers)) return false;
        final java.lang.Object this$monthlyActiveUsers = this.getMonthlyActiveUsers();
        final java.lang.Object other$monthlyActiveUsers = other.getMonthlyActiveUsers();
        if (this$monthlyActiveUsers == null ? other$monthlyActiveUsers != null : !this$monthlyActiveUsers.equals(other$monthlyActiveUsers)) return false;
        return true;
    }

    @java.lang.SuppressWarnings("all")
    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof RetentionTrendsResponse;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $dailyActiveUsers = this.getDailyActiveUsers();
        result = result * PRIME + ($dailyActiveUsers == null ? 43 : $dailyActiveUsers.hashCode());
        final java.lang.Object $weeklyActiveUsers = this.getWeeklyActiveUsers();
        result = result * PRIME + ($weeklyActiveUsers == null ? 43 : $weeklyActiveUsers.hashCode());
        final java.lang.Object $monthlyActiveUsers = this.getMonthlyActiveUsers();
        result = result * PRIME + ($monthlyActiveUsers == null ? 43 : $monthlyActiveUsers.hashCode());
        return result;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public java.lang.String toString() {
        return "RetentionTrendsResponse(dailyActiveUsers=" + this.getDailyActiveUsers() + ", weeklyActiveUsers=" + this.getWeeklyActiveUsers() + ", monthlyActiveUsers=" + this.getMonthlyActiveUsers() + ")";
    }
}
