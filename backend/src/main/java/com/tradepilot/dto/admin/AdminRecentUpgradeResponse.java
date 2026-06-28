package com.tradepilot.dto.admin;

import com.tradepilot.enums.AdminAuditAction;
import java.time.LocalDateTime;

public class AdminRecentUpgradeResponse {
    private Long id;
    private Long adminUserId;
    private Long targetUserId;
    private String targetUserEmail;
    private String targetUserName;
    private AdminAuditAction action;
    private String reason;
    private String source;
    private String beforeValue;
    private String afterValue;
    private LocalDateTime createdAt;

    @java.lang.SuppressWarnings("all")
    AdminRecentUpgradeResponse(final Long id, final Long adminUserId, final Long targetUserId, final String targetUserEmail, final String targetUserName, final AdminAuditAction action, final String reason, final String source, final String beforeValue, final String afterValue, final LocalDateTime createdAt) {
        this.id = id;
        this.adminUserId = adminUserId;
        this.targetUserId = targetUserId;
        this.targetUserEmail = targetUserEmail;
        this.targetUserName = targetUserName;
        this.action = action;
        this.reason = reason;
        this.source = source;
        this.beforeValue = beforeValue;
        this.afterValue = afterValue;
        this.createdAt = createdAt;
    }


    @java.lang.SuppressWarnings("all")
    public static class AdminRecentUpgradeResponseBuilder {
        @java.lang.SuppressWarnings("all")
        private Long id;
        @java.lang.SuppressWarnings("all")
        private Long adminUserId;
        @java.lang.SuppressWarnings("all")
        private Long targetUserId;
        @java.lang.SuppressWarnings("all")
        private String targetUserEmail;
        @java.lang.SuppressWarnings("all")
        private String targetUserName;
        @java.lang.SuppressWarnings("all")
        private AdminAuditAction action;
        @java.lang.SuppressWarnings("all")
        private String reason;
        @java.lang.SuppressWarnings("all")
        private String source;
        @java.lang.SuppressWarnings("all")
        private String beforeValue;
        @java.lang.SuppressWarnings("all")
        private String afterValue;
        @java.lang.SuppressWarnings("all")
        private LocalDateTime createdAt;

        @java.lang.SuppressWarnings("all")
        AdminRecentUpgradeResponseBuilder() {
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public AdminRecentUpgradeResponse.AdminRecentUpgradeResponseBuilder id(final Long id) {
            this.id = id;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public AdminRecentUpgradeResponse.AdminRecentUpgradeResponseBuilder adminUserId(final Long adminUserId) {
            this.adminUserId = adminUserId;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public AdminRecentUpgradeResponse.AdminRecentUpgradeResponseBuilder targetUserId(final Long targetUserId) {
            this.targetUserId = targetUserId;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public AdminRecentUpgradeResponse.AdminRecentUpgradeResponseBuilder targetUserEmail(final String targetUserEmail) {
            this.targetUserEmail = targetUserEmail;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public AdminRecentUpgradeResponse.AdminRecentUpgradeResponseBuilder targetUserName(final String targetUserName) {
            this.targetUserName = targetUserName;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public AdminRecentUpgradeResponse.AdminRecentUpgradeResponseBuilder action(final AdminAuditAction action) {
            this.action = action;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public AdminRecentUpgradeResponse.AdminRecentUpgradeResponseBuilder reason(final String reason) {
            this.reason = reason;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public AdminRecentUpgradeResponse.AdminRecentUpgradeResponseBuilder source(final String source) {
            this.source = source;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public AdminRecentUpgradeResponse.AdminRecentUpgradeResponseBuilder beforeValue(final String beforeValue) {
            this.beforeValue = beforeValue;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public AdminRecentUpgradeResponse.AdminRecentUpgradeResponseBuilder afterValue(final String afterValue) {
            this.afterValue = afterValue;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public AdminRecentUpgradeResponse.AdminRecentUpgradeResponseBuilder createdAt(final LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        @java.lang.SuppressWarnings("all")
        public AdminRecentUpgradeResponse build() {
            return new AdminRecentUpgradeResponse(this.id, this.adminUserId, this.targetUserId, this.targetUserEmail, this.targetUserName, this.action, this.reason, this.source, this.beforeValue, this.afterValue, this.createdAt);
        }

        @java.lang.Override
        @java.lang.SuppressWarnings("all")
        public java.lang.String toString() {
            return "AdminRecentUpgradeResponse.AdminRecentUpgradeResponseBuilder(id=" + this.id + ", adminUserId=" + this.adminUserId + ", targetUserId=" + this.targetUserId + ", targetUserEmail=" + this.targetUserEmail + ", targetUserName=" + this.targetUserName + ", action=" + this.action + ", reason=" + this.reason + ", source=" + this.source + ", beforeValue=" + this.beforeValue + ", afterValue=" + this.afterValue + ", createdAt=" + this.createdAt + ")";
        }
    }

    @java.lang.SuppressWarnings("all")
    public static AdminRecentUpgradeResponse.AdminRecentUpgradeResponseBuilder builder() {
        return new AdminRecentUpgradeResponse.AdminRecentUpgradeResponseBuilder();
    }

    @java.lang.SuppressWarnings("all")
    public Long getId() {
        return this.id;
    }

    @java.lang.SuppressWarnings("all")
    public Long getAdminUserId() {
        return this.adminUserId;
    }

    @java.lang.SuppressWarnings("all")
    public Long getTargetUserId() {
        return this.targetUserId;
    }

    @java.lang.SuppressWarnings("all")
    public String getTargetUserEmail() {
        return this.targetUserEmail;
    }

    @java.lang.SuppressWarnings("all")
    public String getTargetUserName() {
        return this.targetUserName;
    }

    @java.lang.SuppressWarnings("all")
    public AdminAuditAction getAction() {
        return this.action;
    }

    @java.lang.SuppressWarnings("all")
    public String getReason() {
        return this.reason;
    }

    @java.lang.SuppressWarnings("all")
    public String getSource() {
        return this.source;
    }

    @java.lang.SuppressWarnings("all")
    public String getBeforeValue() {
        return this.beforeValue;
    }

    @java.lang.SuppressWarnings("all")
    public String getAfterValue() {
        return this.afterValue;
    }

    @java.lang.SuppressWarnings("all")
    public LocalDateTime getCreatedAt() {
        return this.createdAt;
    }

    @java.lang.SuppressWarnings("all")
    public void setId(final Long id) {
        this.id = id;
    }

    @java.lang.SuppressWarnings("all")
    public void setAdminUserId(final Long adminUserId) {
        this.adminUserId = adminUserId;
    }

    @java.lang.SuppressWarnings("all")
    public void setTargetUserId(final Long targetUserId) {
        this.targetUserId = targetUserId;
    }

    @java.lang.SuppressWarnings("all")
    public void setTargetUserEmail(final String targetUserEmail) {
        this.targetUserEmail = targetUserEmail;
    }

    @java.lang.SuppressWarnings("all")
    public void setTargetUserName(final String targetUserName) {
        this.targetUserName = targetUserName;
    }

    @java.lang.SuppressWarnings("all")
    public void setAction(final AdminAuditAction action) {
        this.action = action;
    }

    @java.lang.SuppressWarnings("all")
    public void setReason(final String reason) {
        this.reason = reason;
    }

    @java.lang.SuppressWarnings("all")
    public void setSource(final String source) {
        this.source = source;
    }

    @java.lang.SuppressWarnings("all")
    public void setBeforeValue(final String beforeValue) {
        this.beforeValue = beforeValue;
    }

    @java.lang.SuppressWarnings("all")
    public void setAfterValue(final String afterValue) {
        this.afterValue = afterValue;
    }

    @java.lang.SuppressWarnings("all")
    public void setCreatedAt(final LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof AdminRecentUpgradeResponse)) return false;
        final AdminRecentUpgradeResponse other = (AdminRecentUpgradeResponse) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$id = this.getId();
        final java.lang.Object other$id = other.getId();
        if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
        final java.lang.Object this$adminUserId = this.getAdminUserId();
        final java.lang.Object other$adminUserId = other.getAdminUserId();
        if (this$adminUserId == null ? other$adminUserId != null : !this$adminUserId.equals(other$adminUserId)) return false;
        final java.lang.Object this$targetUserId = this.getTargetUserId();
        final java.lang.Object other$targetUserId = other.getTargetUserId();
        if (this$targetUserId == null ? other$targetUserId != null : !this$targetUserId.equals(other$targetUserId)) return false;
        final java.lang.Object this$targetUserEmail = this.getTargetUserEmail();
        final java.lang.Object other$targetUserEmail = other.getTargetUserEmail();
        if (this$targetUserEmail == null ? other$targetUserEmail != null : !this$targetUserEmail.equals(other$targetUserEmail)) return false;
        final java.lang.Object this$targetUserName = this.getTargetUserName();
        final java.lang.Object other$targetUserName = other.getTargetUserName();
        if (this$targetUserName == null ? other$targetUserName != null : !this$targetUserName.equals(other$targetUserName)) return false;
        final java.lang.Object this$action = this.getAction();
        final java.lang.Object other$action = other.getAction();
        if (this$action == null ? other$action != null : !this$action.equals(other$action)) return false;
        final java.lang.Object this$reason = this.getReason();
        final java.lang.Object other$reason = other.getReason();
        if (this$reason == null ? other$reason != null : !this$reason.equals(other$reason)) return false;
        final java.lang.Object this$source = this.getSource();
        final java.lang.Object other$source = other.getSource();
        if (this$source == null ? other$source != null : !this$source.equals(other$source)) return false;
        final java.lang.Object this$beforeValue = this.getBeforeValue();
        final java.lang.Object other$beforeValue = other.getBeforeValue();
        if (this$beforeValue == null ? other$beforeValue != null : !this$beforeValue.equals(other$beforeValue)) return false;
        final java.lang.Object this$afterValue = this.getAfterValue();
        final java.lang.Object other$afterValue = other.getAfterValue();
        if (this$afterValue == null ? other$afterValue != null : !this$afterValue.equals(other$afterValue)) return false;
        final java.lang.Object this$createdAt = this.getCreatedAt();
        final java.lang.Object other$createdAt = other.getCreatedAt();
        if (this$createdAt == null ? other$createdAt != null : !this$createdAt.equals(other$createdAt)) return false;
        return true;
    }

    @java.lang.SuppressWarnings("all")
    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof AdminRecentUpgradeResponse;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $id = this.getId();
        result = result * PRIME + ($id == null ? 43 : $id.hashCode());
        final java.lang.Object $adminUserId = this.getAdminUserId();
        result = result * PRIME + ($adminUserId == null ? 43 : $adminUserId.hashCode());
        final java.lang.Object $targetUserId = this.getTargetUserId();
        result = result * PRIME + ($targetUserId == null ? 43 : $targetUserId.hashCode());
        final java.lang.Object $targetUserEmail = this.getTargetUserEmail();
        result = result * PRIME + ($targetUserEmail == null ? 43 : $targetUserEmail.hashCode());
        final java.lang.Object $targetUserName = this.getTargetUserName();
        result = result * PRIME + ($targetUserName == null ? 43 : $targetUserName.hashCode());
        final java.lang.Object $action = this.getAction();
        result = result * PRIME + ($action == null ? 43 : $action.hashCode());
        final java.lang.Object $reason = this.getReason();
        result = result * PRIME + ($reason == null ? 43 : $reason.hashCode());
        final java.lang.Object $source = this.getSource();
        result = result * PRIME + ($source == null ? 43 : $source.hashCode());
        final java.lang.Object $beforeValue = this.getBeforeValue();
        result = result * PRIME + ($beforeValue == null ? 43 : $beforeValue.hashCode());
        final java.lang.Object $afterValue = this.getAfterValue();
        result = result * PRIME + ($afterValue == null ? 43 : $afterValue.hashCode());
        final java.lang.Object $createdAt = this.getCreatedAt();
        result = result * PRIME + ($createdAt == null ? 43 : $createdAt.hashCode());
        return result;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public java.lang.String toString() {
        return "AdminRecentUpgradeResponse(id=" + this.getId() + ", adminUserId=" + this.getAdminUserId() + ", targetUserId=" + this.getTargetUserId() + ", targetUserEmail=" + this.getTargetUserEmail() + ", targetUserName=" + this.getTargetUserName() + ", action=" + this.getAction() + ", reason=" + this.getReason() + ", source=" + this.getSource() + ", beforeValue=" + this.getBeforeValue() + ", afterValue=" + this.getAfterValue() + ", createdAt=" + this.getCreatedAt() + ")";
    }
}
