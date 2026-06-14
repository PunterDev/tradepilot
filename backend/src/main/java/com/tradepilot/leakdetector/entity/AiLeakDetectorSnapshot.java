package com.tradepilot.leakdetector.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "ai_leak_detector_snapshots",
        indexes = {
                @Index(name = "idx_ai_leak_detector_user_id", columnList = "user_id"),
                @Index(name = "idx_ai_leak_detector_user_account_hash", columnList = "user_id, account_id, data_hash"),
                @Index(name = "idx_ai_leak_detector_user_account_updated", columnList = "user_id, account_id, updated_at")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiLeakDetectorSnapshot {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "account_id", nullable = false)
    private Long accountId;

    @Column(name = "data_hash", nullable = false, length = 128)
    private String dataHash;

    @Column(name = "total_trades", nullable = false)
    private Integer totalTrades;

    @Column(name = "result_json", columnDefinition = "TEXT", nullable = false)
    private String resultJson;

    @Column(name = "model", length = 80)
    private String model;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
