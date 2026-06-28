package com.tradepilot.dto.admin;

import com.tradepilot.dto.account.TradingAccountResponse;
import java.util.List;

public class AdminUserDetailResponse {
    private AdminUserResponse user;
    private List<TradingAccountResponse> accounts;
    private List<AdminContactMessageResponse> contactMessages;
    private List<AdminRecentUpgradeResponse> auditLogs;

    @java.lang.SuppressWarnings("all")
    AdminUserDetailResponse(final AdminUserResponse user, final List<TradingAccountResponse> accounts, final List<AdminContactMessageResponse> contactMessages, final List<AdminRecentUpgradeResponse> auditLogs) {
        this.user = user;
        this.accounts = accounts;
        this.contactMessages = contactMessages;
        this.auditLogs = auditLogs;
    }


    @java.lang.SuppressWarnings("all")
    public static class AdminUserDetailResponseBuilder {
        @java.lang.SuppressWarnings("all")
        private AdminUserResponse user;
        @java.lang.SuppressWarnings("all")
        private List<TradingAccountResponse> accounts;
        @java.lang.SuppressWarnings("all")
        private List<AdminContactMessageResponse> contactMessages;
        @java.lang.SuppressWarnings("all")
        private List<AdminRecentUpgradeResponse> auditLogs;

        @java.lang.SuppressWarnings("all")
        AdminUserDetailResponseBuilder() {
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public AdminUserDetailResponse.AdminUserDetailResponseBuilder user(final AdminUserResponse user) {
            this.user = user;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public AdminUserDetailResponse.AdminUserDetailResponseBuilder accounts(final List<TradingAccountResponse> accounts) {
            this.accounts = accounts;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public AdminUserDetailResponse.AdminUserDetailResponseBuilder contactMessages(final List<AdminContactMessageResponse> contactMessages) {
            this.contactMessages = contactMessages;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public AdminUserDetailResponse.AdminUserDetailResponseBuilder auditLogs(final List<AdminRecentUpgradeResponse> auditLogs) {
            this.auditLogs = auditLogs;
            return this;
        }

        @java.lang.SuppressWarnings("all")
        public AdminUserDetailResponse build() {
            return new AdminUserDetailResponse(this.user, this.accounts, this.contactMessages, this.auditLogs);
        }

        @java.lang.Override
        @java.lang.SuppressWarnings("all")
        public java.lang.String toString() {
            return "AdminUserDetailResponse.AdminUserDetailResponseBuilder(user=" + this.user + ", accounts=" + this.accounts + ", contactMessages=" + this.contactMessages + ", auditLogs=" + this.auditLogs + ")";
        }
    }

    @java.lang.SuppressWarnings("all")
    public static AdminUserDetailResponse.AdminUserDetailResponseBuilder builder() {
        return new AdminUserDetailResponse.AdminUserDetailResponseBuilder();
    }

    @java.lang.SuppressWarnings("all")
    public AdminUserResponse getUser() {
        return this.user;
    }

    @java.lang.SuppressWarnings("all")
    public List<TradingAccountResponse> getAccounts() {
        return this.accounts;
    }

    @java.lang.SuppressWarnings("all")
    public List<AdminContactMessageResponse> getContactMessages() {
        return this.contactMessages;
    }

    @java.lang.SuppressWarnings("all")
    public List<AdminRecentUpgradeResponse> getAuditLogs() {
        return this.auditLogs;
    }

    @java.lang.SuppressWarnings("all")
    public void setUser(final AdminUserResponse user) {
        this.user = user;
    }

    @java.lang.SuppressWarnings("all")
    public void setAccounts(final List<TradingAccountResponse> accounts) {
        this.accounts = accounts;
    }

    @java.lang.SuppressWarnings("all")
    public void setContactMessages(final List<AdminContactMessageResponse> contactMessages) {
        this.contactMessages = contactMessages;
    }

    @java.lang.SuppressWarnings("all")
    public void setAuditLogs(final List<AdminRecentUpgradeResponse> auditLogs) {
        this.auditLogs = auditLogs;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof AdminUserDetailResponse)) return false;
        final AdminUserDetailResponse other = (AdminUserDetailResponse) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$user = this.getUser();
        final java.lang.Object other$user = other.getUser();
        if (this$user == null ? other$user != null : !this$user.equals(other$user)) return false;
        final java.lang.Object this$accounts = this.getAccounts();
        final java.lang.Object other$accounts = other.getAccounts();
        if (this$accounts == null ? other$accounts != null : !this$accounts.equals(other$accounts)) return false;
        final java.lang.Object this$contactMessages = this.getContactMessages();
        final java.lang.Object other$contactMessages = other.getContactMessages();
        if (this$contactMessages == null ? other$contactMessages != null : !this$contactMessages.equals(other$contactMessages)) return false;
        final java.lang.Object this$auditLogs = this.getAuditLogs();
        final java.lang.Object other$auditLogs = other.getAuditLogs();
        if (this$auditLogs == null ? other$auditLogs != null : !this$auditLogs.equals(other$auditLogs)) return false;
        return true;
    }

    @java.lang.SuppressWarnings("all")
    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof AdminUserDetailResponse;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $user = this.getUser();
        result = result * PRIME + ($user == null ? 43 : $user.hashCode());
        final java.lang.Object $accounts = this.getAccounts();
        result = result * PRIME + ($accounts == null ? 43 : $accounts.hashCode());
        final java.lang.Object $contactMessages = this.getContactMessages();
        result = result * PRIME + ($contactMessages == null ? 43 : $contactMessages.hashCode());
        final java.lang.Object $auditLogs = this.getAuditLogs();
        result = result * PRIME + ($auditLogs == null ? 43 : $auditLogs.hashCode());
        return result;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public java.lang.String toString() {
        return "AdminUserDetailResponse(user=" + this.getUser() + ", accounts=" + this.getAccounts() + ", contactMessages=" + this.getContactMessages() + ", auditLogs=" + this.getAuditLogs() + ")";
    }
}
