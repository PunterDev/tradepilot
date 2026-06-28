package com.tradepilot.dto.admin;

import com.tradepilot.enums.ContactMessageStatus;
import java.time.LocalDateTime;

public class AdminContactMessageResponse {
    private Long id;
    private String name;
    private String email;
    private String subject;
    private String message;
    private ContactMessageStatus status;
    private boolean emailSent;
    private String emailError;
    private String remoteAddress;
    private String userAgent;
    private String adminNotes;
    private String leadSource;
    private LocalDateTime repliedAt;
    private LocalDateTime lastViewedAt;
    private String replyBody;
    private String replySubject;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @java.lang.SuppressWarnings("all")
    AdminContactMessageResponse(final Long id, final String name, final String email, final String subject, final String message, final ContactMessageStatus status, final boolean emailSent, final String emailError, final String remoteAddress, final String userAgent, final String adminNotes, final String leadSource, final LocalDateTime repliedAt, final LocalDateTime lastViewedAt, final String replyBody, final String replySubject, final LocalDateTime createdAt, final LocalDateTime updatedAt) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.subject = subject;
        this.message = message;
        this.status = status;
        this.emailSent = emailSent;
        this.emailError = emailError;
        this.remoteAddress = remoteAddress;
        this.userAgent = userAgent;
        this.adminNotes = adminNotes;
        this.leadSource = leadSource;
        this.repliedAt = repliedAt;
        this.lastViewedAt = lastViewedAt;
        this.replyBody = replyBody;
        this.replySubject = replySubject;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }


    @java.lang.SuppressWarnings("all")
    public static class AdminContactMessageResponseBuilder {
        @java.lang.SuppressWarnings("all")
        private Long id;
        @java.lang.SuppressWarnings("all")
        private String name;
        @java.lang.SuppressWarnings("all")
        private String email;
        @java.lang.SuppressWarnings("all")
        private String subject;
        @java.lang.SuppressWarnings("all")
        private String message;
        @java.lang.SuppressWarnings("all")
        private ContactMessageStatus status;
        @java.lang.SuppressWarnings("all")
        private boolean emailSent;
        @java.lang.SuppressWarnings("all")
        private String emailError;
        @java.lang.SuppressWarnings("all")
        private String remoteAddress;
        @java.lang.SuppressWarnings("all")
        private String userAgent;
        @java.lang.SuppressWarnings("all")
        private String adminNotes;
        @java.lang.SuppressWarnings("all")
        private String leadSource;
        @java.lang.SuppressWarnings("all")
        private LocalDateTime repliedAt;
        @java.lang.SuppressWarnings("all")
        private LocalDateTime lastViewedAt;
        @java.lang.SuppressWarnings("all")
        private String replyBody;
        @java.lang.SuppressWarnings("all")
        private String replySubject;
        @java.lang.SuppressWarnings("all")
        private LocalDateTime createdAt;
        @java.lang.SuppressWarnings("all")
        private LocalDateTime updatedAt;

        @java.lang.SuppressWarnings("all")
        AdminContactMessageResponseBuilder() {
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public AdminContactMessageResponse.AdminContactMessageResponseBuilder id(final Long id) {
            this.id = id;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public AdminContactMessageResponse.AdminContactMessageResponseBuilder name(final String name) {
            this.name = name;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public AdminContactMessageResponse.AdminContactMessageResponseBuilder email(final String email) {
            this.email = email;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public AdminContactMessageResponse.AdminContactMessageResponseBuilder subject(final String subject) {
            this.subject = subject;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public AdminContactMessageResponse.AdminContactMessageResponseBuilder message(final String message) {
            this.message = message;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public AdminContactMessageResponse.AdminContactMessageResponseBuilder status(final ContactMessageStatus status) {
            this.status = status;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public AdminContactMessageResponse.AdminContactMessageResponseBuilder emailSent(final boolean emailSent) {
            this.emailSent = emailSent;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public AdminContactMessageResponse.AdminContactMessageResponseBuilder emailError(final String emailError) {
            this.emailError = emailError;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public AdminContactMessageResponse.AdminContactMessageResponseBuilder remoteAddress(final String remoteAddress) {
            this.remoteAddress = remoteAddress;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public AdminContactMessageResponse.AdminContactMessageResponseBuilder userAgent(final String userAgent) {
            this.userAgent = userAgent;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public AdminContactMessageResponse.AdminContactMessageResponseBuilder adminNotes(final String adminNotes) {
            this.adminNotes = adminNotes;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public AdminContactMessageResponse.AdminContactMessageResponseBuilder leadSource(final String leadSource) {
            this.leadSource = leadSource;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public AdminContactMessageResponse.AdminContactMessageResponseBuilder repliedAt(final LocalDateTime repliedAt) {
            this.repliedAt = repliedAt;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public AdminContactMessageResponse.AdminContactMessageResponseBuilder lastViewedAt(final LocalDateTime lastViewedAt) {
            this.lastViewedAt = lastViewedAt;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public AdminContactMessageResponse.AdminContactMessageResponseBuilder replyBody(final String replyBody) {
            this.replyBody = replyBody;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public AdminContactMessageResponse.AdminContactMessageResponseBuilder replySubject(final String replySubject) {
            this.replySubject = replySubject;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public AdminContactMessageResponse.AdminContactMessageResponseBuilder createdAt(final LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public AdminContactMessageResponse.AdminContactMessageResponseBuilder updatedAt(final LocalDateTime updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        @java.lang.SuppressWarnings("all")
        public AdminContactMessageResponse build() {
            return new AdminContactMessageResponse(this.id, this.name, this.email, this.subject, this.message, this.status, this.emailSent, this.emailError, this.remoteAddress, this.userAgent, this.adminNotes, this.leadSource, this.repliedAt, this.lastViewedAt, this.replyBody, this.replySubject, this.createdAt, this.updatedAt);
        }

        @java.lang.Override
        @java.lang.SuppressWarnings("all")
        public java.lang.String toString() {
            return "AdminContactMessageResponse.AdminContactMessageResponseBuilder(id=" + this.id + ", name=" + this.name + ", email=" + this.email + ", subject=" + this.subject + ", message=" + this.message + ", status=" + this.status + ", emailSent=" + this.emailSent + ", emailError=" + this.emailError + ", remoteAddress=" + this.remoteAddress + ", userAgent=" + this.userAgent + ", adminNotes=" + this.adminNotes + ", leadSource=" + this.leadSource + ", repliedAt=" + this.repliedAt + ", lastViewedAt=" + this.lastViewedAt + ", replyBody=" + this.replyBody + ", replySubject=" + this.replySubject + ", createdAt=" + this.createdAt + ", updatedAt=" + this.updatedAt + ")";
        }
    }

    @java.lang.SuppressWarnings("all")
    public static AdminContactMessageResponse.AdminContactMessageResponseBuilder builder() {
        return new AdminContactMessageResponse.AdminContactMessageResponseBuilder();
    }

    @java.lang.SuppressWarnings("all")
    public Long getId() {
        return this.id;
    }

    @java.lang.SuppressWarnings("all")
    public String getName() {
        return this.name;
    }

    @java.lang.SuppressWarnings("all")
    public String getEmail() {
        return this.email;
    }

    @java.lang.SuppressWarnings("all")
    public String getSubject() {
        return this.subject;
    }

    @java.lang.SuppressWarnings("all")
    public String getMessage() {
        return this.message;
    }

    @java.lang.SuppressWarnings("all")
    public ContactMessageStatus getStatus() {
        return this.status;
    }

    @java.lang.SuppressWarnings("all")
    public boolean isEmailSent() {
        return this.emailSent;
    }

    @java.lang.SuppressWarnings("all")
    public String getEmailError() {
        return this.emailError;
    }

    @java.lang.SuppressWarnings("all")
    public String getRemoteAddress() {
        return this.remoteAddress;
    }

    @java.lang.SuppressWarnings("all")
    public String getUserAgent() {
        return this.userAgent;
    }

    @java.lang.SuppressWarnings("all")
    public String getAdminNotes() {
        return this.adminNotes;
    }

    @java.lang.SuppressWarnings("all")
    public String getLeadSource() {
        return this.leadSource;
    }

    @java.lang.SuppressWarnings("all")
    public LocalDateTime getRepliedAt() {
        return this.repliedAt;
    }

    @java.lang.SuppressWarnings("all")
    public LocalDateTime getLastViewedAt() {
        return this.lastViewedAt;
    }

    @java.lang.SuppressWarnings("all")
    public String getReplyBody() {
        return this.replyBody;
    }

    @java.lang.SuppressWarnings("all")
    public String getReplySubject() {
        return this.replySubject;
    }

    @java.lang.SuppressWarnings("all")
    public LocalDateTime getCreatedAt() {
        return this.createdAt;
    }

    @java.lang.SuppressWarnings("all")
    public LocalDateTime getUpdatedAt() {
        return this.updatedAt;
    }

    @java.lang.SuppressWarnings("all")
    public void setId(final Long id) {
        this.id = id;
    }

    @java.lang.SuppressWarnings("all")
    public void setName(final String name) {
        this.name = name;
    }

    @java.lang.SuppressWarnings("all")
    public void setEmail(final String email) {
        this.email = email;
    }

    @java.lang.SuppressWarnings("all")
    public void setSubject(final String subject) {
        this.subject = subject;
    }

    @java.lang.SuppressWarnings("all")
    public void setMessage(final String message) {
        this.message = message;
    }

    @java.lang.SuppressWarnings("all")
    public void setStatus(final ContactMessageStatus status) {
        this.status = status;
    }

    @java.lang.SuppressWarnings("all")
    public void setEmailSent(final boolean emailSent) {
        this.emailSent = emailSent;
    }

    @java.lang.SuppressWarnings("all")
    public void setEmailError(final String emailError) {
        this.emailError = emailError;
    }

    @java.lang.SuppressWarnings("all")
    public void setRemoteAddress(final String remoteAddress) {
        this.remoteAddress = remoteAddress;
    }

    @java.lang.SuppressWarnings("all")
    public void setUserAgent(final String userAgent) {
        this.userAgent = userAgent;
    }

    @java.lang.SuppressWarnings("all")
    public void setAdminNotes(final String adminNotes) {
        this.adminNotes = adminNotes;
    }

    @java.lang.SuppressWarnings("all")
    public void setLeadSource(final String leadSource) {
        this.leadSource = leadSource;
    }

    @java.lang.SuppressWarnings("all")
    public void setRepliedAt(final LocalDateTime repliedAt) {
        this.repliedAt = repliedAt;
    }

    @java.lang.SuppressWarnings("all")
    public void setLastViewedAt(final LocalDateTime lastViewedAt) {
        this.lastViewedAt = lastViewedAt;
    }

    @java.lang.SuppressWarnings("all")
    public void setReplyBody(final String replyBody) {
        this.replyBody = replyBody;
    }

    @java.lang.SuppressWarnings("all")
    public void setReplySubject(final String replySubject) {
        this.replySubject = replySubject;
    }

    @java.lang.SuppressWarnings("all")
    public void setCreatedAt(final LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @java.lang.SuppressWarnings("all")
    public void setUpdatedAt(final LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof AdminContactMessageResponse)) return false;
        final AdminContactMessageResponse other = (AdminContactMessageResponse) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        if (this.isEmailSent() != other.isEmailSent()) return false;
        final java.lang.Object this$id = this.getId();
        final java.lang.Object other$id = other.getId();
        if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
        final java.lang.Object this$name = this.getName();
        final java.lang.Object other$name = other.getName();
        if (this$name == null ? other$name != null : !this$name.equals(other$name)) return false;
        final java.lang.Object this$email = this.getEmail();
        final java.lang.Object other$email = other.getEmail();
        if (this$email == null ? other$email != null : !this$email.equals(other$email)) return false;
        final java.lang.Object this$subject = this.getSubject();
        final java.lang.Object other$subject = other.getSubject();
        if (this$subject == null ? other$subject != null : !this$subject.equals(other$subject)) return false;
        final java.lang.Object this$message = this.getMessage();
        final java.lang.Object other$message = other.getMessage();
        if (this$message == null ? other$message != null : !this$message.equals(other$message)) return false;
        final java.lang.Object this$status = this.getStatus();
        final java.lang.Object other$status = other.getStatus();
        if (this$status == null ? other$status != null : !this$status.equals(other$status)) return false;
        final java.lang.Object this$emailError = this.getEmailError();
        final java.lang.Object other$emailError = other.getEmailError();
        if (this$emailError == null ? other$emailError != null : !this$emailError.equals(other$emailError)) return false;
        final java.lang.Object this$remoteAddress = this.getRemoteAddress();
        final java.lang.Object other$remoteAddress = other.getRemoteAddress();
        if (this$remoteAddress == null ? other$remoteAddress != null : !this$remoteAddress.equals(other$remoteAddress)) return false;
        final java.lang.Object this$userAgent = this.getUserAgent();
        final java.lang.Object other$userAgent = other.getUserAgent();
        if (this$userAgent == null ? other$userAgent != null : !this$userAgent.equals(other$userAgent)) return false;
        final java.lang.Object this$adminNotes = this.getAdminNotes();
        final java.lang.Object other$adminNotes = other.getAdminNotes();
        if (this$adminNotes == null ? other$adminNotes != null : !this$adminNotes.equals(other$adminNotes)) return false;
        final java.lang.Object this$leadSource = this.getLeadSource();
        final java.lang.Object other$leadSource = other.getLeadSource();
        if (this$leadSource == null ? other$leadSource != null : !this$leadSource.equals(other$leadSource)) return false;
        final java.lang.Object this$repliedAt = this.getRepliedAt();
        final java.lang.Object other$repliedAt = other.getRepliedAt();
        if (this$repliedAt == null ? other$repliedAt != null : !this$repliedAt.equals(other$repliedAt)) return false;
        final java.lang.Object this$lastViewedAt = this.getLastViewedAt();
        final java.lang.Object other$lastViewedAt = other.getLastViewedAt();
        if (this$lastViewedAt == null ? other$lastViewedAt != null : !this$lastViewedAt.equals(other$lastViewedAt)) return false;
        final java.lang.Object this$replyBody = this.getReplyBody();
        final java.lang.Object other$replyBody = other.getReplyBody();
        if (this$replyBody == null ? other$replyBody != null : !this$replyBody.equals(other$replyBody)) return false;
        final java.lang.Object this$replySubject = this.getReplySubject();
        final java.lang.Object other$replySubject = other.getReplySubject();
        if (this$replySubject == null ? other$replySubject != null : !this$replySubject.equals(other$replySubject)) return false;
        final java.lang.Object this$createdAt = this.getCreatedAt();
        final java.lang.Object other$createdAt = other.getCreatedAt();
        if (this$createdAt == null ? other$createdAt != null : !this$createdAt.equals(other$createdAt)) return false;
        final java.lang.Object this$updatedAt = this.getUpdatedAt();
        final java.lang.Object other$updatedAt = other.getUpdatedAt();
        if (this$updatedAt == null ? other$updatedAt != null : !this$updatedAt.equals(other$updatedAt)) return false;
        return true;
    }

    @java.lang.SuppressWarnings("all")
    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof AdminContactMessageResponse;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        result = result * PRIME + (this.isEmailSent() ? 79 : 97);
        final java.lang.Object $id = this.getId();
        result = result * PRIME + ($id == null ? 43 : $id.hashCode());
        final java.lang.Object $name = this.getName();
        result = result * PRIME + ($name == null ? 43 : $name.hashCode());
        final java.lang.Object $email = this.getEmail();
        result = result * PRIME + ($email == null ? 43 : $email.hashCode());
        final java.lang.Object $subject = this.getSubject();
        result = result * PRIME + ($subject == null ? 43 : $subject.hashCode());
        final java.lang.Object $message = this.getMessage();
        result = result * PRIME + ($message == null ? 43 : $message.hashCode());
        final java.lang.Object $status = this.getStatus();
        result = result * PRIME + ($status == null ? 43 : $status.hashCode());
        final java.lang.Object $emailError = this.getEmailError();
        result = result * PRIME + ($emailError == null ? 43 : $emailError.hashCode());
        final java.lang.Object $remoteAddress = this.getRemoteAddress();
        result = result * PRIME + ($remoteAddress == null ? 43 : $remoteAddress.hashCode());
        final java.lang.Object $userAgent = this.getUserAgent();
        result = result * PRIME + ($userAgent == null ? 43 : $userAgent.hashCode());
        final java.lang.Object $adminNotes = this.getAdminNotes();
        result = result * PRIME + ($adminNotes == null ? 43 : $adminNotes.hashCode());
        final java.lang.Object $leadSource = this.getLeadSource();
        result = result * PRIME + ($leadSource == null ? 43 : $leadSource.hashCode());
        final java.lang.Object $repliedAt = this.getRepliedAt();
        result = result * PRIME + ($repliedAt == null ? 43 : $repliedAt.hashCode());
        final java.lang.Object $lastViewedAt = this.getLastViewedAt();
        result = result * PRIME + ($lastViewedAt == null ? 43 : $lastViewedAt.hashCode());
        final java.lang.Object $replyBody = this.getReplyBody();
        result = result * PRIME + ($replyBody == null ? 43 : $replyBody.hashCode());
        final java.lang.Object $replySubject = this.getReplySubject();
        result = result * PRIME + ($replySubject == null ? 43 : $replySubject.hashCode());
        final java.lang.Object $createdAt = this.getCreatedAt();
        result = result * PRIME + ($createdAt == null ? 43 : $createdAt.hashCode());
        final java.lang.Object $updatedAt = this.getUpdatedAt();
        result = result * PRIME + ($updatedAt == null ? 43 : $updatedAt.hashCode());
        return result;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public java.lang.String toString() {
        return "AdminContactMessageResponse(id=" + this.getId() + ", name=" + this.getName() + ", email=" + this.getEmail() + ", subject=" + this.getSubject() + ", message=" + this.getMessage() + ", status=" + this.getStatus() + ", emailSent=" + this.isEmailSent() + ", emailError=" + this.getEmailError() + ", remoteAddress=" + this.getRemoteAddress() + ", userAgent=" + this.getUserAgent() + ", adminNotes=" + this.getAdminNotes() + ", leadSource=" + this.getLeadSource() + ", repliedAt=" + this.getRepliedAt() + ", lastViewedAt=" + this.getLastViewedAt() + ", replyBody=" + this.getReplyBody() + ", replySubject=" + this.getReplySubject() + ", createdAt=" + this.getCreatedAt() + ", updatedAt=" + this.getUpdatedAt() + ")";
    }
}
