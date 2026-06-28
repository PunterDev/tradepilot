package com.tradepilot.tradingdna.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public class TradingDnaContext {
    private Long userId;
    private Long accountId;
    private boolean includeAllAccounts;
    private int days;
    private LocalDate startDate;
    private LocalDate endDate;
    private int totalTrades;
    private BigDecimal netPnl;
    private BigDecimal winRate;
    private String bestSetup;
    private String worstSetup;
    private String bestSession;
    private String worstSession;
    private Map<String, Long> mistakeCounts;
    private Map<String, BigDecimal> setupPnl;
    private Map<String, BigDecimal> emotionPnl;
    private Map<String, BigDecimal> confidencePnl;
    private Map<String, Long> violationCounts;
    private List<String> journalMetadataExamples;
    private List<String> behavioralInsights;
    private List<String> tradeReviewSummaries;
    private List<String> aiTradeStrengths;
    private List<String> aiTradeMistakes;
    private List<String> coachNotes;
    private List<String> deterministicSignals;
    private List<TradingDnaTimelinePoint> timeline;
    private Integer deterministicDisciplineScore;
    private Integer deterministicExecutionScore;
    private Integer deterministicPatienceScore;
    private Integer deterministicConsistencyScore;

    public static TradingDnaContext of(Long userId, Long accountId, boolean includeAllAccounts, int days, LocalDate startDate, LocalDate endDate, int totalTrades, BigDecimal netPnl, BigDecimal winRate, String bestSetup, String worstSetup, String bestSession, String worstSession, Map<String, Long> mistakeCounts, Map<String, BigDecimal> setupPnl, Map<String, BigDecimal> emotionPnl, Map<String, BigDecimal> confidencePnl, Map<String, Long> violationCounts, List<String> journalMetadataExamples, List<String> behavioralInsights, List<String> tradeReviewSummaries, List<String> aiTradeStrengths, List<String> aiTradeMistakes, List<String> coachNotes, List<String> deterministicSignals, List<TradingDnaTimelinePoint> timeline, Integer deterministicDisciplineScore, Integer deterministicExecutionScore, Integer deterministicPatienceScore, Integer deterministicConsistencyScore) {
        TradingDnaContext context = new TradingDnaContext();
        context.setUserId(userId);
        context.setAccountId(accountId);
        context.setIncludeAllAccounts(includeAllAccounts);
        context.setDays(days);
        context.setStartDate(startDate);
        context.setEndDate(endDate);
        context.setTotalTrades(totalTrades);
        context.setNetPnl(netPnl);
        context.setWinRate(winRate);
        context.setBestSetup(bestSetup);
        context.setWorstSetup(worstSetup);
        context.setBestSession(bestSession);
        context.setWorstSession(worstSession);
        context.setMistakeCounts(mistakeCounts);
        context.setSetupPnl(setupPnl);
        context.setEmotionPnl(emotionPnl);
        context.setConfidencePnl(confidencePnl);
        context.setViolationCounts(violationCounts);
        context.setJournalMetadataExamples(journalMetadataExamples);
        context.setBehavioralInsights(behavioralInsights);
        context.setTradeReviewSummaries(tradeReviewSummaries);
        context.setAiTradeStrengths(aiTradeStrengths);
        context.setAiTradeMistakes(aiTradeMistakes);
        context.setCoachNotes(coachNotes);
        context.setDeterministicSignals(deterministicSignals);
        context.setTimeline(timeline);
        context.setDeterministicDisciplineScore(deterministicDisciplineScore);
        context.setDeterministicExecutionScore(deterministicExecutionScore);
        context.setDeterministicPatienceScore(deterministicPatienceScore);
        context.setDeterministicConsistencyScore(deterministicConsistencyScore);
        return context;
    }

    @java.lang.SuppressWarnings("all")
    public TradingDnaContext() {
    }

    @java.lang.SuppressWarnings("all")
    public Long getUserId() {
        return this.userId;
    }

    @java.lang.SuppressWarnings("all")
    public Long getAccountId() {
        return this.accountId;
    }

    @java.lang.SuppressWarnings("all")
    public boolean isIncludeAllAccounts() {
        return this.includeAllAccounts;
    }

    @java.lang.SuppressWarnings("all")
    public int getDays() {
        return this.days;
    }

    @java.lang.SuppressWarnings("all")
    public LocalDate getStartDate() {
        return this.startDate;
    }

    @java.lang.SuppressWarnings("all")
    public LocalDate getEndDate() {
        return this.endDate;
    }

    @java.lang.SuppressWarnings("all")
    public int getTotalTrades() {
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
    public Map<String, Long> getMistakeCounts() {
        return this.mistakeCounts;
    }

    @java.lang.SuppressWarnings("all")
    public Map<String, BigDecimal> getSetupPnl() {
        return this.setupPnl;
    }

    @java.lang.SuppressWarnings("all")
    public Map<String, BigDecimal> getEmotionPnl() {
        return this.emotionPnl;
    }

    @java.lang.SuppressWarnings("all")
    public Map<String, BigDecimal> getConfidencePnl() {
        return this.confidencePnl;
    }

    @java.lang.SuppressWarnings("all")
    public Map<String, Long> getViolationCounts() {
        return this.violationCounts;
    }

    @java.lang.SuppressWarnings("all")
    public List<String> getJournalMetadataExamples() {
        return this.journalMetadataExamples;
    }

    @java.lang.SuppressWarnings("all")
    public List<String> getBehavioralInsights() {
        return this.behavioralInsights;
    }

    @java.lang.SuppressWarnings("all")
    public List<String> getTradeReviewSummaries() {
        return this.tradeReviewSummaries;
    }

    @java.lang.SuppressWarnings("all")
    public List<String> getAiTradeStrengths() {
        return this.aiTradeStrengths;
    }

    @java.lang.SuppressWarnings("all")
    public List<String> getAiTradeMistakes() {
        return this.aiTradeMistakes;
    }

    @java.lang.SuppressWarnings("all")
    public List<String> getCoachNotes() {
        return this.coachNotes;
    }

    @java.lang.SuppressWarnings("all")
    public List<String> getDeterministicSignals() {
        return this.deterministicSignals;
    }

    @java.lang.SuppressWarnings("all")
    public List<TradingDnaTimelinePoint> getTimeline() {
        return this.timeline;
    }

    @java.lang.SuppressWarnings("all")
    public Integer getDeterministicDisciplineScore() {
        return this.deterministicDisciplineScore;
    }

    @java.lang.SuppressWarnings("all")
    public Integer getDeterministicExecutionScore() {
        return this.deterministicExecutionScore;
    }

    @java.lang.SuppressWarnings("all")
    public Integer getDeterministicPatienceScore() {
        return this.deterministicPatienceScore;
    }

    @java.lang.SuppressWarnings("all")
    public Integer getDeterministicConsistencyScore() {
        return this.deterministicConsistencyScore;
    }

    @java.lang.SuppressWarnings("all")
    public void setUserId(final Long userId) {
        this.userId = userId;
    }

    @java.lang.SuppressWarnings("all")
    public void setAccountId(final Long accountId) {
        this.accountId = accountId;
    }

    @java.lang.SuppressWarnings("all")
    public void setIncludeAllAccounts(final boolean includeAllAccounts) {
        this.includeAllAccounts = includeAllAccounts;
    }

    @java.lang.SuppressWarnings("all")
    public void setDays(final int days) {
        this.days = days;
    }

    @java.lang.SuppressWarnings("all")
    public void setStartDate(final LocalDate startDate) {
        this.startDate = startDate;
    }

    @java.lang.SuppressWarnings("all")
    public void setEndDate(final LocalDate endDate) {
        this.endDate = endDate;
    }

    @java.lang.SuppressWarnings("all")
    public void setTotalTrades(final int totalTrades) {
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
    public void setMistakeCounts(final Map<String, Long> mistakeCounts) {
        this.mistakeCounts = mistakeCounts;
    }

    @java.lang.SuppressWarnings("all")
    public void setSetupPnl(final Map<String, BigDecimal> setupPnl) {
        this.setupPnl = setupPnl;
    }

    @java.lang.SuppressWarnings("all")
    public void setEmotionPnl(final Map<String, BigDecimal> emotionPnl) {
        this.emotionPnl = emotionPnl;
    }

    @java.lang.SuppressWarnings("all")
    public void setConfidencePnl(final Map<String, BigDecimal> confidencePnl) {
        this.confidencePnl = confidencePnl;
    }

    @java.lang.SuppressWarnings("all")
    public void setViolationCounts(final Map<String, Long> violationCounts) {
        this.violationCounts = violationCounts;
    }

    @java.lang.SuppressWarnings("all")
    public void setJournalMetadataExamples(final List<String> journalMetadataExamples) {
        this.journalMetadataExamples = journalMetadataExamples;
    }

    @java.lang.SuppressWarnings("all")
    public void setBehavioralInsights(final List<String> behavioralInsights) {
        this.behavioralInsights = behavioralInsights;
    }

    @java.lang.SuppressWarnings("all")
    public void setTradeReviewSummaries(final List<String> tradeReviewSummaries) {
        this.tradeReviewSummaries = tradeReviewSummaries;
    }

    @java.lang.SuppressWarnings("all")
    public void setAiTradeStrengths(final List<String> aiTradeStrengths) {
        this.aiTradeStrengths = aiTradeStrengths;
    }

    @java.lang.SuppressWarnings("all")
    public void setAiTradeMistakes(final List<String> aiTradeMistakes) {
        this.aiTradeMistakes = aiTradeMistakes;
    }

    @java.lang.SuppressWarnings("all")
    public void setCoachNotes(final List<String> coachNotes) {
        this.coachNotes = coachNotes;
    }

    @java.lang.SuppressWarnings("all")
    public void setDeterministicSignals(final List<String> deterministicSignals) {
        this.deterministicSignals = deterministicSignals;
    }

    @java.lang.SuppressWarnings("all")
    public void setTimeline(final List<TradingDnaTimelinePoint> timeline) {
        this.timeline = timeline;
    }

    @java.lang.SuppressWarnings("all")
    public void setDeterministicDisciplineScore(final Integer deterministicDisciplineScore) {
        this.deterministicDisciplineScore = deterministicDisciplineScore;
    }

    @java.lang.SuppressWarnings("all")
    public void setDeterministicExecutionScore(final Integer deterministicExecutionScore) {
        this.deterministicExecutionScore = deterministicExecutionScore;
    }

    @java.lang.SuppressWarnings("all")
    public void setDeterministicPatienceScore(final Integer deterministicPatienceScore) {
        this.deterministicPatienceScore = deterministicPatienceScore;
    }

    @java.lang.SuppressWarnings("all")
    public void setDeterministicConsistencyScore(final Integer deterministicConsistencyScore) {
        this.deterministicConsistencyScore = deterministicConsistencyScore;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof TradingDnaContext)) return false;
        final TradingDnaContext other = (TradingDnaContext) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        if (this.isIncludeAllAccounts() != other.isIncludeAllAccounts()) return false;
        if (this.getDays() != other.getDays()) return false;
        if (this.getTotalTrades() != other.getTotalTrades()) return false;
        final java.lang.Object this$userId = this.getUserId();
        final java.lang.Object other$userId = other.getUserId();
        if (this$userId == null ? other$userId != null : !this$userId.equals(other$userId)) return false;
        final java.lang.Object this$accountId = this.getAccountId();
        final java.lang.Object other$accountId = other.getAccountId();
        if (this$accountId == null ? other$accountId != null : !this$accountId.equals(other$accountId)) return false;
        final java.lang.Object this$deterministicDisciplineScore = this.getDeterministicDisciplineScore();
        final java.lang.Object other$deterministicDisciplineScore = other.getDeterministicDisciplineScore();
        if (this$deterministicDisciplineScore == null ? other$deterministicDisciplineScore != null : !this$deterministicDisciplineScore.equals(other$deterministicDisciplineScore)) return false;
        final java.lang.Object this$deterministicExecutionScore = this.getDeterministicExecutionScore();
        final java.lang.Object other$deterministicExecutionScore = other.getDeterministicExecutionScore();
        if (this$deterministicExecutionScore == null ? other$deterministicExecutionScore != null : !this$deterministicExecutionScore.equals(other$deterministicExecutionScore)) return false;
        final java.lang.Object this$deterministicPatienceScore = this.getDeterministicPatienceScore();
        final java.lang.Object other$deterministicPatienceScore = other.getDeterministicPatienceScore();
        if (this$deterministicPatienceScore == null ? other$deterministicPatienceScore != null : !this$deterministicPatienceScore.equals(other$deterministicPatienceScore)) return false;
        final java.lang.Object this$deterministicConsistencyScore = this.getDeterministicConsistencyScore();
        final java.lang.Object other$deterministicConsistencyScore = other.getDeterministicConsistencyScore();
        if (this$deterministicConsistencyScore == null ? other$deterministicConsistencyScore != null : !this$deterministicConsistencyScore.equals(other$deterministicConsistencyScore)) return false;
        final java.lang.Object this$startDate = this.getStartDate();
        final java.lang.Object other$startDate = other.getStartDate();
        if (this$startDate == null ? other$startDate != null : !this$startDate.equals(other$startDate)) return false;
        final java.lang.Object this$endDate = this.getEndDate();
        final java.lang.Object other$endDate = other.getEndDate();
        if (this$endDate == null ? other$endDate != null : !this$endDate.equals(other$endDate)) return false;
        final java.lang.Object this$netPnl = this.getNetPnl();
        final java.lang.Object other$netPnl = other.getNetPnl();
        if (this$netPnl == null ? other$netPnl != null : !this$netPnl.equals(other$netPnl)) return false;
        final java.lang.Object this$winRate = this.getWinRate();
        final java.lang.Object other$winRate = other.getWinRate();
        if (this$winRate == null ? other$winRate != null : !this$winRate.equals(other$winRate)) return false;
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
        final java.lang.Object this$mistakeCounts = this.getMistakeCounts();
        final java.lang.Object other$mistakeCounts = other.getMistakeCounts();
        if (this$mistakeCounts == null ? other$mistakeCounts != null : !this$mistakeCounts.equals(other$mistakeCounts)) return false;
        final java.lang.Object this$setupPnl = this.getSetupPnl();
        final java.lang.Object other$setupPnl = other.getSetupPnl();
        if (this$setupPnl == null ? other$setupPnl != null : !this$setupPnl.equals(other$setupPnl)) return false;
        final java.lang.Object this$emotionPnl = this.getEmotionPnl();
        final java.lang.Object other$emotionPnl = other.getEmotionPnl();
        if (this$emotionPnl == null ? other$emotionPnl != null : !this$emotionPnl.equals(other$emotionPnl)) return false;
        final java.lang.Object this$confidencePnl = this.getConfidencePnl();
        final java.lang.Object other$confidencePnl = other.getConfidencePnl();
        if (this$confidencePnl == null ? other$confidencePnl != null : !this$confidencePnl.equals(other$confidencePnl)) return false;
        final java.lang.Object this$violationCounts = this.getViolationCounts();
        final java.lang.Object other$violationCounts = other.getViolationCounts();
        if (this$violationCounts == null ? other$violationCounts != null : !this$violationCounts.equals(other$violationCounts)) return false;
        final java.lang.Object this$journalMetadataExamples = this.getJournalMetadataExamples();
        final java.lang.Object other$journalMetadataExamples = other.getJournalMetadataExamples();
        if (this$journalMetadataExamples == null ? other$journalMetadataExamples != null : !this$journalMetadataExamples.equals(other$journalMetadataExamples)) return false;
        final java.lang.Object this$behavioralInsights = this.getBehavioralInsights();
        final java.lang.Object other$behavioralInsights = other.getBehavioralInsights();
        if (this$behavioralInsights == null ? other$behavioralInsights != null : !this$behavioralInsights.equals(other$behavioralInsights)) return false;
        final java.lang.Object this$tradeReviewSummaries = this.getTradeReviewSummaries();
        final java.lang.Object other$tradeReviewSummaries = other.getTradeReviewSummaries();
        if (this$tradeReviewSummaries == null ? other$tradeReviewSummaries != null : !this$tradeReviewSummaries.equals(other$tradeReviewSummaries)) return false;
        final java.lang.Object this$aiTradeStrengths = this.getAiTradeStrengths();
        final java.lang.Object other$aiTradeStrengths = other.getAiTradeStrengths();
        if (this$aiTradeStrengths == null ? other$aiTradeStrengths != null : !this$aiTradeStrengths.equals(other$aiTradeStrengths)) return false;
        final java.lang.Object this$aiTradeMistakes = this.getAiTradeMistakes();
        final java.lang.Object other$aiTradeMistakes = other.getAiTradeMistakes();
        if (this$aiTradeMistakes == null ? other$aiTradeMistakes != null : !this$aiTradeMistakes.equals(other$aiTradeMistakes)) return false;
        final java.lang.Object this$coachNotes = this.getCoachNotes();
        final java.lang.Object other$coachNotes = other.getCoachNotes();
        if (this$coachNotes == null ? other$coachNotes != null : !this$coachNotes.equals(other$coachNotes)) return false;
        final java.lang.Object this$deterministicSignals = this.getDeterministicSignals();
        final java.lang.Object other$deterministicSignals = other.getDeterministicSignals();
        if (this$deterministicSignals == null ? other$deterministicSignals != null : !this$deterministicSignals.equals(other$deterministicSignals)) return false;
        final java.lang.Object this$timeline = this.getTimeline();
        final java.lang.Object other$timeline = other.getTimeline();
        if (this$timeline == null ? other$timeline != null : !this$timeline.equals(other$timeline)) return false;
        return true;
    }

    @java.lang.SuppressWarnings("all")
    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof TradingDnaContext;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        result = result * PRIME + (this.isIncludeAllAccounts() ? 79 : 97);
        result = result * PRIME + this.getDays();
        result = result * PRIME + this.getTotalTrades();
        final java.lang.Object $userId = this.getUserId();
        result = result * PRIME + ($userId == null ? 43 : $userId.hashCode());
        final java.lang.Object $accountId = this.getAccountId();
        result = result * PRIME + ($accountId == null ? 43 : $accountId.hashCode());
        final java.lang.Object $deterministicDisciplineScore = this.getDeterministicDisciplineScore();
        result = result * PRIME + ($deterministicDisciplineScore == null ? 43 : $deterministicDisciplineScore.hashCode());
        final java.lang.Object $deterministicExecutionScore = this.getDeterministicExecutionScore();
        result = result * PRIME + ($deterministicExecutionScore == null ? 43 : $deterministicExecutionScore.hashCode());
        final java.lang.Object $deterministicPatienceScore = this.getDeterministicPatienceScore();
        result = result * PRIME + ($deterministicPatienceScore == null ? 43 : $deterministicPatienceScore.hashCode());
        final java.lang.Object $deterministicConsistencyScore = this.getDeterministicConsistencyScore();
        result = result * PRIME + ($deterministicConsistencyScore == null ? 43 : $deterministicConsistencyScore.hashCode());
        final java.lang.Object $startDate = this.getStartDate();
        result = result * PRIME + ($startDate == null ? 43 : $startDate.hashCode());
        final java.lang.Object $endDate = this.getEndDate();
        result = result * PRIME + ($endDate == null ? 43 : $endDate.hashCode());
        final java.lang.Object $netPnl = this.getNetPnl();
        result = result * PRIME + ($netPnl == null ? 43 : $netPnl.hashCode());
        final java.lang.Object $winRate = this.getWinRate();
        result = result * PRIME + ($winRate == null ? 43 : $winRate.hashCode());
        final java.lang.Object $bestSetup = this.getBestSetup();
        result = result * PRIME + ($bestSetup == null ? 43 : $bestSetup.hashCode());
        final java.lang.Object $worstSetup = this.getWorstSetup();
        result = result * PRIME + ($worstSetup == null ? 43 : $worstSetup.hashCode());
        final java.lang.Object $bestSession = this.getBestSession();
        result = result * PRIME + ($bestSession == null ? 43 : $bestSession.hashCode());
        final java.lang.Object $worstSession = this.getWorstSession();
        result = result * PRIME + ($worstSession == null ? 43 : $worstSession.hashCode());
        final java.lang.Object $mistakeCounts = this.getMistakeCounts();
        result = result * PRIME + ($mistakeCounts == null ? 43 : $mistakeCounts.hashCode());
        final java.lang.Object $setupPnl = this.getSetupPnl();
        result = result * PRIME + ($setupPnl == null ? 43 : $setupPnl.hashCode());
        final java.lang.Object $emotionPnl = this.getEmotionPnl();
        result = result * PRIME + ($emotionPnl == null ? 43 : $emotionPnl.hashCode());
        final java.lang.Object $confidencePnl = this.getConfidencePnl();
        result = result * PRIME + ($confidencePnl == null ? 43 : $confidencePnl.hashCode());
        final java.lang.Object $violationCounts = this.getViolationCounts();
        result = result * PRIME + ($violationCounts == null ? 43 : $violationCounts.hashCode());
        final java.lang.Object $journalMetadataExamples = this.getJournalMetadataExamples();
        result = result * PRIME + ($journalMetadataExamples == null ? 43 : $journalMetadataExamples.hashCode());
        final java.lang.Object $behavioralInsights = this.getBehavioralInsights();
        result = result * PRIME + ($behavioralInsights == null ? 43 : $behavioralInsights.hashCode());
        final java.lang.Object $tradeReviewSummaries = this.getTradeReviewSummaries();
        result = result * PRIME + ($tradeReviewSummaries == null ? 43 : $tradeReviewSummaries.hashCode());
        final java.lang.Object $aiTradeStrengths = this.getAiTradeStrengths();
        result = result * PRIME + ($aiTradeStrengths == null ? 43 : $aiTradeStrengths.hashCode());
        final java.lang.Object $aiTradeMistakes = this.getAiTradeMistakes();
        result = result * PRIME + ($aiTradeMistakes == null ? 43 : $aiTradeMistakes.hashCode());
        final java.lang.Object $coachNotes = this.getCoachNotes();
        result = result * PRIME + ($coachNotes == null ? 43 : $coachNotes.hashCode());
        final java.lang.Object $deterministicSignals = this.getDeterministicSignals();
        result = result * PRIME + ($deterministicSignals == null ? 43 : $deterministicSignals.hashCode());
        final java.lang.Object $timeline = this.getTimeline();
        result = result * PRIME + ($timeline == null ? 43 : $timeline.hashCode());
        return result;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public java.lang.String toString() {
        return "TradingDnaContext(userId=" + this.getUserId() + ", accountId=" + this.getAccountId() + ", includeAllAccounts=" + this.isIncludeAllAccounts() + ", days=" + this.getDays() + ", startDate=" + this.getStartDate() + ", endDate=" + this.getEndDate() + ", totalTrades=" + this.getTotalTrades() + ", netPnl=" + this.getNetPnl() + ", winRate=" + this.getWinRate() + ", bestSetup=" + this.getBestSetup() + ", worstSetup=" + this.getWorstSetup() + ", bestSession=" + this.getBestSession() + ", worstSession=" + this.getWorstSession() + ", mistakeCounts=" + this.getMistakeCounts() + ", setupPnl=" + this.getSetupPnl() + ", emotionPnl=" + this.getEmotionPnl() + ", confidencePnl=" + this.getConfidencePnl() + ", violationCounts=" + this.getViolationCounts() + ", journalMetadataExamples=" + this.getJournalMetadataExamples() + ", behavioralInsights=" + this.getBehavioralInsights() + ", tradeReviewSummaries=" + this.getTradeReviewSummaries() + ", aiTradeStrengths=" + this.getAiTradeStrengths() + ", aiTradeMistakes=" + this.getAiTradeMistakes() + ", coachNotes=" + this.getCoachNotes() + ", deterministicSignals=" + this.getDeterministicSignals() + ", timeline=" + this.getTimeline() + ", deterministicDisciplineScore=" + this.getDeterministicDisciplineScore() + ", deterministicExecutionScore=" + this.getDeterministicExecutionScore() + ", deterministicPatienceScore=" + this.getDeterministicPatienceScore() + ", deterministicConsistencyScore=" + this.getDeterministicConsistencyScore() + ")";
    }
}
