package com.tradepilot.playbook.entity;

import com.tradepilot.entity.User;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "playbook_milestone", indexes = {
        @Index(name = "idx_playbook_milestone_user_id", columnList = "user_id"),
        @Index(name = "idx_playbook_milestone_setup_id", columnList = "setup_id")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_playbook_milestone_once", columnNames = {"user_id", "setup_id", "milestone_type"})
})
public class PlaybookMilestone {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    @Column(name = "account_id")
    private Long accountId;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "setup_id")
    private PlaybookSetup setup;
    @Column(name = "milestone_type", nullable = false, length = 80)
    private String milestoneType;
    @Column(name = "earned_date", nullable = false)
    private LocalDate earnedDate;
    @Column(name = "evidence_json", columnDefinition = "text")
    private String evidenceJson;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (earnedDate == null) earnedDate = LocalDate.now();
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public Long getAccountId() { return accountId; }
    public void setAccountId(Long accountId) { this.accountId = accountId; }
    public PlaybookSetup getSetup() { return setup; }
    public void setSetup(PlaybookSetup setup) { this.setup = setup; }
    public String getMilestoneType() { return milestoneType; }
    public void setMilestoneType(String milestoneType) { this.milestoneType = milestoneType; }
    public LocalDate getEarnedDate() { return earnedDate; }
    public void setEarnedDate(LocalDate earnedDate) { this.earnedDate = earnedDate; }
    public String getEvidenceJson() { return evidenceJson; }
    public void setEvidenceJson(String evidenceJson) { this.evidenceJson = evidenceJson; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
