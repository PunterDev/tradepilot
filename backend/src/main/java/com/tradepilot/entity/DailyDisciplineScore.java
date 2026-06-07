package com.tradepilot.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "daily_discipline_scores",
        indexes = {
                @Index(name = "idx_daily_discipline_scores_user_id", columnList = "user_id"),
                @Index(name = "idx_daily_discipline_scores_user_account_date", columnList = "user_id, account_id, trade_date")
        }
)
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class DailyDisciplineScore {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;
    private LocalDate tradeDate;
    private Long accountId;
    private Integer score;
    private Integer totalTrades;
    private Integer violationCount;
    private Integer majorViolationCount;
    private Integer criticalViolationCount;
    private Integer currentCleanDayStreak;
    private Boolean cleanDay;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
