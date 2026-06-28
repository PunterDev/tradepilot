package com.tradepilot.coach.dto;

import com.tradepilot.coach.entity.CoachSnapshotStatus;
import com.tradepilot.coach.entity.CoachSnapshotType;
import com.tradepilot.leakdetector.dto.LeakResultDto;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class CoachSnapshotDto {
    private Long id;
    private Long accountId;
    private CoachSnapshotType snapshotType;
    private CoachSnapshotStatus status;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private Integer totalTrades;
    private BigDecimal netPnl;
    private BigDecimal winRate;
    private BigDecimal profitFactor;
    private String bestSetup;
    private String worstSetup;
    private String bestSession;
    private String worstSession;
    private String biggestMistake;
    private String deterministicSummaryJson;
    private String aiResponseJson;
    private String errorMessage;
    private LeakResultDto topLeakToWatch;
    private String selectedDaySummary;
    private String primaryIssueToday;
    private String setupDirectionReview;
    private Integer longTradeCount;
    private Integer shortTradeCount;
    private Integer unknownTradeCount;
    private BigDecimal longPnl;
    private BigDecimal shortPnl;
    private String worstTradeSide;
    private String historicalPatternToWatch;
    private Boolean historicalPatternAppliesToday;
    private String dataQuality;
    private String dataQualityReason;
    private String nextSessionRule;
    private Boolean aiGenerated;
    private Boolean noTradeDay;
    private Boolean noTradeWeek;
    private Integer creditsConsumed;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @java.lang.SuppressWarnings("all")
    public CoachSnapshotDto() {
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
    public CoachSnapshotType getSnapshotType() {
        return this.snapshotType;
    }

    @java.lang.SuppressWarnings("all")
    public CoachSnapshotStatus getStatus() {
        return this.status;
    }

    @java.lang.SuppressWarnings("all")
    public LocalDate getPeriodStart() {
        return this.periodStart;
    }

    @java.lang.SuppressWarnings("all")
    public LocalDate getPeriodEnd() {
        return this.periodEnd;
    }

    @java.lang.SuppressWarnings("all")
    public Integer getTotalTrades() {
        return this.totalTrades;
    }

    @java.lang.SuppressWarnings("all")
    public BigDecimal getNetPnl() {
        return this.netPnl;
    }

    @java.lang.SuppressWarnings("all")
    public BigDecimal getWinRate() {
        return this.winRate;
    }

    @java.lang.SuppressWarnings("all")
    public BigDecimal getProfitFactor() {
        return this.profitFactor;
    }

    @java.lang.SuppressWarnings("all")
    public String getBestSetup() {
        return this.bestSetup;
    }

    @java.lang.SuppressWarnings("all")
    public String getWorstSetup() {
        return this.worstSetup;
    }

    @java.lang.SuppressWarnings("all")
    public String getBestSession() {
        return this.bestSession;
    }

    @java.lang.SuppressWarnings("all")
    public String getWorstSession() {
        return this.worstSession;
    }

    @java.lang.SuppressWarnings("all")
    public String getBiggestMistake() {
        return this.biggestMistake;
    }

    @java.lang.SuppressWarnings("all")
    public String getDeterministicSummaryJson() {
        return this.deterministicSummaryJson;
    }

    @java.lang.SuppressWarnings("all")
    public String getAiResponseJson() {
        return this.aiResponseJson;
    }

    @java.lang.SuppressWarnings("all")
    public String getErrorMessage() {
        return this.errorMessage;
    }

    @java.lang.SuppressWarnings("all")
    public LeakResultDto getTopLeakToWatch() {
        return this.topLeakToWatch;
    }

    @java.lang.SuppressWarnings("all")
    public String getSelectedDaySummary() {
        return this.selectedDaySummary;
    }

    @java.lang.SuppressWarnings("all")
    public String getPrimaryIssueToday() {
        return this.primaryIssueToday;
    }

    @java.lang.SuppressWarnings("all")
    public String getSetupDirectionReview() {
        return this.setupDirectionReview;
    }

    @java.lang.SuppressWarnings("all")
    public Integer getLongTradeCount() {
        return this.longTradeCount;
    }

    @java.lang.SuppressWarnings("all")
    public Integer getShortTradeCount() {
        return this.shortTradeCount;
    }

    @java.lang.SuppressWarnings("all")
    public Integer getUnknownTradeCount() {
        return this.unknownTradeCount;
    }

    @java.lang.SuppressWarnings("all")
    public BigDecimal getLongPnl() {
        return this.longPnl;
    }

    @java.lang.SuppressWarnings("all")
    public BigDecimal getShortPnl() {
        return this.shortPnl;
    }

    @java.lang.SuppressWarnings("all")
    public String getWorstTradeSide() {
        return this.worstTradeSide;
    }

    @java.lang.SuppressWarnings("all")
    public String getHistoricalPatternToWatch() {
        return this.historicalPatternToWatch;
    }

    @java.lang.SuppressWarnings("all")
    public Boolean getHistoricalPatternAppliesToday() {
        return this.historicalPatternAppliesToday;
    }

    @java.lang.SuppressWarnings("all")
    public String getDataQuality() {
        return this.dataQuality;
    }

    @java.lang.SuppressWarnings("all")
    public String getDataQualityReason() {
        return this.dataQualityReason;
    }

    @java.lang.SuppressWarnings("all")
    public String getNextSessionRule() {
        return this.nextSessionRule;
    }

    @java.lang.SuppressWarnings("all")
    public Boolean getAiGenerated() {
        return this.aiGenerated;
    }

    @java.lang.SuppressWarnings("all")
    public Boolean getNoTradeDay() {
        return this.noTradeDay;
    }

    @java.lang.SuppressWarnings("all")
    public Boolean getNoTradeWeek() {
        return this.noTradeWeek;
    }

    @java.lang.SuppressWarnings("all")
    public Integer getCreditsConsumed() {
        return this.creditsConsumed;
    }

    @java.lang.SuppressWarnings("all")
    public LocalDateTime getCreatedAt() {
        return this.createdAt;
    }

    @java.lang.SuppressWarnings("all")
    public LocalDateTime getUpdatedAt() {
        return this.updatedAt;
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
    public void setSnapshotType(final CoachSnapshotType snapshotType) {
        this.snapshotType = snapshotType;
    }

    @java.lang.SuppressWarnings("all")
    public void setStatus(final CoachSnapshotStatus status) {
        this.status = status;
    }

    @java.lang.SuppressWarnings("all")
    public void setPeriodStart(final LocalDate periodStart) {
        this.periodStart = periodStart;
    }

    @java.lang.SuppressWarnings("all")
    public void setPeriodEnd(final LocalDate periodEnd) {
        this.periodEnd = periodEnd;
    }

    @java.lang.SuppressWarnings("all")
    public void setTotalTrades(final Integer totalTrades) {
        this.totalTrades = totalTrades;
    }

    @java.lang.SuppressWarnings("all")
    public void setNetPnl(final BigDecimal netPnl) {
        this.netPnl = netPnl;
    }

    @java.lang.SuppressWarnings("all")
    public void setWinRate(final BigDecimal winRate) {
        this.winRate = winRate;
    }

    @java.lang.SuppressWarnings("all")
    public void setProfitFactor(final BigDecimal profitFactor) {
        this.profitFactor = profitFactor;
    }

    @java.lang.SuppressWarnings("all")
    public void setBestSetup(final String bestSetup) {
        this.bestSetup = bestSetup;
    }

    @java.lang.SuppressWarnings("all")
    public void setWorstSetup(final String worstSetup) {
        this.worstSetup = worstSetup;
    }

    @java.lang.SuppressWarnings("all")
    public void setBestSession(final String bestSession) {
        this.bestSession = bestSession;
    }

    @java.lang.SuppressWarnings("all")
    public void setWorstSession(final String worstSession) {
        this.worstSession = worstSession;
    }

    @java.lang.SuppressWarnings("all")
    public void setBiggestMistake(final String biggestMistake) {
        this.biggestMistake = biggestMistake;
    }

    @java.lang.SuppressWarnings("all")
    public void setDeterministicSummaryJson(final String deterministicSummaryJson) {
        this.deterministicSummaryJson = deterministicSummaryJson;
    }

    @java.lang.SuppressWarnings("all")
    public void setAiResponseJson(final String aiResponseJson) {
        this.aiResponseJson = aiResponseJson;
    }

    @java.lang.SuppressWarnings("all")
    public void setErrorMessage(final String errorMessage) {
        this.errorMessage = errorMessage;
    }

    @java.lang.SuppressWarnings("all")
    public void setTopLeakToWatch(final LeakResultDto topLeakToWatch) {
        this.topLeakToWatch = topLeakToWatch;
    }

    @java.lang.SuppressWarnings("all")
    public void setSelectedDaySummary(final String selectedDaySummary) {
        this.selectedDaySummary = selectedDaySummary;
    }

    @java.lang.SuppressWarnings("all")
    public void setPrimaryIssueToday(final String primaryIssueToday) {
        this.primaryIssueToday = primaryIssueToday;
    }

    @java.lang.SuppressWarnings("all")
    public void setSetupDirectionReview(final String setupDirectionReview) {
        this.setupDirectionReview = setupDirectionReview;
    }

    @java.lang.SuppressWarnings("all")
    public void setLongTradeCount(final Integer longTradeCount) {
        this.longTradeCount = longTradeCount;
    }

    @java.lang.SuppressWarnings("all")
    public void setShortTradeCount(final Integer shortTradeCount) {
        this.shortTradeCount = shortTradeCount;
    }

    @java.lang.SuppressWarnings("all")
    public void setUnknownTradeCount(final Integer unknownTradeCount) {
        this.unknownTradeCount = unknownTradeCount;
    }

    @java.lang.SuppressWarnings("all")
    public void setLongPnl(final BigDecimal longPnl) {
        this.longPnl = longPnl;
    }

    @java.lang.SuppressWarnings("all")
    public void setShortPnl(final BigDecimal shortPnl) {
        this.shortPnl = shortPnl;
    }

    @java.lang.SuppressWarnings("all")
    public void setWorstTradeSide(final String worstTradeSide) {
        this.worstTradeSide = worstTradeSide;
    }

    @java.lang.SuppressWarnings("all")
    public void setHistoricalPatternToWatch(final String historicalPatternToWatch) {
        this.historicalPatternToWatch = historicalPatternToWatch;
    }

    @java.lang.SuppressWarnings("all")
    public void setHistoricalPatternAppliesToday(final Boolean historicalPatternAppliesToday) {
        this.historicalPatternAppliesToday = historicalPatternAppliesToday;
    }

    @java.lang.SuppressWarnings("all")
    public void setDataQuality(final String dataQuality) {
        this.dataQuality = dataQuality;
    }

    @java.lang.SuppressWarnings("all")
    public void setDataQualityReason(final String dataQualityReason) {
        this.dataQualityReason = dataQualityReason;
    }

    @java.lang.SuppressWarnings("all")
    public void setNextSessionRule(final String nextSessionRule) {
        this.nextSessionRule = nextSessionRule;
    }

    @java.lang.SuppressWarnings("all")
    public void setAiGenerated(final Boolean aiGenerated) {
        this.aiGenerated = aiGenerated;
    }

    @java.lang.SuppressWarnings("all")
    public void setNoTradeDay(final Boolean noTradeDay) {
        this.noTradeDay = noTradeDay;
    }

    @java.lang.SuppressWarnings("all")
    public void setNoTradeWeek(final Boolean noTradeWeek) {
        this.noTradeWeek = noTradeWeek;
    }

    @java.lang.SuppressWarnings("all")
    public void setCreditsConsumed(final Integer creditsConsumed) {
        this.creditsConsumed = creditsConsumed;
    }

    @java.lang.SuppressWarnings("all")
    public void setCreatedAt(final LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @java.lang.SuppressWarnings("all")
    public void setUpdatedAt(final LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof CoachSnapshotDto)) return false;
        final CoachSnapshotDto other = (CoachSnapshotDto) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$id = this.getId();
        final java.lang.Object other$id = other.getId();
        if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
        final java.lang.Object this$accountId = this.getAccountId();
        final java.lang.Object other$accountId = other.getAccountId();
        if (this$accountId == null ? other$accountId != null : !this$accountId.equals(other$accountId)) return false;
        final java.lang.Object this$totalTrades = this.getTotalTrades();
        final java.lang.Object other$totalTrades = other.getTotalTrades();
        if (this$totalTrades == null ? other$totalTrades != null : !this$totalTrades.equals(other$totalTrades)) return false;
        final java.lang.Object this$longTradeCount = this.getLongTradeCount();
        final java.lang.Object other$longTradeCount = other.getLongTradeCount();
        if (this$longTradeCount == null ? other$longTradeCount != null : !this$longTradeCount.equals(other$longTradeCount)) return false;
        final java.lang.Object this$shortTradeCount = this.getShortTradeCount();
        final java.lang.Object other$shortTradeCount = other.getShortTradeCount();
        if (this$shortTradeCount == null ? other$shortTradeCount != null : !this$shortTradeCount.equals(other$shortTradeCount)) return false;
        final java.lang.Object this$unknownTradeCount = this.getUnknownTradeCount();
        final java.lang.Object other$unknownTradeCount = other.getUnknownTradeCount();
        if (this$unknownTradeCount == null ? other$unknownTradeCount != null : !this$unknownTradeCount.equals(other$unknownTradeCount)) return false;
        final java.lang.Object this$historicalPatternAppliesToday = this.getHistoricalPatternAppliesToday();
        final java.lang.Object other$historicalPatternAppliesToday = other.getHistoricalPatternAppliesToday();
        if (this$historicalPatternAppliesToday == null ? other$historicalPatternAppliesToday != null : !this$historicalPatternAppliesToday.equals(other$historicalPatternAppliesToday)) return false;
        final java.lang.Object this$aiGenerated = this.getAiGenerated();
        final java.lang.Object other$aiGenerated = other.getAiGenerated();
        if (this$aiGenerated == null ? other$aiGenerated != null : !this$aiGenerated.equals(other$aiGenerated)) return false;
        final java.lang.Object this$noTradeDay = this.getNoTradeDay();
        final java.lang.Object other$noTradeDay = other.getNoTradeDay();
        if (this$noTradeDay == null ? other$noTradeDay != null : !this$noTradeDay.equals(other$noTradeDay)) return false;
        final java.lang.Object this$noTradeWeek = this.getNoTradeWeek();
        final java.lang.Object other$noTradeWeek = other.getNoTradeWeek();
        if (this$noTradeWeek == null ? other$noTradeWeek != null : !this$noTradeWeek.equals(other$noTradeWeek)) return false;
        final java.lang.Object this$creditsConsumed = this.getCreditsConsumed();
        final java.lang.Object other$creditsConsumed = other.getCreditsConsumed();
        if (this$creditsConsumed == null ? other$creditsConsumed != null : !this$creditsConsumed.equals(other$creditsConsumed)) return false;
        final java.lang.Object this$snapshotType = this.getSnapshotType();
        final java.lang.Object other$snapshotType = other.getSnapshotType();
        if (this$snapshotType == null ? other$snapshotType != null : !this$snapshotType.equals(other$snapshotType)) return false;
        final java.lang.Object this$status = this.getStatus();
        final java.lang.Object other$status = other.getStatus();
        if (this$status == null ? other$status != null : !this$status.equals(other$status)) return false;
        final java.lang.Object this$periodStart = this.getPeriodStart();
        final java.lang.Object other$periodStart = other.getPeriodStart();
        if (this$periodStart == null ? other$periodStart != null : !this$periodStart.equals(other$periodStart)) return false;
        final java.lang.Object this$periodEnd = this.getPeriodEnd();
        final java.lang.Object other$periodEnd = other.getPeriodEnd();
        if (this$periodEnd == null ? other$periodEnd != null : !this$periodEnd.equals(other$periodEnd)) return false;
        final java.lang.Object this$netPnl = this.getNetPnl();
        final java.lang.Object other$netPnl = other.getNetPnl();
        if (this$netPnl == null ? other$netPnl != null : !this$netPnl.equals(other$netPnl)) return false;
        final java.lang.Object this$winRate = this.getWinRate();
        final java.lang.Object other$winRate = other.getWinRate();
        if (this$winRate == null ? other$winRate != null : !this$winRate.equals(other$winRate)) return false;
        final java.lang.Object this$profitFactor = this.getProfitFactor();
        final java.lang.Object other$profitFactor = other.getProfitFactor();
        if (this$profitFactor == null ? other$profitFactor != null : !this$profitFactor.equals(other$profitFactor)) return false;
        final java.lang.Object this$bestSetup = this.getBestSetup();
        final java.lang.Object other$bestSetup = other.getBestSetup();
        if (this$bestSetup == null ? other$bestSetup != null : !this$bestSetup.equals(other$bestSetup)) return false;
        final java.lang.Object this$worstSetup = this.getWorstSetup();
        final java.lang.Object other$worstSetup = other.getWorstSetup();
        if (this$worstSetup == null ? other$worstSetup != null : !this$worstSetup.equals(other$worstSetup)) return false;
        final java.lang.Object this$bestSession = this.getBestSession();
        final java.lang.Object other$bestSession = other.getBestSession();
        if (this$bestSession == null ? other$bestSession != null : !this$bestSession.equals(other$bestSession)) return false;
        final java.lang.Object this$worstSession = this.getWorstSession();
        final java.lang.Object other$worstSession = other.getWorstSession();
        if (this$worstSession == null ? other$worstSession != null : !this$worstSession.equals(other$worstSession)) return false;
        final java.lang.Object this$biggestMistake = this.getBiggestMistake();
        final java.lang.Object other$biggestMistake = other.getBiggestMistake();
        if (this$biggestMistake == null ? other$biggestMistake != null : !this$biggestMistake.equals(other$biggestMistake)) return false;
        final java.lang.Object this$deterministicSummaryJson = this.getDeterministicSummaryJson();
        final java.lang.Object other$deterministicSummaryJson = other.getDeterministicSummaryJson();
        if (this$deterministicSummaryJson == null ? other$deterministicSummaryJson != null : !this$deterministicSummaryJson.equals(other$deterministicSummaryJson)) return false;
        final java.lang.Object this$aiResponseJson = this.getAiResponseJson();
        final java.lang.Object other$aiResponseJson = other.getAiResponseJson();
        if (this$aiResponseJson == null ? other$aiResponseJson != null : !this$aiResponseJson.equals(other$aiResponseJson)) return false;
        final java.lang.Object this$errorMessage = this.getErrorMessage();
        final java.lang.Object other$errorMessage = other.getErrorMessage();
        if (this$errorMessage == null ? other$errorMessage != null : !this$errorMessage.equals(other$errorMessage)) return false;
        final java.lang.Object this$topLeakToWatch = this.getTopLeakToWatch();
        final java.lang.Object other$topLeakToWatch = other.getTopLeakToWatch();
        if (this$topLeakToWatch == null ? other$topLeakToWatch != null : !this$topLeakToWatch.equals(other$topLeakToWatch)) return false;
        final java.lang.Object this$selectedDaySummary = this.getSelectedDaySummary();
        final java.lang.Object other$selectedDaySummary = other.getSelectedDaySummary();
        if (this$selectedDaySummary == null ? other$selectedDaySummary != null : !this$selectedDaySummary.equals(other$selectedDaySummary)) return false;
        final java.lang.Object this$primaryIssueToday = this.getPrimaryIssueToday();
        final java.lang.Object other$primaryIssueToday = other.getPrimaryIssueToday();
        if (this$primaryIssueToday == null ? other$primaryIssueToday != null : !this$primaryIssueToday.equals(other$primaryIssueToday)) return false;
        final java.lang.Object this$setupDirectionReview = this.getSetupDirectionReview();
        final java.lang.Object other$setupDirectionReview = other.getSetupDirectionReview();
        if (this$setupDirectionReview == null ? other$setupDirectionReview != null : !this$setupDirectionReview.equals(other$setupDirectionReview)) return false;
        final java.lang.Object this$longPnl = this.getLongPnl();
        final java.lang.Object other$longPnl = other.getLongPnl();
        if (this$longPnl == null ? other$longPnl != null : !this$longPnl.equals(other$longPnl)) return false;
        final java.lang.Object this$shortPnl = this.getShortPnl();
        final java.lang.Object other$shortPnl = other.getShortPnl();
        if (this$shortPnl == null ? other$shortPnl != null : !this$shortPnl.equals(other$shortPnl)) return false;
        final java.lang.Object this$worstTradeSide = this.getWorstTradeSide();
        final java.lang.Object other$worstTradeSide = other.getWorstTradeSide();
        if (this$worstTradeSide == null ? other$worstTradeSide != null : !this$worstTradeSide.equals(other$worstTradeSide)) return false;
        final java.lang.Object this$historicalPatternToWatch = this.getHistoricalPatternToWatch();
        final java.lang.Object other$historicalPatternToWatch = other.getHistoricalPatternToWatch();
        if (this$historicalPatternToWatch == null ? other$historicalPatternToWatch != null : !this$historicalPatternToWatch.equals(other$historicalPatternToWatch)) return false;
        final java.lang.Object this$dataQuality = this.getDataQuality();
        final java.lang.Object other$dataQuality = other.getDataQuality();
        if (this$dataQuality == null ? other$dataQuality != null : !this$dataQuality.equals(other$dataQuality)) return false;
        final java.lang.Object this$dataQualityReason = this.getDataQualityReason();
        final java.lang.Object other$dataQualityReason = other.getDataQualityReason();
        if (this$dataQualityReason == null ? other$dataQualityReason != null : !this$dataQualityReason.equals(other$dataQualityReason)) return false;
        final java.lang.Object this$nextSessionRule = this.getNextSessionRule();
        final java.lang.Object other$nextSessionRule = other.getNextSessionRule();
        if (this$nextSessionRule == null ? other$nextSessionRule != null : !this$nextSessionRule.equals(other$nextSessionRule)) return false;
        final java.lang.Object this$createdAt = this.getCreatedAt();
        final java.lang.Object other$createdAt = other.getCreatedAt();
        if (this$createdAt == null ? other$createdAt != null : !this$createdAt.equals(other$createdAt)) return false;
        final java.lang.Object this$updatedAt = this.getUpdatedAt();
        final java.lang.Object other$updatedAt = other.getUpdatedAt();
        if (this$updatedAt == null ? other$updatedAt != null : !this$updatedAt.equals(other$updatedAt)) return false;
        return true;
    }

    @java.lang.SuppressWarnings("all")
    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof CoachSnapshotDto;
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
        final java.lang.Object $totalTrades = this.getTotalTrades();
        result = result * PRIME + ($totalTrades == null ? 43 : $totalTrades.hashCode());
        final java.lang.Object $longTradeCount = this.getLongTradeCount();
        result = result * PRIME + ($longTradeCount == null ? 43 : $longTradeCount.hashCode());
        final java.lang.Object $shortTradeCount = this.getShortTradeCount();
        result = result * PRIME + ($shortTradeCount == null ? 43 : $shortTradeCount.hashCode());
        final java.lang.Object $unknownTradeCount = this.getUnknownTradeCount();
        result = result * PRIME + ($unknownTradeCount == null ? 43 : $unknownTradeCount.hashCode());
        final java.lang.Object $historicalPatternAppliesToday = this.getHistoricalPatternAppliesToday();
        result = result * PRIME + ($historicalPatternAppliesToday == null ? 43 : $historicalPatternAppliesToday.hashCode());
        final java.lang.Object $aiGenerated = this.getAiGenerated();
        result = result * PRIME + ($aiGenerated == null ? 43 : $aiGenerated.hashCode());
        final java.lang.Object $noTradeDay = this.getNoTradeDay();
        result = result * PRIME + ($noTradeDay == null ? 43 : $noTradeDay.hashCode());
        final java.lang.Object $noTradeWeek = this.getNoTradeWeek();
        result = result * PRIME + ($noTradeWeek == null ? 43 : $noTradeWeek.hashCode());
        final java.lang.Object $creditsConsumed = this.getCreditsConsumed();
        result = result * PRIME + ($creditsConsumed == null ? 43 : $creditsConsumed.hashCode());
        final java.lang.Object $snapshotType = this.getSnapshotType();
        result = result * PRIME + ($snapshotType == null ? 43 : $snapshotType.hashCode());
        final java.lang.Object $status = this.getStatus();
        result = result * PRIME + ($status == null ? 43 : $status.hashCode());
        final java.lang.Object $periodStart = this.getPeriodStart();
        result = result * PRIME + ($periodStart == null ? 43 : $periodStart.hashCode());
        final java.lang.Object $periodEnd = this.getPeriodEnd();
        result = result * PRIME + ($periodEnd == null ? 43 : $periodEnd.hashCode());
        final java.lang.Object $netPnl = this.getNetPnl();
        result = result * PRIME + ($netPnl == null ? 43 : $netPnl.hashCode());
        final java.lang.Object $winRate = this.getWinRate();
        result = result * PRIME + ($winRate == null ? 43 : $winRate.hashCode());
        final java.lang.Object $profitFactor = this.getProfitFactor();
        result = result * PRIME + ($profitFactor == null ? 43 : $profitFactor.hashCode());
        final java.lang.Object $bestSetup = this.getBestSetup();
        result = result * PRIME + ($bestSetup == null ? 43 : $bestSetup.hashCode());
        final java.lang.Object $worstSetup = this.getWorstSetup();
        result = result * PRIME + ($worstSetup == null ? 43 : $worstSetup.hashCode());
        final java.lang.Object $bestSession = this.getBestSession();
        result = result * PRIME + ($bestSession == null ? 43 : $bestSession.hashCode());
        final java.lang.Object $worstSession = this.getWorstSession();
        result = result * PRIME + ($worstSession == null ? 43 : $worstSession.hashCode());
        final java.lang.Object $biggestMistake = this.getBiggestMistake();
        result = result * PRIME + ($biggestMistake == null ? 43 : $biggestMistake.hashCode());
        final java.lang.Object $deterministicSummaryJson = this.getDeterministicSummaryJson();
        result = result * PRIME + ($deterministicSummaryJson == null ? 43 : $deterministicSummaryJson.hashCode());
        final java.lang.Object $aiResponseJson = this.getAiResponseJson();
        result = result * PRIME + ($aiResponseJson == null ? 43 : $aiResponseJson.hashCode());
        final java.lang.Object $errorMessage = this.getErrorMessage();
        result = result * PRIME + ($errorMessage == null ? 43 : $errorMessage.hashCode());
        final java.lang.Object $topLeakToWatch = this.getTopLeakToWatch();
        result = result * PRIME + ($topLeakToWatch == null ? 43 : $topLeakToWatch.hashCode());
        final java.lang.Object $selectedDaySummary = this.getSelectedDaySummary();
        result = result * PRIME + ($selectedDaySummary == null ? 43 : $selectedDaySummary.hashCode());
        final java.lang.Object $primaryIssueToday = this.getPrimaryIssueToday();
        result = result * PRIME + ($primaryIssueToday == null ? 43 : $primaryIssueToday.hashCode());
        final java.lang.Object $setupDirectionReview = this.getSetupDirectionReview();
        result = result * PRIME + ($setupDirectionReview == null ? 43 : $setupDirectionReview.hashCode());
        final java.lang.Object $longPnl = this.getLongPnl();
        result = result * PRIME + ($longPnl == null ? 43 : $longPnl.hashCode());
        final java.lang.Object $shortPnl = this.getShortPnl();
        result = result * PRIME + ($shortPnl == null ? 43 : $shortPnl.hashCode());
        final java.lang.Object $worstTradeSide = this.getWorstTradeSide();
        result = result * PRIME + ($worstTradeSide == null ? 43 : $worstTradeSide.hashCode());
        final java.lang.Object $historicalPatternToWatch = this.getHistoricalPatternToWatch();
        result = result * PRIME + ($historicalPatternToWatch == null ? 43 : $historicalPatternToWatch.hashCode());
        final java.lang.Object $dataQuality = this.getDataQuality();
        result = result * PRIME + ($dataQuality == null ? 43 : $dataQuality.hashCode());
        final java.lang.Object $dataQualityReason = this.getDataQualityReason();
        result = result * PRIME + ($dataQualityReason == null ? 43 : $dataQualityReason.hashCode());
        final java.lang.Object $nextSessionRule = this.getNextSessionRule();
        result = result * PRIME + ($nextSessionRule == null ? 43 : $nextSessionRule.hashCode());
        final java.lang.Object $createdAt = this.getCreatedAt();
        result = result * PRIME + ($createdAt == null ? 43 : $createdAt.hashCode());
        final java.lang.Object $updatedAt = this.getUpdatedAt();
        result = result * PRIME + ($updatedAt == null ? 43 : $updatedAt.hashCode());
        return result;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public java.lang.String toString() {
        return "CoachSnapshotDto(id=" + this.getId() + ", accountId=" + this.getAccountId() + ", snapshotType=" + this.getSnapshotType() + ", status=" + this.getStatus() + ", periodStart=" + this.getPeriodStart() + ", periodEnd=" + this.getPeriodEnd() + ", totalTrades=" + this.getTotalTrades() + ", netPnl=" + this.getNetPnl() + ", winRate=" + this.getWinRate() + ", profitFactor=" + this.getProfitFactor() + ", bestSetup=" + this.getBestSetup() + ", worstSetup=" + this.getWorstSetup() + ", bestSession=" + this.getBestSession() + ", worstSession=" + this.getWorstSession() + ", biggestMistake=" + this.getBiggestMistake() + ", deterministicSummaryJson=" + this.getDeterministicSummaryJson() + ", aiResponseJson=" + this.getAiResponseJson() + ", errorMessage=" + this.getErrorMessage() + ", topLeakToWatch=" + this.getTopLeakToWatch() + ", selectedDaySummary=" + this.getSelectedDaySummary() + ", primaryIssueToday=" + this.getPrimaryIssueToday() + ", setupDirectionReview=" + this.getSetupDirectionReview() + ", longTradeCount=" + this.getLongTradeCount() + ", shortTradeCount=" + this.getShortTradeCount() + ", unknownTradeCount=" + this.getUnknownTradeCount() + ", longPnl=" + this.getLongPnl() + ", shortPnl=" + this.getShortPnl() + ", worstTradeSide=" + this.getWorstTradeSide() + ", historicalPatternToWatch=" + this.getHistoricalPatternToWatch() + ", historicalPatternAppliesToday=" + this.getHistoricalPatternAppliesToday() + ", dataQuality=" + this.getDataQuality() + ", dataQualityReason=" + this.getDataQualityReason() + ", nextSessionRule=" + this.getNextSessionRule() + ", aiGenerated=" + this.getAiGenerated() + ", noTradeDay=" + this.getNoTradeDay() + ", noTradeWeek=" + this.getNoTradeWeek() + ", creditsConsumed=" + this.getCreditsConsumed() + ", createdAt=" + this.getCreatedAt() + ", updatedAt=" + this.getUpdatedAt() + ")";
    }
}
