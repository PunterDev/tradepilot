package com.tradepilot;

import com.tradepilot.billing.AiUsageEventRepository;
import com.tradepilot.billing.PlanType;
import com.tradepilot.billing.SubscriptionStatus;
import com.tradepilot.billing.UsageType;
import com.tradepilot.dto.session.SessionReviewResponse;
import com.tradepilot.dto.trade.TradeImportRequest;
import com.tradepilot.dto.trade.TradeResponse;
import com.tradepilot.entity.Trade;
import com.tradepilot.entity.TradingAccount;
import com.tradepilot.entity.User;
import com.tradepilot.enums.OutcomeType;
import com.tradepilot.enums.PropFirmType;
import com.tradepilot.enums.SessionType;
import com.tradepilot.enums.Side;
import com.tradepilot.enums.SetupType;
import com.tradepilot.enums.TradingAccountStatus;
import com.tradepilot.enums.TradingAccountType;
import com.tradepilot.enums.UserRole;
import com.tradepilot.observability.CacheMetricRepository;
import com.tradepilot.repository.TradeRepository;
import com.tradepilot.repository.TradingAccountRepository;
import com.tradepilot.repository.UserRepository;
import com.tradepilot.service.SessionReviewService;
import com.tradepilot.service.TradeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class SessionReviewServiceTests {
    private static final AtomicInteger SEQUENCE = new AtomicInteger();
    private static final LocalDate SESSION_DATE = LocalDate.of(2026, 4, 1);

    @Autowired SessionReviewService sessionReviewService;
    @Autowired UserRepository userRepository;
    @Autowired TradingAccountRepository tradingAccountRepository;
    @Autowired TradeRepository tradeRepository;
    @Autowired AiUsageEventRepository aiUsageEventRepository;
    @Autowired CacheMetricRepository cacheMetricRepository;
    @Autowired TradeService tradeService;

    @Test
    void oneLosingTradeHasNoBestTradeAndKeepsWorstTrade() {
        Fixture fixture = fixture("one-losing");
        trade(fixture, "MESM6", "SHORT", "-140.00", 14);

        SessionReviewResponse review = sessionReviewService.getSessionReview(fixture.user(), fixture.account().getId(), SESSION_DATE);

        assertThat(review.getBestTrade()).isNull();
        assertTrade(review.getWorstTrade(), "MESM6", "-140.00");
        assertThat(review.getSessionStory()).contains("single losing trade");
        assertThat(review.getSessionStory()).doesNotContain("best execution");
        assertThat(review.getReplayInsights().getBestTradingHour()).isNull();
        assertThat(review.getReplayInsights().getBestTradingHourPnl()).isNull();
        assertThat(review.getReplayInsights().getWorstTradingHour()).isEqualTo(14);
        assertThat(review.getReplayInsights().getWorstTradingHourPnl()).isEqualByComparingTo("-140.00");
    }

    @Test
    void oneWinningTradeHasBestTradeAndNoWorstTrade() {
        Fixture fixture = fixture("one-winning");
        trade(fixture, "NQM6", "LONG", "225.00", 10);

        SessionReviewResponse review = sessionReviewService.getSessionReview(fixture.user(), fixture.account().getId(), SESSION_DATE);

        assertTrade(review.getBestTrade(), "NQM6", "225.00");
        assertThat(review.getWorstTrade()).isNull();
        assertThat(review.getSessionStory()).contains("The strongest trade was NQM6 LONG");
        assertThat(review.getSessionStory()).doesNotContain("largest damage");
        assertThat(review.getReplayInsights().getBestTradingHour()).isEqualTo(10);
        assertThat(review.getReplayInsights().getBestTradingHourPnl()).isEqualByComparingTo("225.00");
        assertThat(review.getReplayInsights().getWorstTradingHour()).isNull();
    }

    @Test
    void oneBreakevenTradeHasNoMeaningfulWinner() {
        Fixture fixture = fixture("one-breakeven");
        trade(fixture, "ESM6", "LONG", "0.00", 11);

        SessionReviewResponse review = sessionReviewService.getSessionReview(fixture.user(), fixture.account().getId(), SESSION_DATE);

        assertThat(review.getBestTrade()).isNull();
        assertThat(review.getWorstTrade()).isNull();
        assertThat(review.getBestTradeWhy()).contains("No meaningful winning trade was found");
        assertThat(review.getSessionStory()).contains("There was no meaningful winning trade");
        assertThat(review.getSessionStory()).doesNotContain("strongest trade");
        assertThat(review.getReplayInsights().getLargestWinner()).isNull();
        assertThat(review.getReplayInsights().getBestTradingHour()).isNull();
        assertThat(review.getReplayInsights().getBestTradingHourPnl()).isNull();
        assertThat(review.getReplayInsights().getWorstTradingHour()).isNull();
    }

    @Test
    void scratchWinnerIsNotBestTradeOrLargestWinner() {
        Fixture fixture = fixture("scratch-winner");
        trade(fixture, "MESM6", "LONG", "1.25", 9);
        trade(fixture, "MESM6", "SHORT", "-138.75", 10);
        trade(fixture, "MNQM6", "LONG", "0.00", 11);

        SessionReviewResponse review = sessionReviewService.getSessionReview(fixture.user(), fixture.account().getId(), SESSION_DATE);

        assertThat(review.getBestTrade()).isNull();
        assertTrade(review.getWorstTrade(), "MESM6", "-138.75");
        assertThat(review.getLargestWinner()).isEqualByComparingTo("0.00");
        assertThat(review.getReplayInsights().getLargestWinner()).isNull();
        assertThat(review.getReplayInsights().getLargestLoser().getSymbol()).isEqualTo("MESM6");
        assertThat(review.getSessionStory()).contains("essentially a scratch winner");
        assertThat(review.getSessionStory()).contains("did not materially affect the session");
        assertThat(review.getSessionStory()).doesNotContain("The strongest trade was MESM6 LONG for 1.25");
        assertThat(review.getTimelineTrades())
                .filteredOn(trade -> trade.getTradeId().equals(review.getTrades().get(0).getId()))
                .first()
                .extracting("outcome")
                .isEqualTo(OutcomeType.BE);
    }

    @Test
    void tradeBelowScratchThresholdIsNotMeaningfulWinner() {
        Fixture fixture = fixture("below-threshold");
        trade(fixture, "MESM6", "LONG", "4.99", 9);
        trade(fixture, "MESM6", "SHORT", "-100.00", 10);

        SessionReviewResponse review = sessionReviewService.getSessionReview(fixture.user(), fixture.account().getId(), SESSION_DATE);

        assertThat(review.getBestTrade()).isNull();
        assertThat(review.getReplayInsights().getLargestWinner()).isNull();
        assertThat(review.getSessionStory()).contains("essentially a scratch winner");
    }

    @Test
    void tradeAboveScratchThresholdIsMeaningfulBestTrade() {
        Fixture fixture = fixture("above-threshold");
        trade(fixture, "MESM6", "LONG", "75.00", 9);
        trade(fixture, "MESM6", "SHORT", "-25.00", 10);

        SessionReviewResponse review = sessionReviewService.getSessionReview(fixture.user(), fixture.account().getId(), SESSION_DATE);

        assertTrade(review.getBestTrade(), "MESM6", "75.00");
        assertThat(review.getReplayInsights().getLargestWinner()).isNotNull();
        assertThat(review.getReplayInsights().getLargestWinner().getPnl()).isEqualByComparingTo("75.00");
        assertThat(review.getSessionStory()).contains("The strongest trade was MESM6 LONG for 75.00");
    }

    @Test
    void multipleMixedTradesUseMaxAndMinPnl() {
        Fixture fixture = fixture("mixed");
        trade(fixture, "MESH6", "SHORT", "-20.00", 9);
        trade(fixture, "NQH6", "LONG", "300.00", 10);
        trade(fixture, "ESH6", "LONG", "0.00", 11);
        trade(fixture, "MESM6", "SHORT", "-140.00", 14);

        SessionReviewResponse review = sessionReviewService.getSessionReview(fixture.user(), fixture.account().getId(), SESSION_DATE);

        assertTrade(review.getBestTrade(), "NQH6", "300.00");
        assertTrade(review.getWorstTrade(), "MESM6", "-140.00");
        assertThat(review.getSessionStory()).contains("The strongest trade was NQH6 LONG");
        assertThat(review.getSessionStory()).contains("The largest damage came from MESM6 SHORT");
        assertThat(review.getReplayInsights().getBestTradingHour()).isEqualTo(10);
        assertThat(review.getReplayInsights().getBestTradingHourPnl()).isEqualByComparingTo("300.00");
        assertThat(review.getReplayInsights().getWorstTradingHour()).isEqualTo(14);
        assertThat(review.getReplayInsights().getWorstTradingHourPnl()).isEqualByComparingTo("-140.00");
    }

    @Test
    void allLosingTradesHaveNoBestTradeAndWorstIsLargestLoss() {
        Fixture fixture = fixture("all-losing");
        trade(fixture, "MESH6", "LONG", "-25.00", 9);
        trade(fixture, "MESM6", "SHORT", "-140.00", 14);

        SessionReviewResponse review = sessionReviewService.getSessionReview(fixture.user(), fixture.account().getId(), SESSION_DATE);

        assertThat(review.getBestTrade()).isNull();
        assertTrade(review.getWorstTrade(), "MESM6", "-140.00");
        assertThat(review.getSessionStory()).contains("There was no meaningful winning trade in this session");
        assertThat(review.getReplayInsights().getBestTradingHour()).isNull();
        assertThat(review.getReplayInsights().getWorstTradingHour()).isEqualTo(14);
    }

    @Test
    void sessionReviewPreservesLongDirectionForLosingTrade() {
        Fixture fixture = fixture("long-loser-direction");
        trade(fixture, "MNQU6", "LONG", "-200.50", 9);

        SessionReviewResponse review = sessionReviewService.getSessionReview(fixture.user(), fixture.account().getId(), SESSION_DATE);

        assertThat(review.getTimelineTrades()).first()
                .satisfies(trade -> {
                    assertThat(trade.getSymbol()).isEqualTo("MNQU6");
                    assertThat(trade.getSide()).isEqualTo("LONG");
                });
        assertThat(review.getWorstTrade()).isNotNull();
        assertThat(review.getWorstTrade().getSymbol()).isEqualTo("MNQU6");
        assertThat(review.getWorstTrade().getSide()).isEqualTo(Side.LONG);
        assertThat(review.getReplayInsights().getLargestLoser()).isNotNull();
        assertThat(review.getReplayInsights().getLargestLoser().getSide()).isEqualTo("LONG");
        assertThat(review.getSessionStory()).contains("MNQU6 LONG");
        assertThat(review.getSessionStory()).doesNotContain("MNQU6 SHORT");
    }

    @Test
    void sessionReviewPreservesShortDirectionForLosingTrade() {
        Fixture fixture = fixture("short-loser-direction");
        trade(fixture, "MESU6", "SHORT", "-161.25", 9);

        SessionReviewResponse review = sessionReviewService.getSessionReview(fixture.user(), fixture.account().getId(), SESSION_DATE);

        assertThat(review.getTimelineTrades()).first()
                .satisfies(trade -> {
                    assertThat(trade.getSymbol()).isEqualTo("MESU6");
                    assertThat(trade.getSide()).isEqualTo("SHORT");
                });
        assertThat(review.getWorstTrade()).isNotNull();
        assertThat(review.getWorstTrade().getSide()).isEqualTo(Side.SHORT);
        assertThat(review.getReplayInsights().getLargestLoser()).isNotNull();
        assertThat(review.getReplayInsights().getLargestLoser().getSide()).isEqualTo("SHORT");
    }

    @Test
    void sessionReviewUsesUnknownForInvalidDirection() {
        Fixture fixture = fixture("unknown-direction");
        trade(fixture, "MNQU6", "SIDEWAYS", "-200.50", 9);

        SessionReviewResponse review = sessionReviewService.getSessionReview(fixture.user(), fixture.account().getId(), SESSION_DATE);

        assertThat(review.getTimelineTrades()).first()
                .satisfies(trade -> {
                    assertThat(trade.getSymbol()).isEqualTo("MNQU6");
                    assertThat(trade.getSide()).isEqualTo("UNKNOWN");
                });
        assertThat(review.getWorstTrade()).isNotNull();
        assertThat(review.getWorstTrade().getSide()).isEqualTo(Side.UNKNOWN);
        assertThat(review.getSessionStory()).contains("MNQU6 UNKNOWN");
        assertThat(review.getSessionStory()).doesNotContain("MNQU6 SHORT");
    }

    @Test
    void tradovateDirectionRepairFixesStaleLongLoserAndInvalidatesSessionReviewCache() {
        Fixture fixture = fixture("repair-long-loser");
        staleImportedTrade(
                fixture,
                "MNQU6",
                "SHORT",
                "-200.50",
                "6/16/2026 9:30:00 AM",
                "6/16/2026 9:42:00 AM",
                "19450.25",
                "19440.25"
        );

        SessionReviewResponse stale = sessionReviewService.getSessionReview(fixture.user(), fixture.account().getId(), LocalDate.of(2026, 6, 16));
        assertThat(stale.getTimelineTrades()).first().extracting("side").isEqualTo("SHORT");

        var repairResult = tradeService.repairTradovateDirections(fixture.user(), fixture.account().getId(), LocalDate.of(2026, 6, 16));
        assertThat(repairResult.get("updated")).isEqualTo(1);

        SessionReviewResponse repaired = sessionReviewService.getSessionReview(fixture.user(), fixture.account().getId(), LocalDate.of(2026, 6, 16));
        assertThat(repaired.getTimelineTrades()).first()
                .satisfies(trade -> {
                    assertThat(trade.getSymbol()).isEqualTo("MNQU6");
                    assertThat(trade.getSide()).isEqualTo("LONG");
                });
        assertThat(repaired.getWorstTrade()).isNotNull();
        assertThat(repaired.getWorstTrade().getSide()).isEqualTo(Side.LONG);
        assertThat(repaired.getReplayInsights().getLargestLoser()).isNotNull();
        assertThat(repaired.getReplayInsights().getLargestLoser().getSide()).isEqualTo("LONG");
        assertThat(repaired.getSessionStory()).contains("MNQU6 LONG");
        assertThat(repaired.getSessionStory()).doesNotContain("MNQU6 SHORT");
    }

    @Test
    void tradovateImportInfersLongLoserFromNoSecondsBuySellTimestampsWhenRequestSideUnknown() {
        Fixture fixture = fixture("import-long-loser-no-seconds");
        String boughtTimestamp = "6/22/2026 9:30 AM";
        String soldTimestamp = "6/22/2026 9:44 AM";
        String externalKey = String.join("|",
                String.valueOf(fixture.account().getId()),
                "MESU6",
                boughtTimestamp,
                soldTimestamp,
                "1",
                "5485.00",
                "5477.50",
                "-76.25"
        );

        List<TradeResponse> imported = tradeService.importTrades(fixture.user(), List.of(new TradeImportRequest(
                fixture.account().getId(),
                "tradovate.csv",
                externalKey,
                "MESU6",
                "FUTURES",
                LocalDate.of(2026, 6, 22),
                LocalDateTime.of(2026, 6, 22, 9, 30),
                LocalDateTime.of(2026, 6, 22, 9, 44),
                "UNKNOWN",
                SetupType.OTHER,
                SessionType.NY_AM,
                new BigDecimal("5485.00"),
                new BigDecimal("5477.50"),
                1,
                new BigDecimal("-76.25"),
                new BigDecimal("-0.76"),
                OutcomeType.LOSS
        )));

        assertThat(imported).hasSize(1);
        assertThat(imported.get(0).getSide()).isEqualTo(Side.LONG);

        SessionReviewResponse review = sessionReviewService.getSessionReview(fixture.user(), fixture.account().getId(), LocalDate.of(2026, 6, 22));
        assertThat(review.getTimelineTrades()).first()
                .satisfies(trade -> {
                    assertThat(trade.getSymbol()).isEqualTo("MESU6");
                    assertThat(trade.getSide()).isEqualTo("LONG");
                });
        assertThat(review.getWorstTrade()).isNotNull();
        assertThat(review.getWorstTrade().getSide()).isEqualTo(Side.LONG);
        assertThat(review.getReplayInsights().getLargestLoser()).isNotNull();
        assertThat(review.getReplayInsights().getLargestLoser().getSide()).isEqualTo("LONG");
        assertThat(review.getSessionStory()).contains("MESU6 LONG");
        assertThat(review.getSessionStory()).doesNotContain("MESU6 UNKNOWN");
        assertThat(review.getSessionStory()).doesNotContain("MESU6 SHORT");
    }

    @Test
    void tradovateDirectionRepairKeepsShortLoserFromOpeningSellExecution() {
        Fixture fixture = fixture("repair-short-loser");
        staleImportedTrade(
                fixture,
                "MESU6",
                "SHORT",
                "-161.25",
                "6/16/2026 10:14:00 AM",
                "6/16/2026 10:02:00 AM",
                "5480.25",
                "5477.00"
        );

        var repairResult = tradeService.repairTradovateDirections(fixture.user(), fixture.account().getId(), LocalDate.of(2026, 6, 16));
        assertThat(repairResult.get("updated")).isEqualTo(1);

        SessionReviewResponse review = sessionReviewService.getSessionReview(fixture.user(), fixture.account().getId(), LocalDate.of(2026, 6, 16));
        assertThat(review.getTimelineTrades()).first()
                .satisfies(trade -> {
                    assertThat(trade.getSymbol()).isEqualTo("MESU6");
                    assertThat(trade.getSide()).isEqualTo("SHORT");
                    assertThat(trade.getEntryTime()).isBefore(trade.getExitTime());
                });
    }

    @Test
    void allWinningTradesHaveBestTradeAndNoWorstTradeOrWorstHour() {
        Fixture fixture = fixture("all-winning");
        trade(fixture, "MESH6", "LONG", "125.00", 9);
        trade(fixture, "NQM6", "LONG", "300.00", 10);

        SessionReviewResponse review = sessionReviewService.getSessionReview(fixture.user(), fixture.account().getId(), SESSION_DATE);

        assertTrade(review.getBestTrade(), "NQM6", "300.00");
        assertThat(review.getWorstTrade()).isNull();
        assertThat(review.getSessionStory()).contains("The strongest trade was NQM6 LONG");
        assertThat(review.getSessionStory()).doesNotContain("largest damage");
        assertThat(review.getReplayInsights().getBestTradingHour()).isEqualTo(10);
        assertThat(review.getReplayInsights().getWorstTradingHour()).isNull();
        assertThat(review.getReplayInsights().getWorstTradingHourPnl()).isNull();
    }

    @Test
    void generateSessionReviewUsesCachedAiReviewForIdenticalInput() {
        Fixture fixture = fixture("generate-cache");
        trade(fixture, "MESM6", "SHORT", "-140.00", 14);

        SessionReviewResponse first = sessionReviewService.generateSessionReview(fixture.user(), fixture.account().getId(), SESSION_DATE, false);
        SessionReviewResponse second = sessionReviewService.generateSessionReview(fixture.user(), fixture.account().getId(), SESSION_DATE, false);

        assertThat(first.getAiReviewCached()).isFalse();
        assertThat(second.getAiReviewCached()).isTrue();
        assertThat(second.getAiSessionReview()).isNotNull();
        assertThat(aiUsageEventRepository.countByUserIdAndUsageTypeAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                fixture.user().getId(),
                UsageType.SESSION_REVIEW,
                LocalDateTime.now().minusMinutes(5),
                LocalDateTime.now().plusMinutes(5)
        )).isEqualTo(1);
        waitForCacheMetric("session-review-cache-hit", true, 1);
        waitForCacheMetric("session-review-cache-miss", false, 1);
    }

    @Test
    void generateSessionReviewWithNoTradesSkipsAiAndCredits() {
        Fixture fixture = fixture("generate-no-trades");

        SessionReviewResponse review = sessionReviewService.generateSessionReview(fixture.user(), fixture.account().getId(), SESSION_DATE, true);
        SessionReviewResponse cachedRead = sessionReviewService.getSessionReview(fixture.user(), fixture.account().getId(), SESSION_DATE);

        assertThat(review.getNoTrades()).isTrue();
        assertThat(review.getAiGenerated()).isFalse();
        assertThat(review.getCreditsConsumed()).isZero();
        assertThat(review.getAiSessionReview()).isNull();
        assertThat(cachedRead.getNoTrades()).isTrue();
        assertThat(cachedRead.getAiGenerated()).isFalse();
        assertThat(aiUsageEventRepository.countByUserIdAndUsageTypeAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                fixture.user().getId(),
                UsageType.SESSION_REVIEW,
                LocalDateTime.now().minusMinutes(5),
                LocalDateTime.now().plusMinutes(5)
        )).isZero();
    }

    private void waitForCacheMetric(String cacheName, boolean hit, long minimumCount) {
        for (int i = 0; i < 20; i++) {
            if (cacheMetricRepository.countByCacheNameAndHit(cacheName, hit) >= minimumCount) {
                return;
            }
            try {
                Thread.sleep(50);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        assertThat(cacheMetricRepository.countByCacheNameAndHit(cacheName, hit)).isGreaterThanOrEqualTo(minimumCount);
    }

    private Fixture fixture(String name) {
        int id = SEQUENCE.incrementAndGet();
        User user = userRepository.save(User.builder()
                .name("Session Review " + name)
                .email("session-review-" + name + "-" + id + "@example.com")
                .passwordHash("test-password-hash")
                .emailVerified(true)
                .role(UserRole.USER)
                .plan(PlanType.PRO)
                .subscriptionStatus(SubscriptionStatus.ACTIVE)
                .build());
        TradingAccount account = tradingAccountRepository.save(TradingAccount.builder()
                .user(user)
                .name("Session Review Account " + name)
                .propFirm(PropFirmType.TOPSTEP)
                .accountType(TradingAccountType.PERSONAL)
                .startingBalance(BigDecimal.valueOf(50_000))
                .status(TradingAccountStatus.ACTIVE)
                .build());
        return new Fixture(user, account);
    }

    private void trade(Fixture fixture, String symbol, String side, String pnl, int entryHour) {
        BigDecimal pnlAmount = new BigDecimal(pnl);
        LocalDateTime entryTime = SESSION_DATE.atTime(entryHour, 0);
        tradeRepository.save(Trade.builder()
                .user(fixture.user())
                .account(fixture.account())
                .symbol(symbol)
                .market("FUTURES")
                .tradeDate(SESSION_DATE)
                .entryTime(entryTime)
                .exitTime(entryTime.plusMinutes(5))
                .side(side)
                .setup(SetupType.OTHER)
                .session(SessionType.NY_PM)
                .entryPrice(BigDecimal.valueOf(100))
                .exitPrice(BigDecimal.valueOf(101))
                .quantity(1)
                .riskAmount(BigDecimal.valueOf(100))
                .pnlAmount(pnlAmount)
                .pnlR(pnlAmount.divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP))
                .outcome(outcome(pnlAmount))
                .followedPlan(true)
                .externalTradeKey(fixture.user().getId() + "-" + symbol + "-" + entryHour + "-" + pnl)
                .build());
    }

    private void staleImportedTrade(
            Fixture fixture,
            String symbol,
            String storedSide,
            String pnl,
            String boughtTimestamp,
            String soldTimestamp,
            String buyPrice,
            String sellPrice
    ) {
        BigDecimal pnlAmount = new BigDecimal(pnl);
        LocalDateTime boughtAt = parseFixtureTimestamp(boughtTimestamp);
        LocalDateTime soldAt = parseFixtureTimestamp(soldTimestamp);
        tradeRepository.save(Trade.builder()
                .user(fixture.user())
                .account(fixture.account())
                .symbol(symbol)
                .market("FUTURES")
                .tradeDate(soldAt.toLocalDate())
                .entryTime(boughtAt)
                .exitTime(soldAt)
                .side(storedSide)
                .setup(SetupType.OTHER)
                .session(SessionType.NY_PM)
                .entryPrice(new BigDecimal(buyPrice))
                .exitPrice(new BigDecimal(sellPrice))
                .quantity(1)
                .riskAmount(BigDecimal.valueOf(100))
                .pnlAmount(pnlAmount)
                .pnlR(pnlAmount.divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP))
                .outcome(outcome(pnlAmount))
                .followedPlan(true)
                .externalTradeKey(String.join("|",
                        String.valueOf(fixture.account().getId()),
                        symbol,
                        boughtTimestamp,
                        soldTimestamp,
                        "1",
                        buyPrice,
                        sellPrice,
                        pnl
                ))
                .build());
    }

    private LocalDateTime parseFixtureTimestamp(String timestamp) {
        for (String pattern : List.of("M/d/yyyy h:mm:ss a", "M/d/yyyy h:mm a")) {
            try {
                return LocalDateTime.parse(
                        timestamp,
                        new java.time.format.DateTimeFormatterBuilder()
                                .parseCaseInsensitive()
                                .appendPattern(pattern)
                                .toFormatter(java.util.Locale.US)
                );
            } catch (Exception ignored) {
                // Try the next fixture shape.
            }
        }
        throw new IllegalArgumentException("Unsupported fixture timestamp: " + timestamp);
    }

    private OutcomeType outcome(BigDecimal pnl) {
        if (pnl.compareTo(BigDecimal.ZERO) > 0) return OutcomeType.WIN;
        if (pnl.compareTo(BigDecimal.ZERO) < 0) return OutcomeType.LOSS;
        return OutcomeType.BE;
    }

    private void assertTrade(TradeResponse trade, String symbol, String pnl) {
        assertThat(trade).isNotNull();
        assertThat(trade.getSymbol()).isEqualTo(symbol);
        assertThat(trade.getPnlAmount()).isEqualByComparingTo(pnl);
    }

    private record Fixture(User user, TradingAccount account) {
    }
}
