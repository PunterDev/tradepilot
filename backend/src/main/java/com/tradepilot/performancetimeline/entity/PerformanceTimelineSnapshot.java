package com.tradepilot.performancetimeline.entity;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "performance_timeline_snapshots",
        uniqueConstraints = @UniqueConstraint(name = "uk_performance_timeline_account_period", columnNames = {"account_id", "period_type", "period_start"}),
        indexes = {
                @Index(name = "idx_performance_timeline_user_account", columnList = "user_id, account_id"),
                @Index(name = "idx_performance_timeline_period", columnList = "account_id, period_start")
        }
)
public class PerformanceTimelineSnapshot {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "user_id", nullable = false)
    private Long userId;
    @Column(name = "account_id", nullable = false)
    private Long accountId;
    @Column(name = "period_type", nullable = false, length = 20)
    private String periodType = "MONTHLY";
    @Column(name = "period_start", nullable = false)
    private LocalDate periodStart;
    @Column(name = "period_end", nullable = false)
    private LocalDate periodEnd;
    @Enumerated(EnumType.STRING)
    @Column(name = "data_status", nullable = false, length = 30)
    private TimelineDataStatus dataStatus;
    @Column(name = "trade_count", nullable = false)
    private Integer tradeCount;
    @Column(name = "metrics_json", columnDefinition = "text")
    private String metricsJson;
    @Column(name = "comparison_json", columnDefinition = "text")
    private String comparisonJson;
    @Column(name = "category_scores_json", columnDefinition = "text")
    private String categoryScoresJson;
    @Column(name = "evolution_score")
    private Integer evolutionScore;
    @Enumerated(EnumType.STRING)
    @Column(name = "score_confidence", length = 20)
    private ScoreConfidence scoreConfidence;
    @Column(name = "narrative_json", columnDefinition = "text")
    private String narrativeJson;
    @Column(name = "ai_model", length = 80)
    private String aiModel;
    @Column(name = "prompt_version", length = 40)
    private String promptVersion;
    @Column(name = "data_hash", nullable = false, length = 128)
    private String dataHash;
    @Column(name = "generated_at", nullable = false)
    private LocalDateTime generatedAt;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = createdAt == null ? now : createdAt;
        updatedAt = updatedAt == null ? now : updatedAt;
        generatedAt = generatedAt == null ? now : generatedAt;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getAccountId() { return accountId; }
    public void setAccountId(Long accountId) { this.accountId = accountId; }
    public String getPeriodType() { return periodType; }
    public void setPeriodType(String periodType) { this.periodType = periodType; }
    public LocalDate getPeriodStart() { return periodStart; }
    public void setPeriodStart(LocalDate periodStart) { this.periodStart = periodStart; }
    public LocalDate getPeriodEnd() { return periodEnd; }
    public void setPeriodEnd(LocalDate periodEnd) { this.periodEnd = periodEnd; }
    public TimelineDataStatus getDataStatus() { return dataStatus; }
    public void setDataStatus(TimelineDataStatus dataStatus) { this.dataStatus = dataStatus; }
    public Integer getTradeCount() { return tradeCount; }
    public void setTradeCount(Integer tradeCount) { this.tradeCount = tradeCount; }
    public String getMetricsJson() { return metricsJson; }
    public void setMetricsJson(String metricsJson) { this.metricsJson = metricsJson; }
    public String getComparisonJson() { return comparisonJson; }
    public void setComparisonJson(String comparisonJson) { this.comparisonJson = comparisonJson; }
    public String getCategoryScoresJson() { return categoryScoresJson; }
    public void setCategoryScoresJson(String categoryScoresJson) { this.categoryScoresJson = categoryScoresJson; }
    public Integer getEvolutionScore() { return evolutionScore; }
    public void setEvolutionScore(Integer evolutionScore) { this.evolutionScore = evolutionScore; }
    public ScoreConfidence getScoreConfidence() { return scoreConfidence; }
    public void setScoreConfidence(ScoreConfidence scoreConfidence) { this.scoreConfidence = scoreConfidence; }
    public String getNarrativeJson() { return narrativeJson; }
    public void setNarrativeJson(String narrativeJson) { this.narrativeJson = narrativeJson; }
    public String getAiModel() { return aiModel; }
    public void setAiModel(String aiModel) { this.aiModel = aiModel; }
    public String getPromptVersion() { return promptVersion; }
    public void setPromptVersion(String promptVersion) { this.promptVersion = promptVersion; }
    public String getDataHash() { return dataHash; }
    public void setDataHash(String dataHash) { this.dataHash = dataHash; }
    public LocalDateTime getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(LocalDateTime generatedAt) { this.generatedAt = generatedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
