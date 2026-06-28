package com.tradepilot.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "ai_trade_analyses", indexes = {@Index(name = "idx_ai_trade_analysis_user_id", columnList = "user_id"), @Index(name = "idx_ai_trade_analysis_trade_id", columnList = "trade_id"), @Index(name = "idx_ai_trade_analysis_hash", columnList = "review_hash"), @Index(name = "idx_ai_trade_analysis_user_cache", columnList = "user_id, trade_id, review_version, review_hash")}, uniqueConstraints = {@UniqueConstraint(name = "uk_ai_trade_analysis_cache", columnNames = {"user_id", "trade_id", "review_version", "review_hash"})})
public class AiTradeAnalysis {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "user_id", nullable = false)
    private Long userId;
    @Column(name = "trade_id", nullable = false)
    private Long tradeId;
    @Column(name = "review_version", nullable = false, length = 32)
    private String reviewVersion;
    @Column(name = "review_hash", nullable = false, length = 128)
    private String reviewHash;
    @Column(length = 32, nullable = false)
    private String status;
    @Column(length = 32)
    private String symbol;
    @Column(precision = 14, scale = 2)
    private BigDecimal pnl;
    @Column(length = 8)
    private String grade;
    @Column(name = "execution_score")
    private Integer executionScore;
    @Column(name = "discipline_score")
    private Integer disciplineScore;
    @Column(name = "risk_score")
    private Integer riskScore;
    @Column(columnDefinition = "TEXT")
    private String summary;
    @Column(name = "market_context", columnDefinition = "TEXT")
    private String marketContext;
    @Column(name = "entry_analysis", columnDefinition = "TEXT")
    private String entryAnalysis;
    @Column(name = "exit_analysis", columnDefinition = "TEXT")
    private String exitAnalysis;
    @Column(name = "risk_analysis", columnDefinition = "TEXT")
    private String riskAnalysis;
    @Column(name = "psychology_analysis", columnDefinition = "TEXT")
    private String psychologyAnalysis;
    @Column(name = "chart_analysis", columnDefinition = "TEXT")
    private String chartAnalysis;
    @Column(name = "mistakes_json", columnDefinition = "TEXT")
    private String mistakesJson;
    @Column(name = "strengths_json", columnDefinition = "TEXT")
    private String strengthsJson;
    @Column(name = "improvement_plan_json", columnDefinition = "TEXT")
    private String improvementPlanJson;
    @Column(name = "tags_json", columnDefinition = "TEXT")
    private String tagsJson;
    @Column(name = "has_chart_image", nullable = false)
    private boolean hasChartImage;
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    @java.lang.SuppressWarnings("all")
    public Long getId() {
        return this.id;
    }

    @java.lang.SuppressWarnings("all")
    public Long getUserId() {
        return this.userId;
    }

    @java.lang.SuppressWarnings("all")
    public Long getTradeId() {
        return this.tradeId;
    }

    @java.lang.SuppressWarnings("all")
    public String getReviewVersion() {
        return this.reviewVersion;
    }

    @java.lang.SuppressWarnings("all")
    public String getReviewHash() {
        return this.reviewHash;
    }

    @java.lang.SuppressWarnings("all")
    public String getStatus() {
        return this.status;
    }

    @java.lang.SuppressWarnings("all")
    public String getSymbol() {
        return this.symbol;
    }

    @java.lang.SuppressWarnings("all")
    public BigDecimal getPnl() {
        return this.pnl;
    }

    @java.lang.SuppressWarnings("all")
    public String getGrade() {
        return this.grade;
    }

    @java.lang.SuppressWarnings("all")
    public Integer getExecutionScore() {
        return this.executionScore;
    }

    @java.lang.SuppressWarnings("all")
    public Integer getDisciplineScore() {
        return this.disciplineScore;
    }

    @java.lang.SuppressWarnings("all")
    public Integer getRiskScore() {
        return this.riskScore;
    }

    @java.lang.SuppressWarnings("all")
    public String getSummary() {
        return this.summary;
    }

    @java.lang.SuppressWarnings("all")
    public String getMarketContext() {
        return this.marketContext;
    }

    @java.lang.SuppressWarnings("all")
    public String getEntryAnalysis() {
        return this.entryAnalysis;
    }

    @java.lang.SuppressWarnings("all")
    public String getExitAnalysis() {
        return this.exitAnalysis;
    }

    @java.lang.SuppressWarnings("all")
    public String getRiskAnalysis() {
        return this.riskAnalysis;
    }

    @java.lang.SuppressWarnings("all")
    public String getPsychologyAnalysis() {
        return this.psychologyAnalysis;
    }

    @java.lang.SuppressWarnings("all")
    public String getChartAnalysis() {
        return this.chartAnalysis;
    }

    @java.lang.SuppressWarnings("all")
    public String getMistakesJson() {
        return this.mistakesJson;
    }

    @java.lang.SuppressWarnings("all")
    public String getStrengthsJson() {
        return this.strengthsJson;
    }

    @java.lang.SuppressWarnings("all")
    public String getImprovementPlanJson() {
        return this.improvementPlanJson;
    }

    @java.lang.SuppressWarnings("all")
    public String getTagsJson() {
        return this.tagsJson;
    }

    @java.lang.SuppressWarnings("all")
    public boolean isHasChartImage() {
        return this.hasChartImage;
    }

    @java.lang.SuppressWarnings("all")
    public String getErrorMessage() {
        return this.errorMessage;
    }

    @java.lang.SuppressWarnings("all")
    public Instant getCreatedAt() {
        return this.createdAt;
    }

    @java.lang.SuppressWarnings("all")
    public Instant getUpdatedAt() {
        return this.updatedAt;
    }

    @java.lang.SuppressWarnings("all")
    public void setId(final Long id) {
        this.id = id;
    }

    @java.lang.SuppressWarnings("all")
    public void setUserId(final Long userId) {
        this.userId = userId;
    }

    @java.lang.SuppressWarnings("all")
    public void setTradeId(final Long tradeId) {
        this.tradeId = tradeId;
    }

    @java.lang.SuppressWarnings("all")
    public void setReviewVersion(final String reviewVersion) {
        this.reviewVersion = reviewVersion;
    }

    @java.lang.SuppressWarnings("all")
    public void setReviewHash(final String reviewHash) {
        this.reviewHash = reviewHash;
    }

    @java.lang.SuppressWarnings("all")
    public void setStatus(final String status) {
        this.status = status;
    }

    @java.lang.SuppressWarnings("all")
    public void setSymbol(final String symbol) {
        this.symbol = symbol;
    }

    @java.lang.SuppressWarnings("all")
    public void setPnl(final BigDecimal pnl) {
        this.pnl = pnl;
    }

    @java.lang.SuppressWarnings("all")
    public void setGrade(final String grade) {
        this.grade = grade;
    }

    @java.lang.SuppressWarnings("all")
    public void setExecutionScore(final Integer executionScore) {
        this.executionScore = executionScore;
    }

    @java.lang.SuppressWarnings("all")
    public void setDisciplineScore(final Integer disciplineScore) {
        this.disciplineScore = disciplineScore;
    }

    @java.lang.SuppressWarnings("all")
    public void setRiskScore(final Integer riskScore) {
        this.riskScore = riskScore;
    }

    @java.lang.SuppressWarnings("all")
    public void setSummary(final String summary) {
        this.summary = summary;
    }

    @java.lang.SuppressWarnings("all")
    public void setMarketContext(final String marketContext) {
        this.marketContext = marketContext;
    }

    @java.lang.SuppressWarnings("all")
    public void setEntryAnalysis(final String entryAnalysis) {
        this.entryAnalysis = entryAnalysis;
    }

    @java.lang.SuppressWarnings("all")
    public void setExitAnalysis(final String exitAnalysis) {
        this.exitAnalysis = exitAnalysis;
    }

    @java.lang.SuppressWarnings("all")
    public void setRiskAnalysis(final String riskAnalysis) {
        this.riskAnalysis = riskAnalysis;
    }

    @java.lang.SuppressWarnings("all")
    public void setPsychologyAnalysis(final String psychologyAnalysis) {
        this.psychologyAnalysis = psychologyAnalysis;
    }

    @java.lang.SuppressWarnings("all")
    public void setChartAnalysis(final String chartAnalysis) {
        this.chartAnalysis = chartAnalysis;
    }

    @java.lang.SuppressWarnings("all")
    public void setMistakesJson(final String mistakesJson) {
        this.mistakesJson = mistakesJson;
    }

    @java.lang.SuppressWarnings("all")
    public void setStrengthsJson(final String strengthsJson) {
        this.strengthsJson = strengthsJson;
    }

    @java.lang.SuppressWarnings("all")
    public void setImprovementPlanJson(final String improvementPlanJson) {
        this.improvementPlanJson = improvementPlanJson;
    }

    @java.lang.SuppressWarnings("all")
    public void setTagsJson(final String tagsJson) {
        this.tagsJson = tagsJson;
    }

    @java.lang.SuppressWarnings("all")
    public void setHasChartImage(final boolean hasChartImage) {
        this.hasChartImage = hasChartImage;
    }

    @java.lang.SuppressWarnings("all")
    public void setErrorMessage(final String errorMessage) {
        this.errorMessage = errorMessage;
    }

    @java.lang.SuppressWarnings("all")
    public void setCreatedAt(final Instant createdAt) {
        this.createdAt = createdAt;
    }

    @java.lang.SuppressWarnings("all")
    public void setUpdatedAt(final Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
