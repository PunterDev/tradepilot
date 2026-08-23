package com.tradepilot.playbook.entity;

import com.tradepilot.entity.User;
import com.tradepilot.playbook.enums.PlaybookConfidence;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "playbook_analysis_snapshot", indexes = {
        @Index(name = "idx_playbook_analysis_user_id", columnList = "user_id"),
        @Index(name = "idx_playbook_analysis_setup_id", columnList = "setup_id"),
        @Index(name = "idx_playbook_analysis_generated_at", columnList = "generated_at")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_playbook_analysis_setup_latest", columnNames = {"setup_id"})
})
public class PlaybookAnalysisSnapshot {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    @Column(name = "account_id")
    private Long accountId;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "setup_id", nullable = false)
    private PlaybookSetup setup;
    @Column(name = "metrics_json", columnDefinition = "text")
    private String metricsJson;
    @Column(name = "behavior_comparison_json", columnDefinition = "text")
    private String behaviorComparisonJson;
    @Column(name = "health_score")
    private Integer healthScore;
    @Enumerated(EnumType.STRING)
    @Column(name = "health_score_confidence", length = 20)
    private PlaybookConfidence healthScoreConfidence;
    @Column(name = "ai_review_json", columnDefinition = "text")
    private String aiReviewJson;
    @Column(name = "ai_model", length = 80)
    private String aiModel;
    @Column(name = "prompt_version", nullable = false, length = 40)
    private String promptVersion;
    @Column(name = "calculation_version", nullable = false, length = 40)
    private String calculationVersion;
    @Column(name = "data_hash", nullable = false, length = 128)
    private String dataHash;
    @Column(name = "generated_at", nullable = false)
    private LocalDateTime generatedAt;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
        if (generatedAt == null) generatedAt = now;
    }

    @PreUpdate
    void onUpdate() { updatedAt = LocalDateTime.now(); }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public Long getAccountId() { return accountId; }
    public void setAccountId(Long accountId) { this.accountId = accountId; }
    public PlaybookSetup getSetup() { return setup; }
    public void setSetup(PlaybookSetup setup) { this.setup = setup; }
    public String getMetricsJson() { return metricsJson; }
    public void setMetricsJson(String metricsJson) { this.metricsJson = metricsJson; }
    public String getBehaviorComparisonJson() { return behaviorComparisonJson; }
    public void setBehaviorComparisonJson(String behaviorComparisonJson) { this.behaviorComparisonJson = behaviorComparisonJson; }
    public Integer getHealthScore() { return healthScore; }
    public void setHealthScore(Integer healthScore) { this.healthScore = healthScore; }
    public PlaybookConfidence getHealthScoreConfidence() { return healthScoreConfidence; }
    public void setHealthScoreConfidence(PlaybookConfidence healthScoreConfidence) { this.healthScoreConfidence = healthScoreConfidence; }
    public String getAiReviewJson() { return aiReviewJson; }
    public void setAiReviewJson(String aiReviewJson) { this.aiReviewJson = aiReviewJson; }
    public String getAiModel() { return aiModel; }
    public void setAiModel(String aiModel) { this.aiModel = aiModel; }
    public String getPromptVersion() { return promptVersion; }
    public void setPromptVersion(String promptVersion) { this.promptVersion = promptVersion; }
    public String getCalculationVersion() { return calculationVersion; }
    public void setCalculationVersion(String calculationVersion) { this.calculationVersion = calculationVersion; }
    public String getDataHash() { return dataHash; }
    public void setDataHash(String dataHash) { this.dataHash = dataHash; }
    public LocalDateTime getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(LocalDateTime generatedAt) { this.generatedAt = generatedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
