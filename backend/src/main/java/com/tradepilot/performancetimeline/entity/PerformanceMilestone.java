package com.tradepilot.performancetimeline.entity;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "performance_milestones",
        uniqueConstraints = @UniqueConstraint(name = "uk_performance_milestone_account_type", columnNames = {"account_id", "milestone_type"}),
        indexes = {
                @Index(name = "idx_performance_milestones_user_account", columnList = "user_id, account_id"),
                @Index(name = "idx_performance_milestones_period", columnList = "account_id, period_start")
        }
)
public class PerformanceMilestone {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "user_id", nullable = false)
    private Long userId;
    @Column(name = "account_id", nullable = false)
    private Long accountId;
    @Enumerated(EnumType.STRING)
    @Column(name = "milestone_type", nullable = false, length = 80)
    private MilestoneType milestoneType;
    @Column(name = "earned_date", nullable = false)
    private LocalDate earnedDate;
    @Column(name = "period_start", nullable = false)
    private LocalDate periodStart;
    @Column(name = "period_end", nullable = false)
    private LocalDate periodEnd;
    @Column(name = "evidence_json", columnDefinition = "text")
    private String evidenceJson;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        createdAt = createdAt == null ? LocalDateTime.now() : createdAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getAccountId() { return accountId; }
    public void setAccountId(Long accountId) { this.accountId = accountId; }
    public MilestoneType getMilestoneType() { return milestoneType; }
    public void setMilestoneType(MilestoneType milestoneType) { this.milestoneType = milestoneType; }
    public LocalDate getEarnedDate() { return earnedDate; }
    public void setEarnedDate(LocalDate earnedDate) { this.earnedDate = earnedDate; }
    public LocalDate getPeriodStart() { return periodStart; }
    public void setPeriodStart(LocalDate periodStart) { this.periodStart = periodStart; }
    public LocalDate getPeriodEnd() { return periodEnd; }
    public void setPeriodEnd(LocalDate periodEnd) { this.periodEnd = periodEnd; }
    public String getEvidenceJson() { return evidenceJson; }
    public void setEvidenceJson(String evidenceJson) { this.evidenceJson = evidenceJson; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
