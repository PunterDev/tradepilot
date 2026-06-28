package com.tradepilot.dto.session;

import com.tradepilot.dto.account.TradingAccountResponse;
import com.tradepilot.dto.trade.TradeResponse;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public class SessionReviewResponse {
    private Long accountId;
    private LocalDate date;
    private LocalDate sessionDate;
    private TradingAccountResponse account;
    private List<TradeResponse> trades;
    private List<SessionTimelineTradeResponse> timelineTrades;
    private Integer totalTrades;
    private Integer winners;
    private Integer losers;
    private Integer breakevenTrades;
    private BigDecimal netPnl;
    private BigDecimal grossProfit;
    private BigDecimal grossLoss;
    private BigDecimal largestWinner;
    private BigDecimal largestLoser;
    private BigDecimal averageWinner;
    private BigDecimal averageLoser;
    private BigDecimal totalPnl;
    private BigDecimal winRate;
    private BigDecimal profitFactor;
    private String sessionGrade;
    private String sessionStory;
    private String bestTradeWhy;
    private String worstTradeWhy;
    private TradeResponse bestTrade;
    private TradeResponse worstTrade;
    private Map<String, Long> mistakeSummary;
    private List<SessionMistakeResponse> detectedMistakes;
    private SessionReplayInsightsResponse replayInsights;
    private Integer sessionDisciplineScore;
    private AiSessionReviewResponse aiSessionReview;
    private Boolean aiReviewCached;
    private Boolean aiGenerated;
    private Boolean noTrades;
    private Integer creditsConsumed;
    private LocalDate previousTradingDay;
    private LocalDate nextTradingDay;

    @java.lang.SuppressWarnings("all")
    public SessionReviewResponse() {
    }

    @java.lang.SuppressWarnings("all")
    public Long getAccountId() {
        return this.accountId;
    }

    @java.lang.SuppressWarnings("all")
    public LocalDate getDate() {
        return this.date;
    }

    @java.lang.SuppressWarnings("all")
    public LocalDate getSessionDate() {
        return this.sessionDate;
    }

    @java.lang.SuppressWarnings("all")
    public TradingAccountResponse getAccount() {
        return this.account;
    }

    @java.lang.SuppressWarnings("all")
    public List<TradeResponse> getTrades() {
        return this.trades;
    }

    @java.lang.SuppressWarnings("all")
    public List<SessionTimelineTradeResponse> getTimelineTrades() {
        return this.timelineTrades;
    }

    @java.lang.SuppressWarnings("all")
    public Integer getTotalTrades() {
        return this.totalTrades;
    }

    @java.lang.SuppressWarnings("all")
    public Integer getWinners() {
        return this.winners;
    }

    @java.lang.SuppressWarnings("all")
    public Integer getLosers() {
        return this.losers;
    }

    @java.lang.SuppressWarnings("all")
    public Integer getBreakevenTrades() {
        return this.breakevenTrades;
    }

    @java.lang.SuppressWarnings("all")
    public BigDecimal getNetPnl() {
        return this.netPnl;
    }

    @java.lang.SuppressWarnings("all")
    public BigDecimal getGrossProfit() {
        return this.grossProfit;
    }

    @java.lang.SuppressWarnings("all")
    public BigDecimal getGrossLoss() {
        return this.grossLoss;
    }

    @java.lang.SuppressWarnings("all")
    public BigDecimal getLargestWinner() {
        return this.largestWinner;
    }

    @java.lang.SuppressWarnings("all")
    public BigDecimal getLargestLoser() {
        return this.largestLoser;
    }

    @java.lang.SuppressWarnings("all")
    public BigDecimal getAverageWinner() {
        return this.averageWinner;
    }

    @java.lang.SuppressWarnings("all")
    public BigDecimal getAverageLoser() {
        return this.averageLoser;
    }

    @java.lang.SuppressWarnings("all")
    public BigDecimal getTotalPnl() {
        return this.totalPnl;
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
    public String getSessionGrade() {
        return this.sessionGrade;
    }

    @java.lang.SuppressWarnings("all")
    public String getSessionStory() {
        return this.sessionStory;
    }

    @java.lang.SuppressWarnings("all")
    public String getBestTradeWhy() {
        return this.bestTradeWhy;
    }

    @java.lang.SuppressWarnings("all")
    public String getWorstTradeWhy() {
        return this.worstTradeWhy;
    }

    @java.lang.SuppressWarnings("all")
    public TradeResponse getBestTrade() {
        return this.bestTrade;
    }

    @java.lang.SuppressWarnings("all")
    public TradeResponse getWorstTrade() {
        return this.worstTrade;
    }

    @java.lang.SuppressWarnings("all")
    public Map<String, Long> getMistakeSummary() {
        return this.mistakeSummary;
    }

    @java.lang.SuppressWarnings("all")
    public List<SessionMistakeResponse> getDetectedMistakes() {
        return this.detectedMistakes;
    }

    @java.lang.SuppressWarnings("all")
    public SessionReplayInsightsResponse getReplayInsights() {
        return this.replayInsights;
    }

    @java.lang.SuppressWarnings("all")
    public Integer getSessionDisciplineScore() {
        return this.sessionDisciplineScore;
    }

    @java.lang.SuppressWarnings("all")
    public AiSessionReviewResponse getAiSessionReview() {
        return this.aiSessionReview;
    }

    @java.lang.SuppressWarnings("all")
    public Boolean getAiReviewCached() {
        return this.aiReviewCached;
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
    public Integer getCreditsConsumed() {
        return this.creditsConsumed;
    }

    @java.lang.SuppressWarnings("all")
    public LocalDate getPreviousTradingDay() {
        return this.previousTradingDay;
    }

    @java.lang.SuppressWarnings("all")
    public LocalDate getNextTradingDay() {
        return this.nextTradingDay;
    }

    @java.lang.SuppressWarnings("all")
    public void setAccountId(final Long accountId) {
        this.accountId = accountId;
    }

    @java.lang.SuppressWarnings("all")
    public void setDate(final LocalDate date) {
        this.date = date;
    }

    @java.lang.SuppressWarnings("all")
    public void setSessionDate(final LocalDate sessionDate) {
        this.sessionDate = sessionDate;
    }

    @java.lang.SuppressWarnings("all")
    public void setAccount(final TradingAccountResponse account) {
        this.account = account;
    }

    @java.lang.SuppressWarnings("all")
    public void setTrades(final List<TradeResponse> trades) {
        this.trades = trades;
    }

    @java.lang.SuppressWarnings("all")
    public void setTimelineTrades(final List<SessionTimelineTradeResponse> timelineTrades) {
        this.timelineTrades = timelineTrades;
    }

    @java.lang.SuppressWarnings("all")
    public void setTotalTrades(final Integer totalTrades) {
        this.totalTrades = totalTrades;
    }

    @java.lang.SuppressWarnings("all")
    public void setWinners(final Integer winners) {
        this.winners = winners;
    }

    @java.lang.SuppressWarnings("all")
    public void setLosers(final Integer losers) {
        this.losers = losers;
    }

    @java.lang.SuppressWarnings("all")
    public void setBreakevenTrades(final Integer breakevenTrades) {
        this.breakevenTrades = breakevenTrades;
    }

    @java.lang.SuppressWarnings("all")
    public void setNetPnl(final BigDecimal netPnl) {
        this.netPnl = netPnl;
    }

    @java.lang.SuppressWarnings("all")
    public void setGrossProfit(final BigDecimal grossProfit) {
        this.grossProfit = grossProfit;
    }

    @java.lang.SuppressWarnings("all")
    public void setGrossLoss(final BigDecimal grossLoss) {
        this.grossLoss = grossLoss;
    }

    @java.lang.SuppressWarnings("all")
    public void setLargestWinner(final BigDecimal largestWinner) {
        this.largestWinner = largestWinner;
    }

    @java.lang.SuppressWarnings("all")
    public void setLargestLoser(final BigDecimal largestLoser) {
        this.largestLoser = largestLoser;
    }

    @java.lang.SuppressWarnings("all")
    public void setAverageWinner(final BigDecimal averageWinner) {
        this.averageWinner = averageWinner;
    }

    @java.lang.SuppressWarnings("all")
    public void setAverageLoser(final BigDecimal averageLoser) {
        this.averageLoser = averageLoser;
    }

    @java.lang.SuppressWarnings("all")
    public void setTotalPnl(final BigDecimal totalPnl) {
        this.totalPnl = totalPnl;
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
    public void setSessionGrade(final String sessionGrade) {
        this.sessionGrade = sessionGrade;
    }

    @java.lang.SuppressWarnings("all")
    public void setSessionStory(final String sessionStory) {
        this.sessionStory = sessionStory;
    }

    @java.lang.SuppressWarnings("all")
    public void setBestTradeWhy(final String bestTradeWhy) {
        this.bestTradeWhy = bestTradeWhy;
    }

    @java.lang.SuppressWarnings("all")
    public void setWorstTradeWhy(final String worstTradeWhy) {
        this.worstTradeWhy = worstTradeWhy;
    }

    @java.lang.SuppressWarnings("all")
    public void setBestTrade(final TradeResponse bestTrade) {
        this.bestTrade = bestTrade;
    }

    @java.lang.SuppressWarnings("all")
    public void setWorstTrade(final TradeResponse worstTrade) {
        this.worstTrade = worstTrade;
    }

    @java.lang.SuppressWarnings("all")
    public void setMistakeSummary(final Map<String, Long> mistakeSummary) {
        this.mistakeSummary = mistakeSummary;
    }

    @java.lang.SuppressWarnings("all")
    public void setDetectedMistakes(final List<SessionMistakeResponse> detectedMistakes) {
        this.detectedMistakes = detectedMistakes;
    }

    @java.lang.SuppressWarnings("all")
    public void setReplayInsights(final SessionReplayInsightsResponse replayInsights) {
        this.replayInsights = replayInsights;
    }

    @java.lang.SuppressWarnings("all")
    public void setSessionDisciplineScore(final Integer sessionDisciplineScore) {
        this.sessionDisciplineScore = sessionDisciplineScore;
    }

    @java.lang.SuppressWarnings("all")
    public void setAiSessionReview(final AiSessionReviewResponse aiSessionReview) {
        this.aiSessionReview = aiSessionReview;
    }

    @java.lang.SuppressWarnings("all")
    public void setAiReviewCached(final Boolean aiReviewCached) {
        this.aiReviewCached = aiReviewCached;
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
    public void setCreditsConsumed(final Integer creditsConsumed) {
        this.creditsConsumed = creditsConsumed;
    }

    @java.lang.SuppressWarnings("all")
    public void setPreviousTradingDay(final LocalDate previousTradingDay) {
        this.previousTradingDay = previousTradingDay;
    }

    @java.lang.SuppressWarnings("all")
    public void setNextTradingDay(final LocalDate nextTradingDay) {
        this.nextTradingDay = nextTradingDay;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof SessionReviewResponse)) return false;
        final SessionReviewResponse other = (SessionReviewResponse) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$accountId = this.getAccountId();
        final java.lang.Object other$accountId = other.getAccountId();
        if (this$accountId == null ? other$accountId != null : !this$accountId.equals(other$accountId)) return false;
        final java.lang.Object this$totalTrades = this.getTotalTrades();
        final java.lang.Object other$totalTrades = other.getTotalTrades();
        if (this$totalTrades == null ? other$totalTrades != null : !this$totalTrades.equals(other$totalTrades)) return false;
        final java.lang.Object this$winners = this.getWinners();
        final java.lang.Object other$winners = other.getWinners();
        if (this$winners == null ? other$winners != null : !this$winners.equals(other$winners)) return false;
        final java.lang.Object this$losers = this.getLosers();
        final java.lang.Object other$losers = other.getLosers();
        if (this$losers == null ? other$losers != null : !this$losers.equals(other$losers)) return false;
        final java.lang.Object this$breakevenTrades = this.getBreakevenTrades();
        final java.lang.Object other$breakevenTrades = other.getBreakevenTrades();
        if (this$breakevenTrades == null ? other$breakevenTrades != null : !this$breakevenTrades.equals(other$breakevenTrades)) return false;
        final java.lang.Object this$sessionDisciplineScore = this.getSessionDisciplineScore();
        final java.lang.Object other$sessionDisciplineScore = other.getSessionDisciplineScore();
        if (this$sessionDisciplineScore == null ? other$sessionDisciplineScore != null : !this$sessionDisciplineScore.equals(other$sessionDisciplineScore)) return false;
        final java.lang.Object this$aiReviewCached = this.getAiReviewCached();
        final java.lang.Object other$aiReviewCached = other.getAiReviewCached();
        if (this$aiReviewCached == null ? other$aiReviewCached != null : !this$aiReviewCached.equals(other$aiReviewCached)) return false;
        final java.lang.Object this$aiGenerated = this.getAiGenerated();
        final java.lang.Object other$aiGenerated = other.getAiGenerated();
        if (this$aiGenerated == null ? other$aiGenerated != null : !this$aiGenerated.equals(other$aiGenerated)) return false;
        final java.lang.Object this$noTrades = this.getNoTrades();
        final java.lang.Object other$noTrades = other.getNoTrades();
        if (this$noTrades == null ? other$noTrades != null : !this$noTrades.equals(other$noTrades)) return false;
        final java.lang.Object this$creditsConsumed = this.getCreditsConsumed();
        final java.lang.Object other$creditsConsumed = other.getCreditsConsumed();
        if (this$creditsConsumed == null ? other$creditsConsumed != null : !this$creditsConsumed.equals(other$creditsConsumed)) return false;
        final java.lang.Object this$date = this.getDate();
        final java.lang.Object other$date = other.getDate();
        if (this$date == null ? other$date != null : !this$date.equals(other$date)) return false;
        final java.lang.Object this$sessionDate = this.getSessionDate();
        final java.lang.Object other$sessionDate = other.getSessionDate();
        if (this$sessionDate == null ? other$sessionDate != null : !this$sessionDate.equals(other$sessionDate)) return false;
        final java.lang.Object this$account = this.getAccount();
        final java.lang.Object other$account = other.getAccount();
        if (this$account == null ? other$account != null : !this$account.equals(other$account)) return false;
        final java.lang.Object this$trades = this.getTrades();
        final java.lang.Object other$trades = other.getTrades();
        if (this$trades == null ? other$trades != null : !this$trades.equals(other$trades)) return false;
        final java.lang.Object this$timelineTrades = this.getTimelineTrades();
        final java.lang.Object other$timelineTrades = other.getTimelineTrades();
        if (this$timelineTrades == null ? other$timelineTrades != null : !this$timelineTrades.equals(other$timelineTrades)) return false;
        final java.lang.Object this$netPnl = this.getNetPnl();
        final java.lang.Object other$netPnl = other.getNetPnl();
        if (this$netPnl == null ? other$netPnl != null : !this$netPnl.equals(other$netPnl)) return false;
        final java.lang.Object this$grossProfit = this.getGrossProfit();
        final java.lang.Object other$grossProfit = other.getGrossProfit();
        if (this$grossProfit == null ? other$grossProfit != null : !this$grossProfit.equals(other$grossProfit)) return false;
        final java.lang.Object this$grossLoss = this.getGrossLoss();
        final java.lang.Object other$grossLoss = other.getGrossLoss();
        if (this$grossLoss == null ? other$grossLoss != null : !this$grossLoss.equals(other$grossLoss)) return false;
        final java.lang.Object this$largestWinner = this.getLargestWinner();
        final java.lang.Object other$largestWinner = other.getLargestWinner();
        if (this$largestWinner == null ? other$largestWinner != null : !this$largestWinner.equals(other$largestWinner)) return false;
        final java.lang.Object this$largestLoser = this.getLargestLoser();
        final java.lang.Object other$largestLoser = other.getLargestLoser();
        if (this$largestLoser == null ? other$largestLoser != null : !this$largestLoser.equals(other$largestLoser)) return false;
        final java.lang.Object this$averageWinner = this.getAverageWinner();
        final java.lang.Object other$averageWinner = other.getAverageWinner();
        if (this$averageWinner == null ? other$averageWinner != null : !this$averageWinner.equals(other$averageWinner)) return false;
        final java.lang.Object this$averageLoser = this.getAverageLoser();
        final java.lang.Object other$averageLoser = other.getAverageLoser();
        if (this$averageLoser == null ? other$averageLoser != null : !this$averageLoser.equals(other$averageLoser)) return false;
        final java.lang.Object this$totalPnl = this.getTotalPnl();
        final java.lang.Object other$totalPnl = other.getTotalPnl();
        if (this$totalPnl == null ? other$totalPnl != null : !this$totalPnl.equals(other$totalPnl)) return false;
        final java.lang.Object this$winRate = this.getWinRate();
        final java.lang.Object other$winRate = other.getWinRate();
        if (this$winRate == null ? other$winRate != null : !this$winRate.equals(other$winRate)) return false;
        final java.lang.Object this$profitFactor = this.getProfitFactor();
        final java.lang.Object other$profitFactor = other.getProfitFactor();
        if (this$profitFactor == null ? other$profitFactor != null : !this$profitFactor.equals(other$profitFactor)) return false;
        final java.lang.Object this$sessionGrade = this.getSessionGrade();
        final java.lang.Object other$sessionGrade = other.getSessionGrade();
        if (this$sessionGrade == null ? other$sessionGrade != null : !this$sessionGrade.equals(other$sessionGrade)) return false;
        final java.lang.Object this$sessionStory = this.getSessionStory();
        final java.lang.Object other$sessionStory = other.getSessionStory();
        if (this$sessionStory == null ? other$sessionStory != null : !this$sessionStory.equals(other$sessionStory)) return false;
        final java.lang.Object this$bestTradeWhy = this.getBestTradeWhy();
        final java.lang.Object other$bestTradeWhy = other.getBestTradeWhy();
        if (this$bestTradeWhy == null ? other$bestTradeWhy != null : !this$bestTradeWhy.equals(other$bestTradeWhy)) return false;
        final java.lang.Object this$worstTradeWhy = this.getWorstTradeWhy();
        final java.lang.Object other$worstTradeWhy = other.getWorstTradeWhy();
        if (this$worstTradeWhy == null ? other$worstTradeWhy != null : !this$worstTradeWhy.equals(other$worstTradeWhy)) return false;
        final java.lang.Object this$bestTrade = this.getBestTrade();
        final java.lang.Object other$bestTrade = other.getBestTrade();
        if (this$bestTrade == null ? other$bestTrade != null : !this$bestTrade.equals(other$bestTrade)) return false;
        final java.lang.Object this$worstTrade = this.getWorstTrade();
        final java.lang.Object other$worstTrade = other.getWorstTrade();
        if (this$worstTrade == null ? other$worstTrade != null : !this$worstTrade.equals(other$worstTrade)) return false;
        final java.lang.Object this$mistakeSummary = this.getMistakeSummary();
        final java.lang.Object other$mistakeSummary = other.getMistakeSummary();
        if (this$mistakeSummary == null ? other$mistakeSummary != null : !this$mistakeSummary.equals(other$mistakeSummary)) return false;
        final java.lang.Object this$detectedMistakes = this.getDetectedMistakes();
        final java.lang.Object other$detectedMistakes = other.getDetectedMistakes();
        if (this$detectedMistakes == null ? other$detectedMistakes != null : !this$detectedMistakes.equals(other$detectedMistakes)) return false;
        final java.lang.Object this$replayInsights = this.getReplayInsights();
        final java.lang.Object other$replayInsights = other.getReplayInsights();
        if (this$replayInsights == null ? other$replayInsights != null : !this$replayInsights.equals(other$replayInsights)) return false;
        final java.lang.Object this$aiSessionReview = this.getAiSessionReview();
        final java.lang.Object other$aiSessionReview = other.getAiSessionReview();
        if (this$aiSessionReview == null ? other$aiSessionReview != null : !this$aiSessionReview.equals(other$aiSessionReview)) return false;
        final java.lang.Object this$previousTradingDay = this.getPreviousTradingDay();
        final java.lang.Object other$previousTradingDay = other.getPreviousTradingDay();
        if (this$previousTradingDay == null ? other$previousTradingDay != null : !this$previousTradingDay.equals(other$previousTradingDay)) return false;
        final java.lang.Object this$nextTradingDay = this.getNextTradingDay();
        final java.lang.Object other$nextTradingDay = other.getNextTradingDay();
        if (this$nextTradingDay == null ? other$nextTradingDay != null : !this$nextTradingDay.equals(other$nextTradingDay)) return false;
        return true;
    }

    @java.lang.SuppressWarnings("all")
    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof SessionReviewResponse;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $accountId = this.getAccountId();
        result = result * PRIME + ($accountId == null ? 43 : $accountId.hashCode());
        final java.lang.Object $totalTrades = this.getTotalTrades();
        result = result * PRIME + ($totalTrades == null ? 43 : $totalTrades.hashCode());
        final java.lang.Object $winners = this.getWinners();
        result = result * PRIME + ($winners == null ? 43 : $winners.hashCode());
        final java.lang.Object $losers = this.getLosers();
        result = result * PRIME + ($losers == null ? 43 : $losers.hashCode());
        final java.lang.Object $breakevenTrades = this.getBreakevenTrades();
        result = result * PRIME + ($breakevenTrades == null ? 43 : $breakevenTrades.hashCode());
        final java.lang.Object $sessionDisciplineScore = this.getSessionDisciplineScore();
        result = result * PRIME + ($sessionDisciplineScore == null ? 43 : $sessionDisciplineScore.hashCode());
        final java.lang.Object $aiReviewCached = this.getAiReviewCached();
        result = result * PRIME + ($aiReviewCached == null ? 43 : $aiReviewCached.hashCode());
        final java.lang.Object $aiGenerated = this.getAiGenerated();
        result = result * PRIME + ($aiGenerated == null ? 43 : $aiGenerated.hashCode());
        final java.lang.Object $noTrades = this.getNoTrades();
        result = result * PRIME + ($noTrades == null ? 43 : $noTrades.hashCode());
        final java.lang.Object $creditsConsumed = this.getCreditsConsumed();
        result = result * PRIME + ($creditsConsumed == null ? 43 : $creditsConsumed.hashCode());
        final java.lang.Object $date = this.getDate();
        result = result * PRIME + ($date == null ? 43 : $date.hashCode());
        final java.lang.Object $sessionDate = this.getSessionDate();
        result = result * PRIME + ($sessionDate == null ? 43 : $sessionDate.hashCode());
        final java.lang.Object $account = this.getAccount();
        result = result * PRIME + ($account == null ? 43 : $account.hashCode());
        final java.lang.Object $trades = this.getTrades();
        result = result * PRIME + ($trades == null ? 43 : $trades.hashCode());
        final java.lang.Object $timelineTrades = this.getTimelineTrades();
        result = result * PRIME + ($timelineTrades == null ? 43 : $timelineTrades.hashCode());
        final java.lang.Object $netPnl = this.getNetPnl();
        result = result * PRIME + ($netPnl == null ? 43 : $netPnl.hashCode());
        final java.lang.Object $grossProfit = this.getGrossProfit();
        result = result * PRIME + ($grossProfit == null ? 43 : $grossProfit.hashCode());
        final java.lang.Object $grossLoss = this.getGrossLoss();
        result = result * PRIME + ($grossLoss == null ? 43 : $grossLoss.hashCode());
        final java.lang.Object $largestWinner = this.getLargestWinner();
        result = result * PRIME + ($largestWinner == null ? 43 : $largestWinner.hashCode());
        final java.lang.Object $largestLoser = this.getLargestLoser();
        result = result * PRIME + ($largestLoser == null ? 43 : $largestLoser.hashCode());
        final java.lang.Object $averageWinner = this.getAverageWinner();
        result = result * PRIME + ($averageWinner == null ? 43 : $averageWinner.hashCode());
        final java.lang.Object $averageLoser = this.getAverageLoser();
        result = result * PRIME + ($averageLoser == null ? 43 : $averageLoser.hashCode());
        final java.lang.Object $totalPnl = this.getTotalPnl();
        result = result * PRIME + ($totalPnl == null ? 43 : $totalPnl.hashCode());
        final java.lang.Object $winRate = this.getWinRate();
        result = result * PRIME + ($winRate == null ? 43 : $winRate.hashCode());
        final java.lang.Object $profitFactor = this.getProfitFactor();
        result = result * PRIME + ($profitFactor == null ? 43 : $profitFactor.hashCode());
        final java.lang.Object $sessionGrade = this.getSessionGrade();
        result = result * PRIME + ($sessionGrade == null ? 43 : $sessionGrade.hashCode());
        final java.lang.Object $sessionStory = this.getSessionStory();
        result = result * PRIME + ($sessionStory == null ? 43 : $sessionStory.hashCode());
        final java.lang.Object $bestTradeWhy = this.getBestTradeWhy();
        result = result * PRIME + ($bestTradeWhy == null ? 43 : $bestTradeWhy.hashCode());
        final java.lang.Object $worstTradeWhy = this.getWorstTradeWhy();
        result = result * PRIME + ($worstTradeWhy == null ? 43 : $worstTradeWhy.hashCode());
        final java.lang.Object $bestTrade = this.getBestTrade();
        result = result * PRIME + ($bestTrade == null ? 43 : $bestTrade.hashCode());
        final java.lang.Object $worstTrade = this.getWorstTrade();
        result = result * PRIME + ($worstTrade == null ? 43 : $worstTrade.hashCode());
        final java.lang.Object $mistakeSummary = this.getMistakeSummary();
        result = result * PRIME + ($mistakeSummary == null ? 43 : $mistakeSummary.hashCode());
        final java.lang.Object $detectedMistakes = this.getDetectedMistakes();
        result = result * PRIME + ($detectedMistakes == null ? 43 : $detectedMistakes.hashCode());
        final java.lang.Object $replayInsights = this.getReplayInsights();
        result = result * PRIME + ($replayInsights == null ? 43 : $replayInsights.hashCode());
        final java.lang.Object $aiSessionReview = this.getAiSessionReview();
        result = result * PRIME + ($aiSessionReview == null ? 43 : $aiSessionReview.hashCode());
        final java.lang.Object $previousTradingDay = this.getPreviousTradingDay();
        result = result * PRIME + ($previousTradingDay == null ? 43 : $previousTradingDay.hashCode());
        final java.lang.Object $nextTradingDay = this.getNextTradingDay();
        result = result * PRIME + ($nextTradingDay == null ? 43 : $nextTradingDay.hashCode());
        return result;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public java.lang.String toString() {
        return "SessionReviewResponse(accountId=" + this.getAccountId() + ", date=" + this.getDate() + ", sessionDate=" + this.getSessionDate() + ", account=" + this.getAccount() + ", trades=" + this.getTrades() + ", timelineTrades=" + this.getTimelineTrades() + ", totalTrades=" + this.getTotalTrades() + ", winners=" + this.getWinners() + ", losers=" + this.getLosers() + ", breakevenTrades=" + this.getBreakevenTrades() + ", netPnl=" + this.getNetPnl() + ", grossProfit=" + this.getGrossProfit() + ", grossLoss=" + this.getGrossLoss() + ", largestWinner=" + this.getLargestWinner() + ", largestLoser=" + this.getLargestLoser() + ", averageWinner=" + this.getAverageWinner() + ", averageLoser=" + this.getAverageLoser() + ", totalPnl=" + this.getTotalPnl() + ", winRate=" + this.getWinRate() + ", profitFactor=" + this.getProfitFactor() + ", sessionGrade=" + this.getSessionGrade() + ", sessionStory=" + this.getSessionStory() + ", bestTradeWhy=" + this.getBestTradeWhy() + ", worstTradeWhy=" + this.getWorstTradeWhy() + ", bestTrade=" + this.getBestTrade() + ", worstTrade=" + this.getWorstTrade() + ", mistakeSummary=" + this.getMistakeSummary() + ", detectedMistakes=" + this.getDetectedMistakes() + ", replayInsights=" + this.getReplayInsights() + ", sessionDisciplineScore=" + this.getSessionDisciplineScore() + ", aiSessionReview=" + this.getAiSessionReview() + ", aiReviewCached=" + this.getAiReviewCached() + ", aiGenerated=" + this.getAiGenerated() + ", noTrades=" + this.getNoTrades() + ", creditsConsumed=" + this.getCreditsConsumed() + ", previousTradingDay=" + this.getPreviousTradingDay() + ", nextTradingDay=" + this.getNextTradingDay() + ")";
    }
}
