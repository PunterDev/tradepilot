package com.tradepilot.entity;

import com.tradepilot.enums.AdminAuditAction;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "admin_audit_logs",
        indexes = {
                @Index(name = "idx_admin_audit_admin_user_id", columnList = "admin_user_id"),
                @Index(name = "idx_admin_audit_target_user_id", columnList = "target_user_id"),
                @Index(name = "idx_admin_audit_created_at", columnList = "created_at"),
                @Index(name = "idx_admin_audit_admin_created_at", columnList = "admin_user_id, created_at"),
                @Index(name = "idx_admin_audit_target_created_at", columnList = "target_user_id, created_at")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminAuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "admin_user_id", nullable = false)
    private Long adminUserId;

    @Column(name = "target_user_id", nullable = false)
    private Long targetUserId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private AdminAuditAction action;

    @Column(length = 500)
    private String reason;

    @Column(name = "before_value", columnDefinition = "TEXT")
    private String beforeValue;

    @Column(name = "after_value", columnDefinition = "TEXT")
    private String afterValue;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
