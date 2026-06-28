package com.tradepilot.leakdetector.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class LeakDetectorResponse {
    private Long id;
    private Long accountId;
    private Boolean cached;
    private Boolean aiGenerated;
    private Boolean noTrades;
    private Boolean limitedData;
    private Integer creditsConsumed;
    private Integer totalTrades;
    private Integer sampleTradeCount;
    private String dataHash;
    private List<LeakResultDto> leaks;
    private String message;
    private String biggestLeakTitle;
    private BigDecimal biggestLeakImpact;
    private String biggestLeakFix;
    private BigDecimal totalEstimatedMonthlyImpact;
    private Integer highestConfidence;
    private LocalDateTime generatedAt;

    public static LeakDetectorResponse of(Long id, Long accountId, Boolean cached, Boolean aiGenerated, Boolean noTrades, Boolean limitedData, Integer creditsConsumed, Integer totalTrades, Integer sampleTradeCount, String dataHash, List<LeakResultDto> leaks, String message, String biggestLeakTitle, BigDecimal biggestLeakImpact, String biggestLeakFix, BigDecimal totalEstimatedMonthlyImpact, Integer highestConfidence, LocalDateTime generatedAt) {
        LeakDetectorResponse response = new LeakDetectorResponse();
        response.setId(id);
        response.setAccountId(accountId);
        response.setCached(cached);
        response.setAiGenerated(aiGenerated);
        response.setNoTrades(noTrades);
        response.setLimitedData(limitedData);
        response.setCreditsConsumed(creditsConsumed);
        response.setTotalTrades(totalTrades);
        response.setSampleTradeCount(sampleTradeCount);
        response.setDataHash(dataHash);
        response.setLeaks(leaks);
        response.setMessage(message);
        response.setBiggestLeakTitle(biggestLeakTitle);
        response.setBiggestLeakImpact(biggestLeakImpact);
        response.setBiggestLeakFix(biggestLeakFix);
        response.setTotalEstimatedMonthlyImpact(totalEstimatedMonthlyImpact);
        response.setHighestConfidence(highestConfidence);
        response.setGeneratedAt(generatedAt);
        return response;
    }

    @java.lang.SuppressWarnings("all")
    public Long getId() {
        return this.id;
    }

    @java.lang.SuppressWarnings("all")
    public Long getAccountId() {
        return this.accountId;
    }

    @java.lang.SuppressWarnings("all")
    public Boolean getCached() {
        return this.cached;
    }

    @java.lang.SuppressWarnings("all")
    public Boolean getAiGenerated() {
        return this.aiGenerated;
    }

    @java.lang.SuppressWarnings("all")
    public Boolean getNoTrades() {
        return this.noTrades;
    }

    @java.lang.SuppressWarnings("all")
    public Boolean getLimitedData() {
        return this.limitedData;
    }

    @java.lang.SuppressWarnings("all")
    public Integer getCreditsConsumed() {
        return this.creditsConsumed;
    }

    @java.lang.SuppressWarnings("all")
    public Integer getTotalTrades() {
        return this.totalTrades;
    }

    @java.lang.SuppressWarnings("all")
    public Integer getSampleTradeCount() {
        return this.sampleTradeCount;
    }

    @java.lang.SuppressWarnings("all")
    public String getDataHash() {
        return this.dataHash;
    }

    @java.lang.SuppressWarnings("all")
    public List<LeakResultDto> getLeaks() {
        return this.leaks;
    }

    @java.lang.SuppressWarnings("all")
    public String getMessage() {
        return this.message;
    }

    @java.lang.SuppressWarnings("all")
    public String getBiggestLeakTitle() {
        return this.biggestLeakTitle;
    }

    @java.lang.SuppressWarnings("all")
    public BigDecimal getBiggestLeakImpact() {
        return this.biggestLeakImpact;
    }

    @java.lang.SuppressWarnings("all")
    public String getBiggestLeakFix() {
        return this.biggestLeakFix;
    }

    @java.lang.SuppressWarnings("all")
    public BigDecimal getTotalEstimatedMonthlyImpact() {
        return this.totalEstimatedMonthlyImpact;
    }

    @java.lang.SuppressWarnings("all")
    public Integer getHighestConfidence() {
        return this.highestConfidence;
    }

    @java.lang.SuppressWarnings("all")
    public LocalDateTime getGeneratedAt() {
        return this.generatedAt;
    }

    @java.lang.SuppressWarnings("all")
    public void setId(final Long id) {
        this.id = id;
    }

    @java.lang.SuppressWarnings("all")
    public void setAccountId(final Long accountId) {
        this.accountId = accountId;
    }

    @java.lang.SuppressWarnings("all")
    public void setCached(final Boolean cached) {
        this.cached = cached;
    }

    @java.lang.SuppressWarnings("all")
    public void setAiGenerated(final Boolean aiGenerated) {
        this.aiGenerated = aiGenerated;
    }

    @java.lang.SuppressWarnings("all")
    public void setNoTrades(final Boolean noTrades) {
        this.noTrades = noTrades;
    }

    @java.lang.SuppressWarnings("all")
    public void setLimitedData(final Boolean limitedData) {
        this.limitedData = limitedData;
    }

    @java.lang.SuppressWarnings("all")
    public void setCreditsConsumed(final Integer creditsConsumed) {
        this.creditsConsumed = creditsConsumed;
    }

    @java.lang.SuppressWarnings("all")
    public void setTotalTrades(final Integer totalTrades) {
        this.totalTrades = totalTrades;
    }

    @java.lang.SuppressWarnings("all")
    public void setSampleTradeCount(final Integer sampleTradeCount) {
        this.sampleTradeCount = sampleTradeCount;
    }

    @java.lang.SuppressWarnings("all")
    public void setDataHash(final String dataHash) {
        this.dataHash = dataHash;
    }

    @java.lang.SuppressWarnings("all")
    public void setLeaks(final List<LeakResultDto> leaks) {
        this.leaks = leaks;
    }

    @java.lang.SuppressWarnings("all")
    public void setMessage(final String message) {
        this.message = message;
    }

    @java.lang.SuppressWarnings("all")
    public void setBiggestLeakTitle(final String biggestLeakTitle) {
        this.biggestLeakTitle = biggestLeakTitle;
    }

    @java.lang.SuppressWarnings("all")
    public void setBiggestLeakImpact(final BigDecimal biggestLeakImpact) {
        this.biggestLeakImpact = biggestLeakImpact;
    }

    @java.lang.SuppressWarnings("all")
    public void setBiggestLeakFix(final String biggestLeakFix) {
        this.biggestLeakFix = biggestLeakFix;
    }

    @java.lang.SuppressWarnings("all")
    public void setTotalEstimatedMonthlyImpact(final BigDecimal totalEstimatedMonthlyImpact) {
        this.totalEstimatedMonthlyImpact = totalEstimatedMonthlyImpact;
    }

    @java.lang.SuppressWarnings("all")
    public void setHighestConfidence(final Integer highestConfidence) {
        this.highestConfidence = highestConfidence;
    }

    @java.lang.SuppressWarnings("all")
    public void setGeneratedAt(final LocalDateTime generatedAt) {
        this.generatedAt = generatedAt;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof LeakDetectorResponse)) return false;
        final LeakDetectorResponse other = (LeakDetectorResponse) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$id = this.getId();
        final java.lang.Object other$id = other.getId();
        if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
        final java.lang.Object this$accountId = this.getAccountId();
        final java.lang.Object other$accountId = other.getAccountId();
        if (this$accountId == null ? other$accountId != null : !this$accountId.equals(other$accountId)) return false;
        final java.lang.Object this$cached = this.getCached();
        final java.lang.Object other$cached = other.getCached();
        if (this$cached == null ? other$cached != null : !this$cached.equals(other$cached)) return false;
        final java.lang.Object this$aiGenerated = this.getAiGenerated();
        final java.lang.Object other$aiGenerated = other.getAiGenerated();
        if (this$aiGenerated == null ? other$aiGenerated != null : !this$aiGenerated.equals(other$aiGenerated)) return false;
        final java.lang.Object this$noTrades = this.getNoTrades();
        final java.lang.Object other$noTrades = other.getNoTrades();
        if (this$noTrades == null ? other$noTrades != null : !this$noTrades.equals(other$noTrades)) return false;
        final java.lang.Object this$limitedData = this.getLimitedData();
        final java.lang.Object other$limitedData = other.getLimitedData();
        if (this$limitedData == null ? other$limitedData != null : !this$limitedData.equals(other$limitedData)) return false;
        final java.lang.Object this$creditsConsumed = this.getCreditsConsumed();
        final java.lang.Object other$creditsConsumed = other.getCreditsConsumed();
        if (this$creditsConsumed == null ? other$creditsConsumed != null : !this$creditsConsumed.equals(other$creditsConsumed)) return false;
        final java.lang.Object this$totalTrades = this.getTotalTrades();
        final java.lang.Object other$totalTrades = other.getTotalTrades();
        if (this$totalTrades == null ? other$totalTrades != null : !this$totalTrades.equals(other$totalTrades)) return false;
        final java.lang.Object this$sampleTradeCount = this.getSampleTradeCount();
        final java.lang.Object other$sampleTradeCount = other.getSampleTradeCount();
        if (this$sampleTradeCount == null ? other$sampleTradeCount != null : !this$sampleTradeCount.equals(other$sampleTradeCount)) return false;
        final java.lang.Object this$highestConfidence = this.getHighestConfidence();
        final java.lang.Object other$highestConfidence = other.getHighestConfidence();
        if (this$highestConfidence == null ? other$highestConfidence != null : !this$highestConfidence.equals(other$highestConfidence)) return false;
        final java.lang.Object this$dataHash = this.getDataHash();
        final java.lang.Object other$dataHash = other.getDataHash();
        if (this$dataHash == null ? other$dataHash != null : !this$dataHash.equals(other$dataHash)) return false;
        final java.lang.Object this$leaks = this.getLeaks();
        final java.lang.Object other$leaks = other.getLeaks();
        if (this$leaks == null ? other$leaks != null : !this$leaks.equals(other$leaks)) return false;
        final java.lang.Object this$message = this.getMessage();
        final java.lang.Object other$message = other.getMessage();
        if (this$message == null ? other$message != null : !this$message.equals(other$message)) return false;
        final java.lang.Object this$biggestLeakTitle = this.getBiggestLeakTitle();
        final java.lang.Object other$biggestLeakTitle = other.getBiggestLeakTitle();
        if (this$biggestLeakTitle == null ? other$biggestLeakTitle != null : !this$biggestLeakTitle.equals(other$biggestLeakTitle)) return false;
        final java.lang.Object this$biggestLeakImpact = this.getBiggestLeakImpact();
        final java.lang.Object other$biggestLeakImpact = other.getBiggestLeakImpact();
        if (this$biggestLeakImpact == null ? other$biggestLeakImpact != null : !this$biggestLeakImpact.equals(other$biggestLeakImpact)) return false;
        final java.lang.Object this$biggestLeakFix = this.getBiggestLeakFix();
        final java.lang.Object other$biggestLeakFix = other.getBiggestLeakFix();
        if (this$biggestLeakFix == null ? other$biggestLeakFix != null : !this$biggestLeakFix.equals(other$biggestLeakFix)) return false;
        final java.lang.Object this$totalEstimatedMonthlyImpact = this.getTotalEstimatedMonthlyImpact();
        final java.lang.Object other$totalEstimatedMonthlyImpact = other.getTotalEstimatedMonthlyImpact();
        if (this$totalEstimatedMonthlyImpact == null ? other$totalEstimatedMonthlyImpact != null : !this$totalEstimatedMonthlyImpact.equals(other$totalEstimatedMonthlyImpact)) return false;
        final java.lang.Object this$generatedAt = this.getGeneratedAt();
        final java.lang.Object other$generatedAt = other.getGeneratedAt();
        if (this$generatedAt == null ? other$generatedAt != null : !this$generatedAt.equals(other$generatedAt)) return false;
        return true;
    }

    @java.lang.SuppressWarnings("all")
    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof LeakDetectorResponse;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $id = this.getId();
        result = result * PRIME + ($id == null ? 43 : $id.hashCode());
        final java.lang.Object $accountId = this.getAccountId();
        result = result * PRIME + ($accountId == null ? 43 : $accountId.hashCode());
        final java.lang.Object $cached = this.getCached();
        result = result * PRIME + ($cached == null ? 43 : $cached.hashCode());
        final java.lang.Object $aiGenerated = this.getAiGenerated();
        result = result * PRIME + ($aiGenerated == null ? 43 : $aiGenerated.hashCode());
        final java.lang.Object $noTrades = this.getNoTrades();
        result = result * PRIME + ($noTrades == null ? 43 : $noTrades.hashCode());
        final java.lang.Object $limitedData = this.getLimitedData();
        result = result * PRIME + ($limitedData == null ? 43 : $limitedData.hashCode());
        final java.lang.Object $creditsConsumed = this.getCreditsConsumed();
        result = result * PRIME + ($creditsConsumed == null ? 43 : $creditsConsumed.hashCode());
        final java.lang.Object $totalTrades = this.getTotalTrades();
        result = result * PRIME + ($totalTrades == null ? 43 : $totalTrades.hashCode());
        final java.lang.Object $sampleTradeCount = this.getSampleTradeCount();
        result = result * PRIME + ($sampleTradeCount == null ? 43 : $sampleTradeCount.hashCode());
        final java.lang.Object $highestConfidence = this.getHighestConfidence();
        result = result * PRIME + ($highestConfidence == null ? 43 : $highestConfidence.hashCode());
        final java.lang.Object $dataHash = this.getDataHash();
        result = result * PRIME + ($dataHash == null ? 43 : $dataHash.hashCode());
        final java.lang.Object $leaks = this.getLeaks();
        result = result * PRIME + ($leaks == null ? 43 : $leaks.hashCode());
        final java.lang.Object $message = this.getMessage();
        result = result * PRIME + ($message == null ? 43 : $message.hashCode());
        final java.lang.Object $biggestLeakTitle = this.getBiggestLeakTitle();
        result = result * PRIME + ($biggestLeakTitle == null ? 43 : $biggestLeakTitle.hashCode());
        final java.lang.Object $biggestLeakImpact = this.getBiggestLeakImpact();
        result = result * PRIME + ($biggestLeakImpact == null ? 43 : $biggestLeakImpact.hashCode());
        final java.lang.Object $biggestLeakFix = this.getBiggestLeakFix();
        result = result * PRIME + ($biggestLeakFix == null ? 43 : $biggestLeakFix.hashCode());
        final java.lang.Object $totalEstimatedMonthlyImpact = this.getTotalEstimatedMonthlyImpact();
        result = result * PRIME + ($totalEstimatedMonthlyImpact == null ? 43 : $totalEstimatedMonthlyImpact.hashCode());
        final java.lang.Object $generatedAt = this.getGeneratedAt();
        result = result * PRIME + ($generatedAt == null ? 43 : $generatedAt.hashCode());
        return result;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public java.lang.String toString() {
        return "LeakDetectorResponse(id=" + this.getId() + ", accountId=" + this.getAccountId() + ", cached=" + this.getCached() + ", aiGenerated=" + this.getAiGenerated() + ", noTrades=" + this.getNoTrades() + ", limitedData=" + this.getLimitedData() + ", creditsConsumed=" + this.getCreditsConsumed() + ", totalTrades=" + this.getTotalTrades() + ", sampleTradeCount=" + this.getSampleTradeCount() + ", dataHash=" + this.getDataHash() + ", leaks=" + this.getLeaks() + ", message=" + this.getMessage() + ", biggestLeakTitle=" + this.getBiggestLeakTitle() + ", biggestLeakImpact=" + this.getBiggestLeakImpact() + ", biggestLeakFix=" + this.getBiggestLeakFix() + ", totalEstimatedMonthlyImpact=" + this.getTotalEstimatedMonthlyImpact() + ", highestConfidence=" + this.getHighestConfidence() + ", generatedAt=" + this.getGeneratedAt() + ")";
    }

    @java.lang.SuppressWarnings("all")
    public LeakDetectorResponse() {
    }
}
