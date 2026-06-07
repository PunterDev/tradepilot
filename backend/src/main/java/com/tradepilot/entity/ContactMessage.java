package com.tradepilot.entity;

import com.tradepilot.enums.ContactMessageStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "contact_messages",
        indexes = {
                @Index(name = "idx_contact_messages_status", columnList = "status"),
                @Index(name = "idx_contact_messages_created_at", columnList = "created_at"),
                @Index(name = "idx_contact_messages_status_created_at", columnList = "status, created_at")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
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
    @Builder.Default
    private ContactMessageStatus status = ContactMessageStatus.NEW;

    @Column(name = "email_sent", nullable = false)
    @Builder.Default
    private boolean emailSent = false;

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
}
