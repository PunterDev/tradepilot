package com.tradepilot.leakdetector.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class LeakResultDto {
    private LeakType leakType;
    private String title;
    private String summary;
    private String description;
    private Integer tradeCount;
    private BigDecimal winRate;
    private BigDecimal totalPnl;
    private BigDecimal avgPnl;
    private BigDecimal estimatedMonthlyImpact;
    private BigDecimal totalR;
    private BigDecimal avgR;
    private BigDecimal estimatedMonthlyImpactR;
    private Boolean hasValidRMultiple;
    private Integer confidenceScore;
    private LeakSeverity severity;
    private String recommendedFix;
    private Integer relatedTradeCount;
    private List<Long> relatedTradeIds;
    private LocalDateTime generatedAt;

    public static LeakResultDto of(LeakType leakType, String title, String summary, String description, Integer tradeCount, BigDecimal winRate, BigDecimal totalPnl, BigDecimal avgPnl, BigDecimal estimatedMonthlyImpact, BigDecimal totalR, BigDecimal avgR, BigDecimal estimatedMonthlyImpactR, Boolean hasValidRMultiple, Integer confidenceScore, LeakSeverity severity, String recommendedFix, Integer relatedTradeCount, List<Long> relatedTradeIds, LocalDateTime generatedAt) {
        LeakResultDto result = new LeakResultDto();
        result.setLeakType(leakType);
        result.setTitle(title);
        result.setSummary(summary);
        result.setDescription(description);
        result.setTradeCount(tradeCount);
        result.setWinRate(winRate);
        result.setTotalPnl(totalPnl);
        result.setAvgPnl(avgPnl);
        result.setEstimatedMonthlyImpact(estimatedMonthlyImpact);
        result.setTotalR(totalR);
        result.setAvgR(avgR);
        result.setEstimatedMonthlyImpactR(estimatedMonthlyImpactR);
        result.setHasValidRMultiple(hasValidRMultiple);
        result.setConfidenceScore(confidenceScore);
        result.setSeverity(severity);
        result.setRecommendedFix(recommendedFix);
        result.setRelatedTradeCount(relatedTradeCount);
        result.setRelatedTradeIds(relatedTradeIds);
        result.setGeneratedAt(generatedAt);
        return result;
    }

    @java.lang.SuppressWarnings("all")
    public LeakType getLeakType() {
        return this.leakType;
    }

    @java.lang.SuppressWarnings("all")
    public String getTitle() {
        return this.title;
    }

    @java.lang.SuppressWarnings("all")
    public String getSummary() {
        return this.summary;
    }

    @java.lang.SuppressWarnings("all")
    public String getDescription() {
        return this.description;
    }

    @java.lang.SuppressWarnings("all")
    public Integer getTradeCount() {
        return this.tradeCount;
    }

    @java.lang.SuppressWarnings("all")
    public BigDecimal getWinRate() {
        return this.winRate;
    }

    @java.lang.SuppressWarnings("all")
    public BigDecimal getTotalPnl() {
        return this.totalPnl;
    }

    @java.lang.SuppressWarnings("all")
    public BigDecimal getAvgPnl() {
        return this.avgPnl;
    }

    @java.lang.SuppressWarnings("all")
    public BigDecimal getEstimatedMonthlyImpact() {
        return this.estimatedMonthlyImpact;
    }

    @java.lang.SuppressWarnings("all")
    public BigDecimal getTotalR() {
        return this.totalR;
    }

    @java.lang.SuppressWarnings("all")
    public BigDecimal getAvgR() {
        return this.avgR;
    }

    @java.lang.SuppressWarnings("all")
    public BigDecimal getEstimatedMonthlyImpactR() {
        return this.estimatedMonthlyImpactR;
    }

    @java.lang.SuppressWarnings("all")
    public Boolean getHasValidRMultiple() {
        return this.hasValidRMultiple;
    }

    @java.lang.SuppressWarnings("all")
    public Integer getConfidenceScore() {
        return this.confidenceScore;
    }

    @java.lang.SuppressWarnings("all")
    public LeakSeverity getSeverity() {
        return this.severity;
    }

    @java.lang.SuppressWarnings("all")
    public String getRecommendedFix() {
        return this.recommendedFix;
    }

    @java.lang.SuppressWarnings("all")
    public Integer getRelatedTradeCount() {
        return this.relatedTradeCount;
    }

    @java.lang.SuppressWarnings("all")
    public List<Long> getRelatedTradeIds() {
        return this.relatedTradeIds;
    }

    @java.lang.SuppressWarnings("all")
    public LocalDateTime getGeneratedAt() {
        return this.generatedAt;
    }

    @java.lang.SuppressWarnings("all")
    public void setLeakType(final LeakType leakType) {
        this.leakType = leakType;
    }

    @java.lang.SuppressWarnings("all")
    public void setTitle(final String title) {
        this.title = title;
    }

    @java.lang.SuppressWarnings("all")
    public void setSummary(final String summary) {
        this.summary = summary;
    }

    @java.lang.SuppressWarnings("all")
    public void setDescription(final String description) {
        this.description = description;
    }

    @java.lang.SuppressWarnings("all")
    public void setTradeCount(final Integer tradeCount) {
        this.tradeCount = tradeCount;
    }

    @java.lang.SuppressWarnings("all")
    public void setWinRate(final BigDecimal winRate) {
        this.winRate = winRate;
    }

    @java.lang.SuppressWarnings("all")
    public void setTotalPnl(final BigDecimal totalPnl) {
        this.totalPnl = totalPnl;
    }

    @java.lang.SuppressWarnings("all")
    public void setAvgPnl(final BigDecimal avgPnl) {
        this.avgPnl = avgPnl;
    }

    @java.lang.SuppressWarnings("all")
    public void setEstimatedMonthlyImpact(final BigDecimal estimatedMonthlyImpact) {
        this.estimatedMonthlyImpact = estimatedMonthlyImpact;
    }

    @java.lang.SuppressWarnings("all")
    public void setTotalR(final BigDecimal totalR) {
        this.totalR = totalR;
    }

    @java.lang.SuppressWarnings("all")
    public void setAvgR(final BigDecimal avgR) {
        this.avgR = avgR;
    }

    @java.lang.SuppressWarnings("all")
    public void setEstimatedMonthlyImpactR(final BigDecimal estimatedMonthlyImpactR) {
        this.estimatedMonthlyImpactR = estimatedMonthlyImpactR;
    }

    @java.lang.SuppressWarnings("all")
    public void setHasValidRMultiple(final Boolean hasValidRMultiple) {
        this.hasValidRMultiple = hasValidRMultiple;
    }

    @java.lang.SuppressWarnings("all")
    public void setConfidenceScore(final Integer confidenceScore) {
        this.confidenceScore = confidenceScore;
    }

    @java.lang.SuppressWarnings("all")
    public void setSeverity(final LeakSeverity severity) {
        this.severity = severity;
    }

    @java.lang.SuppressWarnings("all")
    public void setRecommendedFix(final String recommendedFix) {
        this.recommendedFix = recommendedFix;
    }

    @java.lang.SuppressWarnings("all")
    public void setRelatedTradeCount(final Integer relatedTradeCount) {
        this.relatedTradeCount = relatedTradeCount;
    }

    @java.lang.SuppressWarnings("all")
    public void setRelatedTradeIds(final List<Long> relatedTradeIds) {
        this.relatedTradeIds = relatedTradeIds;
    }

    @java.lang.SuppressWarnings("all")
    public void setGeneratedAt(final LocalDateTime generatedAt) {
        this.generatedAt = generatedAt;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof LeakResultDto)) return false;
        final LeakResultDto other = (LeakResultDto) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$tradeCount = this.getTradeCount();
        final java.lang.Object other$tradeCount = other.getTradeCount();
        if (this$tradeCount == null ? other$tradeCount != null : !this$tradeCount.equals(other$tradeCount)) return false;
        final java.lang.Object this$hasValidRMultiple = this.getHasValidRMultiple();
        final java.lang.Object other$hasValidRMultiple = other.getHasValidRMultiple();
        if (this$hasValidRMultiple == null ? other$hasValidRMultiple != null : !this$hasValidRMultiple.equals(other$hasValidRMultiple)) return false;
        final java.lang.Object this$confidenceScore = this.getConfidenceScore();
        final java.lang.Object other$confidenceScore = other.getConfidenceScore();
        if (this$confidenceScore == null ? other$confidenceScore != null : !this$confidenceScore.equals(other$confidenceScore)) return false;
        final java.lang.Object this$relatedTradeCount = this.getRelatedTradeCount();
        final java.lang.Object other$relatedTradeCount = other.getRelatedTradeCount();
        if (this$relatedTradeCount == null ? other$relatedTradeCount != null : !this$relatedTradeCount.equals(other$relatedTradeCount)) return false;
        final java.lang.Object this$leakType = this.getLeakType();
        final java.lang.Object other$leakType = other.getLeakType();
        if (this$leakType == null ? other$leakType != null : !this$leakType.equals(other$leakType)) return false;
        final java.lang.Object this$title = this.getTitle();
        final java.lang.Object other$title = other.getTitle();
        if (this$title == null ? other$title != null : !this$title.equals(other$title)) return false;
        final java.lang.Object this$summary = this.getSummary();
        final java.lang.Object other$summary = other.getSummary();
        if (this$summary == null ? other$summary != null : !this$summary.equals(other$summary)) return false;
        final java.lang.Object this$description = this.getDescription();
        final java.lang.Object other$description = other.getDescription();
        if (this$description == null ? other$description != null : !this$description.equals(other$description)) return false;
        final java.lang.Object this$winRate = this.getWinRate();
        final java.lang.Object other$winRate = other.getWinRate();
        if (this$winRate == null ? other$winRate != null : !this$winRate.equals(other$winRate)) return false;
        final java.lang.Object this$totalPnl = this.getTotalPnl();
        final java.lang.Object other$totalPnl = other.getTotalPnl();
        if (this$totalPnl == null ? other$totalPnl != null : !this$totalPnl.equals(other$totalPnl)) return false;
        final java.lang.Object this$avgPnl = this.getAvgPnl();
        final java.lang.Object other$avgPnl = other.getAvgPnl();
        if (this$avgPnl == null ? other$avgPnl != null : !this$avgPnl.equals(other$avgPnl)) return false;
        final java.lang.Object this$estimatedMonthlyImpact = this.getEstimatedMonthlyImpact();
        final java.lang.Object other$estimatedMonthlyImpact = other.getEstimatedMonthlyImpact();
        if (this$estimatedMonthlyImpact == null ? other$estimatedMonthlyImpact != null : !this$estimatedMonthlyImpact.equals(other$estimatedMonthlyImpact)) return false;
        final java.lang.Object this$totalR = this.getTotalR();
        final java.lang.Object other$totalR = other.getTotalR();
        if (this$totalR == null ? other$totalR != null : !this$totalR.equals(other$totalR)) return false;
        final java.lang.Object this$avgR = this.getAvgR();
        final java.lang.Object other$avgR = other.getAvgR();
        if (this$avgR == null ? other$avgR != null : !this$avgR.equals(other$avgR)) return false;
        final java.lang.Object this$estimatedMonthlyImpactR = this.getEstimatedMonthlyImpactR();
        final java.lang.Object other$estimatedMonthlyImpactR = other.getEstimatedMonthlyImpactR();
        if (this$estimatedMonthlyImpactR == null ? other$estimatedMonthlyImpactR != null : !this$estimatedMonthlyImpactR.equals(other$estimatedMonthlyImpactR)) return false;
        final java.lang.Object this$severity = this.getSeverity();
        final java.lang.Object other$severity = other.getSeverity();
        if (this$severity == null ? other$severity != null : !this$severity.equals(other$severity)) return false;
        final java.lang.Object this$recommendedFix = this.getRecommendedFix();
        final java.lang.Object other$recommendedFix = other.getRecommendedFix();
        if (this$recommendedFix == null ? other$recommendedFix != null : !this$recommendedFix.equals(other$recommendedFix)) return false;
        final java.lang.Object this$relatedTradeIds = this.getRelatedTradeIds();
        final java.lang.Object other$relatedTradeIds = other.getRelatedTradeIds();
        if (this$relatedTradeIds == null ? other$relatedTradeIds != null : !this$relatedTradeIds.equals(other$relatedTradeIds)) return false;
        final java.lang.Object this$generatedAt = this.getGeneratedAt();
        final java.lang.Object other$generatedAt = other.getGeneratedAt();
        if (this$generatedAt == null ? other$generatedAt != null : !this$generatedAt.equals(other$generatedAt)) return false;
        return true;
    }

    @java.lang.SuppressWarnings("all")
    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof LeakResultDto;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $tradeCount = this.getTradeCount();
        result = result * PRIME + ($tradeCount == null ? 43 : $tradeCount.hashCode());
        final java.lang.Object $hasValidRMultiple = this.getHasValidRMultiple();
        result = result * PRIME + ($hasValidRMultiple == null ? 43 : $hasValidRMultiple.hashCode());
        final java.lang.Object $confidenceScore = this.getConfidenceScore();
        result = result * PRIME + ($confidenceScore == null ? 43 : $confidenceScore.hashCode());
        final java.lang.Object $relatedTradeCount = this.getRelatedTradeCount();
        result = result * PRIME + ($relatedTradeCount == null ? 43 : $relatedTradeCount.hashCode());
        final java.lang.Object $leakType = this.getLeakType();
        result = result * PRIME + ($leakType == null ? 43 : $leakType.hashCode());
        final java.lang.Object $title = this.getTitle();
        result = result * PRIME + ($title == null ? 43 : $title.hashCode());
        final java.lang.Object $summary = this.getSummary();
        result = result * PRIME + ($summary == null ? 43 : $summary.hashCode());
        final java.lang.Object $description = this.getDescription();
        result = result * PRIME + ($description == null ? 43 : $description.hashCode());
        final java.lang.Object $winRate = this.getWinRate();
        result = result * PRIME + ($winRate == null ? 43 : $winRate.hashCode());
        final java.lang.Object $totalPnl = this.getTotalPnl();
        result = result * PRIME + ($totalPnl == null ? 43 : $totalPnl.hashCode());
        final java.lang.Object $avgPnl = this.getAvgPnl();
        result = result * PRIME + ($avgPnl == null ? 43 : $avgPnl.hashCode());
        final java.lang.Object $estimatedMonthlyImpact = this.getEstimatedMonthlyImpact();
        result = result * PRIME + ($estimatedMonthlyImpact == null ? 43 : $estimatedMonthlyImpact.hashCode());
        final java.lang.Object $totalR = this.getTotalR();
        result = result * PRIME + ($totalR == null ? 43 : $totalR.hashCode());
        final java.lang.Object $avgR = this.getAvgR();
        result = result * PRIME + ($avgR == null ? 43 : $avgR.hashCode());
        final java.lang.Object $estimatedMonthlyImpactR = this.getEstimatedMonthlyImpactR();
        result = result * PRIME + ($estimatedMonthlyImpactR == null ? 43 : $estimatedMonthlyImpactR.hashCode());
        final java.lang.Object $severity = this.getSeverity();
        result = result * PRIME + ($severity == null ? 43 : $severity.hashCode());
        final java.lang.Object $recommendedFix = this.getRecommendedFix();
        result = result * PRIME + ($recommendedFix == null ? 43 : $recommendedFix.hashCode());
        final java.lang.Object $relatedTradeIds = this.getRelatedTradeIds();
        result = result * PRIME + ($relatedTradeIds == null ? 43 : $relatedTradeIds.hashCode());
        final java.lang.Object $generatedAt = this.getGeneratedAt();
        result = result * PRIME + ($generatedAt == null ? 43 : $generatedAt.hashCode());
        return result;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public java.lang.String toString() {
        return "LeakResultDto(leakType=" + this.getLeakType() + ", title=" + this.getTitle() + ", summary=" + this.getSummary() + ", description=" + this.getDescription() + ", tradeCount=" + this.getTradeCount() + ", winRate=" + this.getWinRate() + ", totalPnl=" + this.getTotalPnl() + ", avgPnl=" + this.getAvgPnl() + ", estimatedMonthlyImpact=" + this.getEstimatedMonthlyImpact() + ", totalR=" + this.getTotalR() + ", avgR=" + this.getAvgR() + ", estimatedMonthlyImpactR=" + this.getEstimatedMonthlyImpactR() + ", hasValidRMultiple=" + this.getHasValidRMultiple() + ", confidenceScore=" + this.getConfidenceScore() + ", severity=" + this.getSeverity() + ", recommendedFix=" + this.getRecommendedFix() + ", relatedTradeCount=" + this.getRelatedTradeCount() + ", relatedTradeIds=" + this.getRelatedTradeIds() + ", generatedAt=" + this.getGeneratedAt() + ")";
    }

    @java.lang.SuppressWarnings("all")
    public LeakResultDto() {
    }
}
