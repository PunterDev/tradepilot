package com.tradepilot.observability;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "system_alerts", indexes = {
        @Index(name = "idx_system_alerts_resolved_created_at", columnList = "resolved, created_at"),
        @Index(name = "idx_system_alerts_type_created_at", columnList = "type, created_at"),
        @Index(name = "idx_system_alerts_status_created_at", columnList = "status, created_at"),
        @Index(name = "idx_system_alerts_severity_created_at", columnList = "severity, created_at")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemAlert {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 80)
    private String type;

    @Column(nullable = false, length = 24)
    private String severity;

    @Column(nullable = false, length = 700)
    private String message;

    @Column(nullable = false)
    @Builder.Default
    private Boolean resolved = false;

    @Column(nullable = false, length = 24)
    @Builder.Default
    private String status = "OPEN";

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "resolved_by")
    private Long resolvedBy;

    @PrePersist
    public void onCreate() {
        if (resolved == null) {
            resolved = false;
        }
        if (status == null || status.isBlank()) {
            status = Boolean.TRUE.equals(resolved) ? "RESOLVED" : "OPEN";
        }
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
