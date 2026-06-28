package com.tradepilot.entity;

import com.tradepilot.enums.ContactMessageStatus;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "contact_messages", indexes = {@Index(name = "idx_contact_messages_status", columnList = "status"), @Index(name = "idx_contact_messages_created_at", columnList = "created_at"), @Index(name = "idx_contact_messages_status_created_at", columnList = "status, created_at")})
public class ContactMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 120)
    private String name;
    @Column(nullable = false, length = 180)
    private String email;
    @Column(nullable = false, length = 180)
    private String subject;
    @Column(nullable = false, length = 5000)
    private String message;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ContactMessageStatus status;
    @Column(name = "email_sent", nullable = false)
    private boolean emailSent;
    @Column(name = "email_error", length = 1000)
    private String emailError;
    @Column(name = "remote_address", length = 120)
    private String remoteAddress;
    @Column(name = "user_agent", length = 500)
    private String userAgent;
    @Column(name = "admin_notes", length = 5000)
    private String adminNotes;
    @Column(name = "lead_source", length = 120)
    private String leadSource;
    @Column(name = "replied_at")
    private LocalDateTime repliedAt;
    @Column(name = "last_viewed_at")
    private LocalDateTime lastViewedAt;
    @Column(name = "reply_body", length = 5000)
    private String replyBody;
    @Column(name = "reply_subject", length = 180)
    private String replySubject;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
        if (status == null) {
            status = ContactMessageStatus.NEW;
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    @java.lang.SuppressWarnings("all")
    private static ContactMessageStatus $default$status() {
        return ContactMessageStatus.NEW;
    }

    @java.lang.SuppressWarnings("all")
    private static boolean $default$emailSent() {
        return false;
    }


    @java.lang.SuppressWarnings("all")
    public static class ContactMessageBuilder {
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
        private boolean status$set;
        @java.lang.SuppressWarnings("all")
        private ContactMessageStatus status$value;
        @java.lang.SuppressWarnings("all")
        private boolean emailSent$set;
        @java.lang.SuppressWarnings("all")
        private boolean emailSent$value;
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
        ContactMessageBuilder() {
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public ContactMessage.ContactMessageBuilder id(final Long id) {
            this.id = id;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public ContactMessage.ContactMessageBuilder name(final String name) {
            this.name = name;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public ContactMessage.ContactMessageBuilder email(final String email) {
            this.email = email;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public ContactMessage.ContactMessageBuilder subject(final String subject) {
            this.subject = subject;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public ContactMessage.ContactMessageBuilder message(final String message) {
            this.message = message;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public ContactMessage.ContactMessageBuilder status(final ContactMessageStatus status) {
            this.status$value = status;
            status$set = true;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public ContactMessage.ContactMessageBuilder emailSent(final boolean emailSent) {
            this.emailSent$value = emailSent;
            emailSent$set = true;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public ContactMessage.ContactMessageBuilder emailError(final String emailError) {
            this.emailError = emailError;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public ContactMessage.ContactMessageBuilder remoteAddress(final String remoteAddress) {
            this.remoteAddress = remoteAddress;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public ContactMessage.ContactMessageBuilder userAgent(final String userAgent) {
            this.userAgent = userAgent;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public ContactMessage.ContactMessageBuilder adminNotes(final String adminNotes) {
            this.adminNotes = adminNotes;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public ContactMessage.ContactMessageBuilder leadSource(final String leadSource) {
            this.leadSource = leadSource;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public ContactMessage.ContactMessageBuilder repliedAt(final LocalDateTime repliedAt) {
            this.repliedAt = repliedAt;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public ContactMessage.ContactMessageBuilder lastViewedAt(final LocalDateTime lastViewedAt) {
            this.lastViewedAt = lastViewedAt;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public ContactMessage.ContactMessageBuilder replyBody(final String replyBody) {
            this.replyBody = replyBody;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public ContactMessage.ContactMessageBuilder replySubject(final String replySubject) {
            this.replySubject = replySubject;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public ContactMessage.ContactMessageBuilder createdAt(final LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public ContactMessage.ContactMessageBuilder updatedAt(final LocalDateTime updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        @java.lang.SuppressWarnings("all")
        public ContactMessage build() {
            ContactMessageStatus status$value = this.status$value;
            if (!this.status$set) status$value = ContactMessage.$default$status();
            boolean emailSent$value = this.emailSent$value;
            if (!this.emailSent$set) emailSent$value = ContactMessage.$default$emailSent();
            return new ContactMessage(this.id, this.name, this.email, this.subject, this.message, status$value, emailSent$value, this.emailError, this.remoteAddress, this.userAgent, this.adminNotes, this.leadSource, this.repliedAt, this.lastViewedAt, this.replyBody, this.replySubject, this.createdAt, this.updatedAt);
        }

        @java.lang.Override
        @java.lang.SuppressWarnings("all")
        public java.lang.String toString() {
            return "ContactMessage.ContactMessageBuilder(id=" + this.id + ", name=" + this.name + ", email=" + this.email + ", subject=" + this.subject + ", message=" + this.message + ", status$value=" + this.status$value + ", emailSent$value=" + this.emailSent$value + ", emailError=" + this.emailError + ", remoteAddress=" + this.remoteAddress + ", userAgent=" + this.userAgent + ", adminNotes=" + this.adminNotes + ", leadSource=" + this.leadSource + ", repliedAt=" + this.repliedAt + ", lastViewedAt=" + this.lastViewedAt + ", replyBody=" + this.replyBody + ", replySubject=" + this.replySubject + ", createdAt=" + this.createdAt + ", updatedAt=" + this.updatedAt + ")";
        }
    }

    @java.lang.SuppressWarnings("all")
    public static ContactMessage.ContactMessageBuilder builder() {
        return new ContactMessage.ContactMessageBuilder();
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

    @java.lang.SuppressWarnings("all")
    public ContactMessage() {
        this.status = ContactMessage.$default$status();
        this.emailSent = ContactMessage.$default$emailSent();
    }

    @java.lang.SuppressWarnings("all")
    public ContactMessage(final Long id, final String name, final String email, final String subject, final String message, final ContactMessageStatus status, final boolean emailSent, final String emailError, final String remoteAddress, final String userAgent, final String adminNotes, final String leadSource, final LocalDateTime repliedAt, final LocalDateTime lastViewedAt, final String replyBody, final String replySubject, final LocalDateTime createdAt, final LocalDateTime updatedAt) {
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
}
