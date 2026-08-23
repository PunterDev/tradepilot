package com.tradepilot.playbook.entity;

import com.tradepilot.entity.Trade;
import com.tradepilot.entity.TradingAccount;
import com.tradepilot.entity.User;
import com.tradepilot.playbook.enums.AssignmentSource;
import com.tradepilot.playbook.enums.ExecutionQualityGrade;
import com.tradepilot.playbook.enums.PlaybookConfidence;
import com.tradepilot.playbook.enums.SetupQualityGrade;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "trade_playbook_assignment", indexes = {
        @Index(name = "idx_trade_playbook_assignment_user_id", columnList = "user_id"),
        @Index(name = "idx_trade_playbook_assignment_account_id", columnList = "account_id"),
        @Index(name = "idx_trade_playbook_assignment_trade_id", columnList = "trade_id"),
        @Index(name = "idx_trade_playbook_assignment_setup_id", columnList = "setup_id")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_trade_playbook_assignment_trade_primary", columnNames = {"trade_id"})
})
public class TradePlaybookAssignment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id")
    private TradingAccount account;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "trade_id", nullable = false)
    private Trade trade;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "setup_id", nullable = false)
    private PlaybookSetup setup;
    @Enumerated(EnumType.STRING)
    @Column(name = "setup_quality_grade", nullable = false, length = 16)
    private SetupQualityGrade setupQualityGrade;
    @Enumerated(EnumType.STRING)
    @Column(name = "execution_quality_grade", nullable = false, length = 16)
    private ExecutionQualityGrade executionQualityGrade;
    @Column(name = "compliance_score")
    private Integer complianceScore;
    @Enumerated(EnumType.STRING)
    @Column(name = "compliance_confidence", length = 20)
    private PlaybookConfidence complianceConfidence;
    @Column(name = "checklist_results_json", columnDefinition = "text")
    private String checklistResultsJson;
    @Column(name = "rule_violations_json", columnDefinition = "text")
    private String ruleViolationsJson;
    @Enumerated(EnumType.STRING)
    @Column(name = "assignment_source", nullable = false, length = 32)
    private AssignmentSource assignmentSource;
    @Column(name = "assignment_confidence")
    private Integer assignmentConfidence;
    @Column(name = "reviewed_by_user", nullable = false)
    private Boolean reviewedByUser;
    @Column(columnDefinition = "text")
    private String notes;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
        if (setupQualityGrade == null) setupQualityGrade = SetupQualityGrade.UNGRADED;
        if (executionQualityGrade == null) executionQualityGrade = ExecutionQualityGrade.UNGRADED;
        if (assignmentSource == null) assignmentSource = AssignmentSource.MANUAL;
        if (reviewedByUser == null) reviewedByUser = false;
    }

    @PreUpdate
    void onUpdate() { updatedAt = LocalDateTime.now(); }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public TradingAccount getAccount() { return account; }
    public void setAccount(TradingAccount account) { this.account = account; }
    public Trade getTrade() { return trade; }
    public void setTrade(Trade trade) { this.trade = trade; }
    public PlaybookSetup getSetup() { return setup; }
    public void setSetup(PlaybookSetup setup) { this.setup = setup; }
    public SetupQualityGrade getSetupQualityGrade() { return setupQualityGrade; }
    public void setSetupQualityGrade(SetupQualityGrade setupQualityGrade) { this.setupQualityGrade = setupQualityGrade; }
    public ExecutionQualityGrade getExecutionQualityGrade() { return executionQualityGrade; }
    public void setExecutionQualityGrade(ExecutionQualityGrade executionQualityGrade) { this.executionQualityGrade = executionQualityGrade; }
    public Integer getComplianceScore() { return complianceScore; }
    public void setComplianceScore(Integer complianceScore) { this.complianceScore = complianceScore; }
    public PlaybookConfidence getComplianceConfidence() { return complianceConfidence; }
    public void setComplianceConfidence(PlaybookConfidence complianceConfidence) { this.complianceConfidence = complianceConfidence; }
    public String getChecklistResultsJson() { return checklistResultsJson; }
    public void setChecklistResultsJson(String checklistResultsJson) { this.checklistResultsJson = checklistResultsJson; }
    public String getRuleViolationsJson() { return ruleViolationsJson; }
    public void setRuleViolationsJson(String ruleViolationsJson) { this.ruleViolationsJson = ruleViolationsJson; }
    public AssignmentSource getAssignmentSource() { return assignmentSource; }
    public void setAssignmentSource(AssignmentSource assignmentSource) { this.assignmentSource = assignmentSource; }
    public Integer getAssignmentConfidence() { return assignmentConfidence; }
    public void setAssignmentConfidence(Integer assignmentConfidence) { this.assignmentConfidence = assignmentConfidence; }
    public Boolean getReviewedByUser() { return reviewedByUser; }
    public void setReviewedByUser(Boolean reviewedByUser) { this.reviewedByUser = reviewedByUser; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
