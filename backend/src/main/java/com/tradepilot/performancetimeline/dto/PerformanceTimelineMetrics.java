package com.tradepilot.performancetimeline.dto;

import java.math.BigDecimal;
import java.util.Map;

public class PerformanceTimelineMetrics {
    private int totalTrades;
    private BigDecimal netPnl;
    private BigDecimal totalR;
    private BigDecimal averageR;
    private BigDecimal winRate;
    private BigDecimal profitFactor;
    private BigDecimal averageWinnerR;
    private BigDecimal averageLoserR;
    private BigDecimal largestWinR;
    private BigDecimal largestLossR;
    private int winningDays;
    private int losingDays;
    private BigDecimal profitableTradingDayPercent;
    private int longTradeCount;
    private int shortTradeCount;
    private int unknownDirectionCount;
    private Map<String, Integer> setupDistribution;
    private String bestPerformingSetup;
    private String worstPerformingSetup;
    private Map<String, Integer> confidenceDistribution;
    private BigDecimal averageHoldMinutes;
    private int scratchTradeCount;
    private int overtradingDays;
    private int tradesAfterConsecutiveLosses;
    private int fomoTradeCount;
    private int revengeTradeCount;
    private BigDecimal averageTradesPerActiveDay;
    private BigDecimal averageRiskPerTrade;
    private BigDecimal riskConsistency;
    private int oversizedTradeCount;
    private BigDecimal largestDailyLossR;
    private BigDecimal averageDailyLossR;
    private BigDecimal riskRewardRatio;
    private BigDecimal configuredRiskUsagePercent;
    private Map<String, Integer> emotionDistribution;
    private String mostCommonNegativeEmotion;
    private String mostCommonPositiveEmotion;
    private int emotionalTradeCount;
    private int calmTradeCount;
    private BigDecimal notesCompletionPercent;
    private BigDecimal metadataCompletionPercent;
    private int greenWeekCount;
    private int redWeekCount;
    private BigDecimal weeklyRVolatility;
    private BigDecimal dailyRVolatility;
    private int longestProfitableDayStreak;
    private int longestLosingDayStreak;
    private BigDecimal highConfidenceTradePercent;

    public PerformanceTimelineMetrics() {
    }

    public int getTotalTrades() { return totalTrades; }
    public void setTotalTrades(int totalTrades) { this.totalTrades = totalTrades; }
    public BigDecimal getNetPnl() { return netPnl; }
    public void setNetPnl(BigDecimal netPnl) { this.netPnl = netPnl; }
    public BigDecimal getTotalR() { return totalR; }
    public void setTotalR(BigDecimal totalR) { this.totalR = totalR; }
    public BigDecimal getAverageR() { return averageR; }
    public void setAverageR(BigDecimal averageR) { this.averageR = averageR; }
    public BigDecimal getWinRate() { return winRate; }
    public void setWinRate(BigDecimal winRate) { this.winRate = winRate; }
    public BigDecimal getProfitFactor() { return profitFactor; }
    public void setProfitFactor(BigDecimal profitFactor) { this.profitFactor = profitFactor; }
    public BigDecimal getAverageWinnerR() { return averageWinnerR; }
    public void setAverageWinnerR(BigDecimal averageWinnerR) { this.averageWinnerR = averageWinnerR; }
    public BigDecimal getAverageLoserR() { return averageLoserR; }
    public void setAverageLoserR(BigDecimal averageLoserR) { this.averageLoserR = averageLoserR; }
    public BigDecimal getLargestWinR() { return largestWinR; }
    public void setLargestWinR(BigDecimal largestWinR) { this.largestWinR = largestWinR; }
    public BigDecimal getLargestLossR() { return largestLossR; }
    public void setLargestLossR(BigDecimal largestLossR) { this.largestLossR = largestLossR; }
    public int getWinningDays() { return winningDays; }
    public void setWinningDays(int winningDays) { this.winningDays = winningDays; }
    public int getLosingDays() { return losingDays; }
    public void setLosingDays(int losingDays) { this.losingDays = losingDays; }
    public BigDecimal getProfitableTradingDayPercent() { return profitableTradingDayPercent; }
    public void setProfitableTradingDayPercent(BigDecimal profitableTradingDayPercent) { this.profitableTradingDayPercent = profitableTradingDayPercent; }
    public int getLongTradeCount() { return longTradeCount; }
    public void setLongTradeCount(int longTradeCount) { this.longTradeCount = longTradeCount; }
    public int getShortTradeCount() { return shortTradeCount; }
    public void setShortTradeCount(int shortTradeCount) { this.shortTradeCount = shortTradeCount; }
    public int getUnknownDirectionCount() { return unknownDirectionCount; }
    public void setUnknownDirectionCount(int unknownDirectionCount) { this.unknownDirectionCount = unknownDirectionCount; }
    public Map<String, Integer> getSetupDistribution() { return setupDistribution; }
    public void setSetupDistribution(Map<String, Integer> setupDistribution) { this.setupDistribution = setupDistribution; }
    public String getBestPerformingSetup() { return bestPerformingSetup; }
    public void setBestPerformingSetup(String bestPerformingSetup) { this.bestPerformingSetup = bestPerformingSetup; }
    public String getWorstPerformingSetup() { return worstPerformingSetup; }
    public void setWorstPerformingSetup(String worstPerformingSetup) { this.worstPerformingSetup = worstPerformingSetup; }
    public Map<String, Integer> getConfidenceDistribution() { return confidenceDistribution; }
    public void setConfidenceDistribution(Map<String, Integer> confidenceDistribution) { this.confidenceDistribution = confidenceDistribution; }
    public BigDecimal getAverageHoldMinutes() { return averageHoldMinutes; }
    public void setAverageHoldMinutes(BigDecimal averageHoldMinutes) { this.averageHoldMinutes = averageHoldMinutes; }
    public int getScratchTradeCount() { return scratchTradeCount; }
    public void setScratchTradeCount(int scratchTradeCount) { this.scratchTradeCount = scratchTradeCount; }
    public int getOvertradingDays() { return overtradingDays; }
    public void setOvertradingDays(int overtradingDays) { this.overtradingDays = overtradingDays; }
    public int getTradesAfterConsecutiveLosses() { return tradesAfterConsecutiveLosses; }
    public void setTradesAfterConsecutiveLosses(int tradesAfterConsecutiveLosses) { this.tradesAfterConsecutiveLosses = tradesAfterConsecutiveLosses; }
    public int getFomoTradeCount() { return fomoTradeCount; }
    public void setFomoTradeCount(int fomoTradeCount) { this.fomoTradeCount = fomoTradeCount; }
    public int getRevengeTradeCount() { return revengeTradeCount; }
    public BigDecimal getAverageTradesPerActiveDay() { return averageTradesPerActiveDay; }
    public void setAverageTradesPerActiveDay(BigDecimal averageTradesPerActiveDay) { this.averageTradesPerActiveDay = averageTradesPerActiveDay; }
    public void setRevengeTradeCount(int revengeTradeCount) { this.revengeTradeCount = revengeTradeCount; }
    public BigDecimal getAverageRiskPerTrade() { return averageRiskPerTrade; }
    public void setAverageRiskPerTrade(BigDecimal averageRiskPerTrade) { this.averageRiskPerTrade = averageRiskPerTrade; }
    public BigDecimal getRiskConsistency() { return riskConsistency; }
    public void setRiskConsistency(BigDecimal riskConsistency) { this.riskConsistency = riskConsistency; }
    public int getOversizedTradeCount() { return oversizedTradeCount; }
    public void setOversizedTradeCount(int oversizedTradeCount) { this.oversizedTradeCount = oversizedTradeCount; }
    public BigDecimal getLargestDailyLossR() { return largestDailyLossR; }
    public void setLargestDailyLossR(BigDecimal largestDailyLossR) { this.largestDailyLossR = largestDailyLossR; }
    public BigDecimal getAverageDailyLossR() { return averageDailyLossR; }
    public void setAverageDailyLossR(BigDecimal averageDailyLossR) { this.averageDailyLossR = averageDailyLossR; }
    public BigDecimal getRiskRewardRatio() { return riskRewardRatio; }
    public void setRiskRewardRatio(BigDecimal riskRewardRatio) { this.riskRewardRatio = riskRewardRatio; }
    public BigDecimal getConfiguredRiskUsagePercent() { return configuredRiskUsagePercent; }
    public void setConfiguredRiskUsagePercent(BigDecimal configuredRiskUsagePercent) { this.configuredRiskUsagePercent = configuredRiskUsagePercent; }
    public Map<String, Integer> getEmotionDistribution() { return emotionDistribution; }
    public void setEmotionDistribution(Map<String, Integer> emotionDistribution) { this.emotionDistribution = emotionDistribution; }
    public String getMostCommonNegativeEmotion() { return mostCommonNegativeEmotion; }
    public void setMostCommonNegativeEmotion(String mostCommonNegativeEmotion) { this.mostCommonNegativeEmotion = mostCommonNegativeEmotion; }
    public String getMostCommonPositiveEmotion() { return mostCommonPositiveEmotion; }
    public void setMostCommonPositiveEmotion(String mostCommonPositiveEmotion) { this.mostCommonPositiveEmotion = mostCommonPositiveEmotion; }
    public int getEmotionalTradeCount() { return emotionalTradeCount; }
    public void setEmotionalTradeCount(int emotionalTradeCount) { this.emotionalTradeCount = emotionalTradeCount; }
    public int getCalmTradeCount() { return calmTradeCount; }
    public void setCalmTradeCount(int calmTradeCount) { this.calmTradeCount = calmTradeCount; }
    public BigDecimal getNotesCompletionPercent() { return notesCompletionPercent; }
    public void setNotesCompletionPercent(BigDecimal notesCompletionPercent) { this.notesCompletionPercent = notesCompletionPercent; }
    public BigDecimal getMetadataCompletionPercent() { return metadataCompletionPercent; }
    public void setMetadataCompletionPercent(BigDecimal metadataCompletionPercent) { this.metadataCompletionPercent = metadataCompletionPercent; }
    public int getGreenWeekCount() { return greenWeekCount; }
    public void setGreenWeekCount(int greenWeekCount) { this.greenWeekCount = greenWeekCount; }
    public int getRedWeekCount() { return redWeekCount; }
    public void setRedWeekCount(int redWeekCount) { this.redWeekCount = redWeekCount; }
    public BigDecimal getWeeklyRVolatility() { return weeklyRVolatility; }
    public void setWeeklyRVolatility(BigDecimal weeklyRVolatility) { this.weeklyRVolatility = weeklyRVolatility; }
    public BigDecimal getDailyRVolatility() { return dailyRVolatility; }
    public void setDailyRVolatility(BigDecimal dailyRVolatility) { this.dailyRVolatility = dailyRVolatility; }
    public int getLongestProfitableDayStreak() { return longestProfitableDayStreak; }
    public void setLongestProfitableDayStreak(int longestProfitableDayStreak) { this.longestProfitableDayStreak = longestProfitableDayStreak; }
    public int getLongestLosingDayStreak() { return longestLosingDayStreak; }
    public void setLongestLosingDayStreak(int longestLosingDayStreak) { this.longestLosingDayStreak = longestLosingDayStreak; }
    public BigDecimal getHighConfidenceTradePercent() { return highConfidenceTradePercent; }
    public void setHighConfidenceTradePercent(BigDecimal highConfidenceTradePercent) { this.highConfidenceTradePercent = highConfidenceTradePercent; }
}
