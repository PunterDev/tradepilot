package com.tradepilot.dto.session;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class SessionReplayInsightsResponse {
    private SessionTimelineTradeResponse firstTrade;
    private SessionTimelineTradeResponse lastTrade;
    private SessionTimelineTradeResponse largestWinner;
    private SessionTimelineTradeResponse largestLoser;
    private SessionTimelineTradeResponse longestHold;
    private SessionTimelineTradeResponse shortestHold;
    private Integer longestHoldMinutes;
    private Integer shortestHoldMinutes;
    private Integer bestTradingHour;
    private BigDecimal bestTradingHourPnl;
    private Integer worstTradingHour;
    private BigDecimal worstTradingHourPnl;
    private LocalDateTime firstTradeTime;
    private LocalDateTime lastTradeTime;

    public static SessionReplayInsightsResponse of(SessionTimelineTradeResponse firstTrade, SessionTimelineTradeResponse lastTrade, SessionTimelineTradeResponse largestWinner, SessionTimelineTradeResponse largestLoser, SessionTimelineTradeResponse longestHold, SessionTimelineTradeResponse shortestHold, Integer longestHoldMinutes, Integer shortestHoldMinutes, Integer bestTradingHour, BigDecimal bestTradingHourPnl, Integer worstTradingHour, BigDecimal worstTradingHourPnl, LocalDateTime firstTradeTime, LocalDateTime lastTradeTime) {
        SessionReplayInsightsResponse response = new SessionReplayInsightsResponse();
        response.setFirstTrade(firstTrade);
        response.setLastTrade(lastTrade);
        response.setLargestWinner(largestWinner);
        response.setLargestLoser(largestLoser);
        response.setLongestHold(longestHold);
        response.setShortestHold(shortestHold);
        response.setLongestHoldMinutes(longestHoldMinutes);
        response.setShortestHoldMinutes(shortestHoldMinutes);
        response.setBestTradingHour(bestTradingHour);
        response.setBestTradingHourPnl(bestTradingHourPnl);
        response.setWorstTradingHour(worstTradingHour);
        response.setWorstTradingHourPnl(worstTradingHourPnl);
        response.setFirstTradeTime(firstTradeTime);
        response.setLastTradeTime(lastTradeTime);
        return response;
    }

    @java.lang.SuppressWarnings("all")
    public SessionReplayInsightsResponse() {
    }

    @java.lang.SuppressWarnings("all")
    public SessionTimelineTradeResponse getFirstTrade() {
        return this.firstTrade;
    }

    @java.lang.SuppressWarnings("all")
    public SessionTimelineTradeResponse getLastTrade() {
        return this.lastTrade;
    }

    @java.lang.SuppressWarnings("all")
    public SessionTimelineTradeResponse getLargestWinner() {
        return this.largestWinner;
    }

    @java.lang.SuppressWarnings("all")
    public SessionTimelineTradeResponse getLargestLoser() {
        return this.largestLoser;
    }

    @java.lang.SuppressWarnings("all")
    public SessionTimelineTradeResponse getLongestHold() {
        return this.longestHold;
    }

    @java.lang.SuppressWarnings("all")
    public SessionTimelineTradeResponse getShortestHold() {
        return this.shortestHold;
    }

    @java.lang.SuppressWarnings("all")
    public Integer getLongestHoldMinutes() {
        return this.longestHoldMinutes;
    }

    @java.lang.SuppressWarnings("all")
    public Integer getShortestHoldMinutes() {
        return this.shortestHoldMinutes;
    }

    @java.lang.SuppressWarnings("all")
    public Integer getBestTradingHour() {
        return this.bestTradingHour;
    }

    @java.lang.SuppressWarnings("all")
    public BigDecimal getBestTradingHourPnl() {
        return this.bestTradingHourPnl;
    }

    @java.lang.SuppressWarnings("all")
    public Integer getWorstTradingHour() {
        return this.worstTradingHour;
    }

    @java.lang.SuppressWarnings("all")
    public BigDecimal getWorstTradingHourPnl() {
        return this.worstTradingHourPnl;
    }

    @java.lang.SuppressWarnings("all")
    public LocalDateTime getFirstTradeTime() {
        return this.firstTradeTime;
    }

    @java.lang.SuppressWarnings("all")
    public LocalDateTime getLastTradeTime() {
        return this.lastTradeTime;
    }

    @java.lang.SuppressWarnings("all")
    public void setFirstTrade(final SessionTimelineTradeResponse firstTrade) {
        this.firstTrade = firstTrade;
    }

    @java.lang.SuppressWarnings("all")
    public void setLastTrade(final SessionTimelineTradeResponse lastTrade) {
        this.lastTrade = lastTrade;
    }

    @java.lang.SuppressWarnings("all")
    public void setLargestWinner(final SessionTimelineTradeResponse largestWinner) {
        this.largestWinner = largestWinner;
    }

    @java.lang.SuppressWarnings("all")
    public void setLargestLoser(final SessionTimelineTradeResponse largestLoser) {
        this.largestLoser = largestLoser;
    }

    @java.lang.SuppressWarnings("all")
    public void setLongestHold(final SessionTimelineTradeResponse longestHold) {
        this.longestHold = longestHold;
    }

    @java.lang.SuppressWarnings("all")
    public void setShortestHold(final SessionTimelineTradeResponse shortestHold) {
        this.shortestHold = shortestHold;
    }

    @java.lang.SuppressWarnings("all")
    public void setLongestHoldMinutes(final Integer longestHoldMinutes) {
        this.longestHoldMinutes = longestHoldMinutes;
    }

    @java.lang.SuppressWarnings("all")
    public void setShortestHoldMinutes(final Integer shortestHoldMinutes) {
        this.shortestHoldMinutes = shortestHoldMinutes;
    }

    @java.lang.SuppressWarnings("all")
    public void setBestTradingHour(final Integer bestTradingHour) {
        this.bestTradingHour = bestTradingHour;
    }

    @java.lang.SuppressWarnings("all")
    public void setBestTradingHourPnl(final BigDecimal bestTradingHourPnl) {
        this.bestTradingHourPnl = bestTradingHourPnl;
    }

    @java.lang.SuppressWarnings("all")
    public void setWorstTradingHour(final Integer worstTradingHour) {
        this.worstTradingHour = worstTradingHour;
    }

    @java.lang.SuppressWarnings("all")
    public void setWorstTradingHourPnl(final BigDecimal worstTradingHourPnl) {
        this.worstTradingHourPnl = worstTradingHourPnl;
    }

    @java.lang.SuppressWarnings("all")
    public void setFirstTradeTime(final LocalDateTime firstTradeTime) {
        this.firstTradeTime = firstTradeTime;
    }

    @java.lang.SuppressWarnings("all")
    public void setLastTradeTime(final LocalDateTime lastTradeTime) {
        this.lastTradeTime = lastTradeTime;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof SessionReplayInsightsResponse)) return false;
        final SessionReplayInsightsResponse other = (SessionReplayInsightsResponse) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$longestHoldMinutes = this.getLongestHoldMinutes();
        final java.lang.Object other$longestHoldMinutes = other.getLongestHoldMinutes();
        if (this$longestHoldMinutes == null ? other$longestHoldMinutes != null : !this$longestHoldMinutes.equals(other$longestHoldMinutes)) return false;
        final java.lang.Object this$shortestHoldMinutes = this.getShortestHoldMinutes();
        final java.lang.Object other$shortestHoldMinutes = other.getShortestHoldMinutes();
        if (this$shortestHoldMinutes == null ? other$shortestHoldMinutes != null : !this$shortestHoldMinutes.equals(other$shortestHoldMinutes)) return false;
        final java.lang.Object this$bestTradingHour = this.getBestTradingHour();
        final java.lang.Object other$bestTradingHour = other.getBestTradingHour();
        if (this$bestTradingHour == null ? other$bestTradingHour != null : !this$bestTradingHour.equals(other$bestTradingHour)) return false;
        final java.lang.Object this$worstTradingHour = this.getWorstTradingHour();
        final java.lang.Object other$worstTradingHour = other.getWorstTradingHour();
        if (this$worstTradingHour == null ? other$worstTradingHour != null : !this$worstTradingHour.equals(other$worstTradingHour)) return false;
        final java.lang.Object this$firstTrade = this.getFirstTrade();
        final java.lang.Object other$firstTrade = other.getFirstTrade();
        if (this$firstTrade == null ? other$firstTrade != null : !this$firstTrade.equals(other$firstTrade)) return false;
        final java.lang.Object this$lastTrade = this.getLastTrade();
        final java.lang.Object other$lastTrade = other.getLastTrade();
        if (this$lastTrade == null ? other$lastTrade != null : !this$lastTrade.equals(other$lastTrade)) return false;
        final java.lang.Object this$largestWinner = this.getLargestWinner();
        final java.lang.Object other$largestWinner = other.getLargestWinner();
        if (this$largestWinner == null ? other$largestWinner != null : !this$largestWinner.equals(other$largestWinner)) return false;
        final java.lang.Object this$largestLoser = this.getLargestLoser();
        final java.lang.Object other$largestLoser = other.getLargestLoser();
        if (this$largestLoser == null ? other$largestLoser != null : !this$largestLoser.equals(other$largestLoser)) return false;
        final java.lang.Object this$longestHold = this.getLongestHold();
        final java.lang.Object other$longestHold = other.getLongestHold();
        if (this$longestHold == null ? other$longestHold != null : !this$longestHold.equals(other$longestHold)) return false;
        final java.lang.Object this$shortestHold = this.getShortestHold();
        final java.lang.Object other$shortestHold = other.getShortestHold();
        if (this$shortestHold == null ? other$shortestHold != null : !this$shortestHold.equals(other$shortestHold)) return false;
        final java.lang.Object this$bestTradingHourPnl = this.getBestTradingHourPnl();
        final java.lang.Object other$bestTradingHourPnl = other.getBestTradingHourPnl();
        if (this$bestTradingHourPnl == null ? other$bestTradingHourPnl != null : !this$bestTradingHourPnl.equals(other$bestTradingHourPnl)) return false;
        final java.lang.Object this$worstTradingHourPnl = this.getWorstTradingHourPnl();
        final java.lang.Object other$worstTradingHourPnl = other.getWorstTradingHourPnl();
        if (this$worstTradingHourPnl == null ? other$worstTradingHourPnl != null : !this$worstTradingHourPnl.equals(other$worstTradingHourPnl)) return false;
        final java.lang.Object this$firstTradeTime = this.getFirstTradeTime();
        final java.lang.Object other$firstTradeTime = other.getFirstTradeTime();
        if (this$firstTradeTime == null ? other$firstTradeTime != null : !this$firstTradeTime.equals(other$firstTradeTime)) return false;
        final java.lang.Object this$lastTradeTime = this.getLastTradeTime();
        final java.lang.Object other$lastTradeTime = other.getLastTradeTime();
        if (this$lastTradeTime == null ? other$lastTradeTime != null : !this$lastTradeTime.equals(other$lastTradeTime)) return false;
        return true;
    }

    @java.lang.SuppressWarnings("all")
    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof SessionReplayInsightsResponse;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $longestHoldMinutes = this.getLongestHoldMinutes();
        result = result * PRIME + ($longestHoldMinutes == null ? 43 : $longestHoldMinutes.hashCode());
        final java.lang.Object $shortestHoldMinutes = this.getShortestHoldMinutes();
        result = result * PRIME + ($shortestHoldMinutes == null ? 43 : $shortestHoldMinutes.hashCode());
        final java.lang.Object $bestTradingHour = this.getBestTradingHour();
        result = result * PRIME + ($bestTradingHour == null ? 43 : $bestTradingHour.hashCode());
        final java.lang.Object $worstTradingHour = this.getWorstTradingHour();
        result = result * PRIME + ($worstTradingHour == null ? 43 : $worstTradingHour.hashCode());
        final java.lang.Object $firstTrade = this.getFirstTrade();
        result = result * PRIME + ($firstTrade == null ? 43 : $firstTrade.hashCode());
        final java.lang.Object $lastTrade = this.getLastTrade();
        result = result * PRIME + ($lastTrade == null ? 43 : $lastTrade.hashCode());
        final java.lang.Object $largestWinner = this.getLargestWinner();
        result = result * PRIME + ($largestWinner == null ? 43 : $largestWinner.hashCode());
        final java.lang.Object $largestLoser = this.getLargestLoser();
        result = result * PRIME + ($largestLoser == null ? 43 : $largestLoser.hashCode());
        final java.lang.Object $longestHold = this.getLongestHold();
        result = result * PRIME + ($longestHold == null ? 43 : $longestHold.hashCode());
        final java.lang.Object $shortestHold = this.getShortestHold();
        result = result * PRIME + ($shortestHold == null ? 43 : $shortestHold.hashCode());
        final java.lang.Object $bestTradingHourPnl = this.getBestTradingHourPnl();
        result = result * PRIME + ($bestTradingHourPnl == null ? 43 : $bestTradingHourPnl.hashCode());
        final java.lang.Object $worstTradingHourPnl = this.getWorstTradingHourPnl();
        result = result * PRIME + ($worstTradingHourPnl == null ? 43 : $worstTradingHourPnl.hashCode());
        final java.lang.Object $firstTradeTime = this.getFirstTradeTime();
        result = result * PRIME + ($firstTradeTime == null ? 43 : $firstTradeTime.hashCode());
        final java.lang.Object $lastTradeTime = this.getLastTradeTime();
        result = result * PRIME + ($lastTradeTime == null ? 43 : $lastTradeTime.hashCode());
        return result;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public java.lang.String toString() {
        return "SessionReplayInsightsResponse(firstTrade=" + this.getFirstTrade() + ", lastTrade=" + this.getLastTrade() + ", largestWinner=" + this.getLargestWinner() + ", largestLoser=" + this.getLargestLoser() + ", longestHold=" + this.getLongestHold() + ", shortestHold=" + this.getShortestHold() + ", longestHoldMinutes=" + this.getLongestHoldMinutes() + ", shortestHoldMinutes=" + this.getShortestHoldMinutes() + ", bestTradingHour=" + this.getBestTradingHour() + ", bestTradingHourPnl=" + this.getBestTradingHourPnl() + ", worstTradingHour=" + this.getWorstTradingHour() + ", worstTradingHourPnl=" + this.getWorstTradingHourPnl() + ", firstTradeTime=" + this.getFirstTradeTime() + ", lastTradeTime=" + this.getLastTradeTime() + ")";
    }
}
