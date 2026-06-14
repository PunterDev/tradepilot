package com.tradepilot;

import com.tradepilot.billing.*;
import com.tradepilot.coach.dto.CoachGenerateRequest;
import com.tradepilot.coach.service.AiCoachService;
import com.tradepilot.dto.account.TradingAccountRequest;
import com.tradepilot.dto.account.TradingAccountResponse;
import com.tradepilot.dto.ai.AiTradeReviewRequest;
import com.tradepilot.dto.ai.TradeSnapshot;
import com.tradepilot.dto.trade.TradeImportRequest;
import com.tradepilot.dto.trade.TradeResponse;
import com.tradepilot.entity.User;
import com.tradepilot.enums.*;
import com.tradepilot.exception.ResourceNotFoundException;
import com.tradepilot.leakdetector.dto.LeakDetectorResponse;
import com.tradepilot.leakdetector.dto.LeakResultDto;
import com.tradepilot.leakdetector.dto.LeakSeverity;
import com.tradepilot.leakdetector.dto.LeakType;
import com.tradepilot.leakdetector.entity.AiLeakDetectorSnapshot;
import com.tradepilot.leakdetector.repository.AiLeakDetectorSnapshotRepository;
import com.tradepilot.leakdetector.service.LeakDetectorService;
import com.tradepilot.observability.CacheMetricRepository;
import com.tradepilot.repository.UserRepository;
import com.tradepilot.service.AiTradeReviewService;
import com.tradepilot.service.SessionReviewService;
import com.tradepilot.service.TradeService;
import com.tradepilot.service.TradingAccountService;
import com.tradepilot.tradingdna.dto.TradingDnaResponse;
import com.tradepilot.tradingdna.service.TradingDnaService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class BillingLimitTests {
    @Autowired UserRepository userRepository;
    @Autowired TradingAccountService tradingAccountService;
    @Autowired TradeService tradeService;
    @Autowired AiCoachService aiCoachService;
    @Autowired AiTradeReviewService aiTradeReviewService;
    @Autowired SessionReviewService sessionReviewService;
    @Autowired TradingDnaService tradingDnaService;
    @Autowired LeakDetectorService leakDetectorService;
    @Autowired UsageLimitService usageLimitService;
    @Autowired AiUsageEventRepository usageEventRepository;
    @Autowired AiCreditUsageRepository creditUsageRepository;
    @Autowired CacheMetricRepository cacheMetricRepository;
    @Autowired AiLeakDetectorSnapshotRepository leakDetectorSnapshotRepository;

    @Test
    void freeUsersCanCreateOnlyOneAccount() {
        User user = createUser("free-account-limit@example.com", PlanType.FREE);
        createAccount(user, "First");

        assertThatThrownBy(() -> createAccount(user, "Second"))
                .isInstanceOf(PlanLimitExceededException.class)
                .hasMessageContaining("trading accounts");
    }

    @Test
    void freeUsersCannotImportMoreThanOneHundredTradesTotal() {
        User user = createUser("free-trade-limit@example.com", PlanType.FREE);
        TradingAccountResponse account = createAccount(user, "Free");

        assertThatThrownBy(() -> tradeService.importTrades(user, importTrades(account.getId(), 101, "too-many")))
                .isInstanceOf(PlanLimitExceededException.class)
                .hasMessageContaining("imported trades");
    }

    @Test
    void cachedCoachDoesNotCountUsageButForceRefreshDoes() {
        User user = createUser("coach-cache@example.com", PlanType.PRO);
        TradingAccountResponse account = createAccount(user, "Coach");
        LocalDate date = LocalDate.of(2026, 5, 27);

        aiCoachService.generateDailyCoach(user, coachRequest(account.getId(), date, false));
        aiCoachService.getDailyCoach(user, account.getId(), date);

        assertThat(usageLimitService.getUsage(user).getUsageThisMonth().get("DAILY_COACH")).isEqualTo(2);

        aiCoachService.generateDailyCoach(user, coachRequest(account.getId(), date, true));

        assertThat(usageLimitService.getUsage(user).getUsageThisMonth().get("DAILY_COACH")).isEqualTo(4);
    }

    @Test
    void freshAiTradeReviewCountsUsage() {
        User user = createUser("ai-review-count@example.com", PlanType.FREE);
        TradingAccountResponse account = createAccount(user, "AI");
        TradeResponse trade = tradeService.importTrades(user, importTrades(account.getId(), 1, "review")).get(0);

        aiTradeReviewService.reviewTrade(user, reviewRequest(trade));

        assertThat(usageLimitService.getUsage(user).getUsageThisMonth().get("AI_TRADE_REVIEW")).isEqualTo(1);
        assertThat(usageLimitService.getUsage(user).getUsageThisMonth().get("AI_CREDITS")).isEqualTo(1);
        assertThat(usageLimitService.getUsage(user).getRemaining().get("AI_CREDITS")).isEqualTo(9);
    }

    @Test
    void freeUsersAreBlockedFromTradingDna() {
        User user = createUser("dna-limit@example.com", PlanType.FREE);
        TradingAccountResponse account = createAccount(user, "DNA");
        tradeService.importTrades(user, importTrades(account.getId(), 1, "dna"));

        assertThatThrownBy(() -> tradingDnaService.getTradingDna(user, account.getId(), 30, false, false))
                .isInstanceOf(FeatureAccessException.class)
                .hasMessageContaining("Trading DNA");
    }

    @Test
    void freeUsersAreBlockedFromSessionReview() {
        User user = createUser("free-session-block@example.com", PlanType.FREE);
        TradingAccountResponse account = createAccount(user, "Session");

        assertThatThrownBy(() -> sessionReviewService.getSessionReview(user, account.getId(), LocalDate.of(2026, 5, 27)))
                .isInstanceOf(FeatureAccessException.class)
                .hasMessageContaining("Session Review");
    }

    @Test
    void proUsersCanAccessSessionReview() {
        User user = createUser("pro-session-access@example.com", PlanType.PRO);
        TradingAccountResponse account = createAccount(user, "Session");

        assertThat(sessionReviewService.getSessionReview(user, account.getId(), LocalDate.of(2026, 5, 27))).isNotNull();
    }

    @Test
    void proTradingDnaConsumesCreditsAndCachedDnaDoesNotCount() {
        User user = createUser("dna-pro-cache@example.com", PlanType.PRO);
        TradingAccountResponse account = createAccount(user, "DNA");
        tradeService.importTrades(user, importTrades(account.getId(), 1, "dna-pro"));
        long cacheHitsBefore = cacheMetricRepository.countByCacheNameAndHit("trading-dna-cache-hit", true);
        long cacheMissesBefore = cacheMetricRepository.countByCacheNameAndHit("trading-dna-cache-miss", false);

        TradingDnaResponse freshResponse = tradingDnaService.getTradingDna(user, account.getId(), 30, false, false);
        TradingDnaResponse cachedResponse = tradingDnaService.getTradingDna(user, account.getId(), 30, false, false);

        assertThat(freshResponse.getCached()).isFalse();
        assertThat(cachedResponse.getCached()).isTrue();
        waitForCacheMetric("trading-dna-cache-miss", false, cacheMissesBefore + 1);
        waitForCacheMetric("trading-dna-cache-hit", true, cacheHitsBefore + 1);
        assertThat(usageLimitService.getUsage(user).getUsageThisMonth().get("TRADING_DNA")).isEqualTo(3);
        assertThat(usageLimitService.getUsage(user).getUsageThisMonth().get("AI_CREDITS")).isEqualTo(3);
    }

    @Test
    void freeUsersAreBlockedFromLeakDetector() {
        User user = createUser("free-leak-block@example.com", PlanType.FREE);
        TradingAccountResponse account = createAccount(user, "Leaks");

        assertThatThrownBy(() -> leakDetectorService.getLeakDetector(user, account.getId()))
                .isInstanceOf(FeatureAccessException.class)
                .hasMessageContaining("AI Leak Detector");
    }

    @Test
    void proLeakDetectorGeneratesAndConsumesCredits() {
        User user = createUser("pro-leak-generate@example.com", PlanType.PRO);
        TradingAccountResponse account = createAccount(user, "Leaks");
        tradeService.importTrades(user, leakTrades(account.getId(), "leak-generate"));

        LeakDetectorResponse response = leakDetectorService.getLeakDetector(user, account.getId());

        assertThat(response.getCached()).isFalse();
        assertThat(response.getTotalTrades()).isEqualTo(24);
        assertThat(response.getLeaks()).isNotEmpty();
        assertThat(usageLimitService.getUsage(user).getUsageThisMonth().get("AI_LEAK_DETECTOR")).isEqualTo(3);
        assertThat(usageLimitService.getUsage(user).getUsageThisMonth().get("AI_CREDITS")).isEqualTo(3);
    }

    @Test
    void leakDetectorKeepsDayOfWeekTitleWithoutEnumSwitches() {
        User user = createUser("pro-leak-day-title@example.com", PlanType.PRO);
        TradingAccountResponse account = createAccount(user, "Day Title");
        tradeService.importTrades(user, dayOfWeekTitleTrades(account.getId(), "leak-day-title"));

        LeakDetectorResponse response = leakDetectorService.getLeakDetector(user, account.getId());

        assertThat(response.getLeaks()).anySatisfy(leak -> {
            assertThat(leak.getLeakType()).isEqualTo(LeakType.DAY_OF_WEEK);
            assertThat(leak.getTitle()).isEqualTo("Friday Performance Leak");
        });
    }

    @Test
    void leakDetectorInsufficientCreditsReturnsLimitException() {
        User user = createUser("pro-leak-credit-limit@example.com", PlanType.PRO);
        TradingAccountResponse account = createAccount(user, "Leaks");
        tradeService.importTrades(user, leakTrades(account.getId(), "leak-credit-limit"));
        creditUsageRepository.save(AiCreditUsage.builder()
                .userId(user.getId())
                .feature(Feature.TRADING_DNA)
                .credits(98)
                .createdAt(LocalDateTime.now())
                .build());

        assertThatThrownBy(() -> leakDetectorService.getLeakDetector(user, account.getId()))
                .isInstanceOf(AiCreditLimitExceededException.class)
                .hasMessageContaining("AI credits");
    }

    @Test
    void cachedLeakDetectorDoesNotConsumeCreditsButRegenerateDoes() {
        User user = createUser("pro-leak-cache@example.com", PlanType.PRO);
        TradingAccountResponse account = createAccount(user, "Leaks");
        tradeService.importTrades(user, leakTrades(account.getId(), "leak-cache"));

        LeakDetectorResponse fresh = leakDetectorService.getLeakDetector(user, account.getId());
        LeakDetectorResponse cached = leakDetectorService.getLeakDetector(user, account.getId());

        assertThat(fresh.getCached()).isFalse();
        assertThat(cached.getCached()).isTrue();
        assertThat(usageLimitService.getUsage(user).getUsageThisMonth().get("AI_LEAK_DETECTOR")).isEqualTo(3);

        leakDetectorService.regenerate(user, account.getId());

        assertThat(usageLimitService.getUsage(user).getUsageThisMonth().get("AI_LEAK_DETECTOR")).isEqualTo(6);
    }

    @Test
    void leakConfidenceIncreasesWithSampleSizeAndSmallSamplesAreCapped() {
        int small = leakDetectorService.confidenceScore(3, new BigDecimal("-0.9"), 0.9, new BigDecimal("-2.7"));
        int large = leakDetectorService.confidenceScore(30, new BigDecimal("-0.9"), 0.9, new BigDecimal("-27"));

        assertThat(small).isLessThanOrEqualTo(45);
        assertThat(large).isGreaterThan(small);
    }

    @Test
    void leakDetectorImpactCalculationIsDeterministic() {
        User user = createUser("pro-leak-impact@example.com", PlanType.PRO);
        TradingAccountResponse account = createAccount(user, "Leaks");
        tradeService.importTrades(user, leakTrades(account.getId(), "leak-impact"));

        LeakDetectorResponse first = leakDetectorService.getLeakDetector(user, account.getId());
        LeakDetectorResponse second = leakDetectorService.getLeakDetector(user, account.getId());

        assertThat(first.getLeaks()).isNotEmpty();
        assertThat(first.getLeaks().get(0).getEstimatedMonthlyImpact())
                .isEqualByComparingTo(second.getLeaks().get(0).getEstimatedMonthlyImpact());
    }

    @Test
    void leakDetectorDoesNotLabelDollarPnlAsRWithoutValidRiskData() {
        User user = createUser("pro-leak-dollar-only@example.com", PlanType.PRO);
        TradingAccountResponse account = createAccount(user, "Dollar Leaks");
        tradeService.importTrades(user, dollarOnlyLeakTrades(account.getId(), "dollar-only"));

        LeakDetectorResponse response = leakDetectorService.getLeakDetector(user, account.getId());

        assertThat(response.getLeaks()).isNotEmpty();
        assertThat(response.getLeaks()).allSatisfy(leak -> {
            assertThat(leak.getTotalPnl()).isNotNull();
            assertThat(leak.getEstimatedMonthlyImpact()).isNotNull();
            assertThat(leak.getHasValidRMultiple()).isFalse();
            assertThat(leak.getTotalR()).isNull();
            assertThat(leak.getEstimatedMonthlyImpactR()).isNull();
        });
    }

    @Test
    void leakDetectorDeduplicatesOverlappingLeakCardsAndBuildsSummary() {
        User user = createUser("pro-leak-dedupe@example.com", PlanType.PRO);
        TradingAccountResponse account = createAccount(user, "Dedupe Leaks");
        tradeService.importTrades(user, duplicateProneDollarTrades(account.getId(), "dedupe"));

        LeakDetectorResponse response = leakDetectorService.getLeakDetector(user, account.getId());

        assertThat(response.getLeaks()).isNotEmpty();
        assertThat(response.getBiggestLeakTitle()).isEqualTo(response.getLeaks().get(0).getTitle());
        assertThat(response.getBiggestLeakImpact()).isEqualByComparingTo(response.getLeaks().get(0).getEstimatedMonthlyImpact());
        assertThat(response.getHighestConfidence()).isNotNull();

        List<String> statSignatures = response.getLeaks().stream()
                .map(leak -> leak.getTradeCount() + "|" + leak.getTotalPnl() + "|" + leak.getAvgPnl())
                .toList();
        assertThat(statSignatures).doesNotHaveDuplicates();
    }

    @Test
    void largeLossLeakUsesOnlyThresholdLossesAndSmallSampleIsNotCritical() {
        User user = createUser("pro-leak-large-loss@example.com", PlanType.PRO);
        TradingAccountResponse account = createAccount(user, "Large Losses");
        tradeService.importTrades(user, largeLossDollarTrades(account.getId(), "large-loss"));

        LeakDetectorResponse response = leakDetectorService.getLeakDetector(user, account.getId());

        LeakResultDto largeLoss = response.getLeaks().stream()
                .filter(leak -> leak.getLeakType() == LeakType.LARGE_LOSS)
                .findFirst()
                .orElseThrow();
        assertThat(largeLoss.getTradeCount()).isEqualTo(3);
        assertThat(largeLoss.getSeverity()).isNotEqualTo(LeakSeverity.CRITICAL);
    }

    @Test
    void longShortBiasComparesSidesInsteadOfAllLosses() {
        User user = createUser("pro-leak-side-bias@example.com", PlanType.PRO);
        TradingAccountResponse account = createAccount(user, "Side Bias");
        tradeService.importTrades(user, sideBiasDollarTrades(account.getId(), "side-bias"));

        LeakDetectorResponse response = leakDetectorService.getLeakDetector(user, account.getId());

        LeakResultDto sideBias = response.getLeaks().stream()
                .filter(leak -> leak.getLeakType() == LeakType.LONG_SHORT_BIAS)
                .findFirst()
                .orElseThrow();
        assertThat(sideBias.getTitle()).isEqualTo("Short Side Bias Leak");
        assertThat(sideBias.getTradeCount()).isEqualTo(12);
        assertThat(sideBias.getTotalPnl()).isEqualByComparingTo(new BigDecimal("-1800.00"));
    }

    @Test
    void leakDetectorReturnsFullRelatedTradeCountWhenIdsAreCapped() {
        User user = createUser("pro-leak-related-count@example.com", PlanType.PRO);
        TradingAccountResponse account = createAccount(user, "Related Count");
        tradeService.importTrades(user, cappedRelatedTradeCountTrades(account.getId(), "related-count"));

        LeakDetectorResponse response = leakDetectorService.getLeakDetector(user, account.getId());

        LeakResultDto sideBias = response.getLeaks().stream()
                .filter(leak -> leak.getLeakType() == LeakType.LONG_SHORT_BIAS)
                .findFirst()
                .orElseThrow();
        assertThat(sideBias.getTradeCount()).isEqualTo(25);
        assertThat(sideBias.getRelatedTradeCount()).isEqualTo(25);
        assertThat(sideBias.getRelatedTradeIds()).hasSize(12);
    }

    @Test
    void cachedLeakDetectorBackfillsRelatedTradeCountForLegacySnapshots() {
        User user = createUser("pro-leak-legacy-related-count@example.com", PlanType.PRO);
        TradingAccountResponse account = createAccount(user, "Legacy Related Count");
        tradeService.importTrades(user, cappedRelatedTradeCountTrades(account.getId(), "legacy-related-count"));
        LeakDetectorResponse fresh = leakDetectorService.getLeakDetector(user, account.getId());
        AiLeakDetectorSnapshot snapshot = leakDetectorSnapshotRepository.findById(fresh.getId()).orElseThrow();
        snapshot.setResultJson(snapshot.getResultJson().replaceAll("\"relatedTradeCount\"\\s*:\\s*\\d+,?", ""));
        leakDetectorSnapshotRepository.save(snapshot);

        LeakDetectorResponse cached = leakDetectorService.getLeakDetector(user, account.getId());

        LeakResultDto sideBias = cached.getLeaks().stream()
                .filter(leak -> leak.getLeakType() == LeakType.LONG_SHORT_BIAS)
                .findFirst()
                .orElseThrow();
        assertThat(cached.getCached()).isTrue();
        assertThat(sideBias.getTradeCount()).isEqualTo(25);
        assertThat(sideBias.getRelatedTradeCount()).isEqualTo(25);
        assertThat(sideBias.getRelatedTradeIds()).hasSize(12);
    }

    @Test
    void leakDetectorEnforcesAccountOwnership() {
        User owner = createUser("leak-owner@example.com", PlanType.PRO);
        User other = createUser("leak-other@example.com", PlanType.PRO);
        TradingAccountResponse account = createAccount(owner, "Owned");

        assertThatThrownBy(() -> leakDetectorService.getLeakDetector(other, account.getId()))
                .isInstanceOf(ResourceNotFoundException.class);
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

    @Test
    void usageResetsByMonthlyPeriod() {
        User user = createUser("monthly-reset@example.com", PlanType.FREE);
        creditUsageRepository.save(AiCreditUsage.builder()
                .userId(user.getId())
                .feature(Feature.TRADING_DNA)
                .credits(3)
                .createdAt(LocalDateTime.now().minusMonths(1))
                .build());

        BillingUsageResponse usage = usageLimitService.getUsage(user);

        assertThat(usage.getUsageThisMonth().get("AI_CREDITS")).isZero();
        assertThat(usage.getRemaining().get("AI_CREDITS")).isEqualTo(10);
    }

    @Test
    void billingUsageUsesShortTtlCacheUntilInvalidated() {
        User user = createUser("billing-cache@example.com", PlanType.FREE);

        BillingUsageResponse first = usageLimitService.getUsage(user);
        creditUsageRepository.save(AiCreditUsage.builder()
                .userId(user.getId())
                .feature(Feature.SESSION_REVIEW)
                .credits(2)
                .createdAt(LocalDateTime.now())
                .build());
        BillingUsageResponse cached = usageLimitService.getUsage(user);
        usageLimitService.invalidateUsageCache(user.getId());
        BillingUsageResponse refreshed = usageLimitService.getUsage(user);

        assertThat(first.getUsageThisMonth().get("AI_CREDITS")).isZero();
        assertThat(cached.getUsageThisMonth().get("AI_CREDITS")).isZero();
        assertThat(refreshed.getUsageThisMonth().get("AI_CREDITS")).isEqualTo(2);
    }

    @Test
    void creditLimitIsEnforcedForFreeAiTradeReviews() {
        User user = createUser("free-credit-limit@example.com", PlanType.FREE);
        for (int i = 0; i < 10; i++) {
            creditUsageRepository.save(AiCreditUsage.builder()
                    .userId(user.getId())
                    .feature(Feature.AI_TRADE_REVIEW)
                    .credits(1)
                    .createdAt(LocalDateTime.now())
                    .build());
        }

        assertThatThrownBy(() -> usageLimitService.enforceAiUsage(user, UsageType.AI_TRADE_REVIEW))
                .isInstanceOf(AiCreditLimitExceededException.class)
                .hasMessageContaining("AI credits");
    }

    @Test
    void proUsersBypassFreeAccountAndTradeLimits() {
        User user = createUser("pro-bypass@example.com", PlanType.PRO);
        TradingAccountResponse first = createAccount(user, "Pro One");
        TradingAccountResponse second = createAccount(user, "Pro Two");

        assertThat(first.getId()).isNotNull();
        assertThat(second.getId()).isNotNull();
        assertThat(tradeService.importTrades(user, importTrades(first.getId(), 101, "pro"))).hasSize(101);
    }

    private User createUser(String email, PlanType plan) {
        return userRepository.save(User.builder()
                .name(email)
                .email(email)
                .passwordHash("test-password-hash")
                .emailVerified(true)
                .plan(plan)
                .subscriptionStatus(plan == PlanType.PRO ? SubscriptionStatus.ACTIVE : SubscriptionStatus.NONE)
                .build());
    }

    private TradingAccountResponse createAccount(User user, String name) {
        return tradingAccountService.create(
                user,
                new TradingAccountRequest(
                        name,
                        PropFirmType.TOPSTEP,
                        null,
                        null,
                        BigDecimal.valueOf(50_000),
                        TradingAccountStatus.ACTIVE
                )
        );
    }

    private List<TradeImportRequest> importTrades(Long accountId, int count, String keyPrefix) {
        return IntStream.range(0, count)
                .mapToObj(i -> importTrade(accountId, keyPrefix + "-" + i, BigDecimal.valueOf(i % 2 == 0 ? 100 : -50)))
                .toList();
    }

    private List<TradeImportRequest> leakTrades(Long accountId, String keyPrefix) {
        LocalDate start = LocalDate.of(2026, 5, 1);
        return IntStream.range(0, 24)
                .mapToObj(i -> {
                    LocalDate date = start.plusDays(i);
                    boolean friday = date.getDayOfWeek() == java.time.DayOfWeek.FRIDAY;
                    BigDecimal pnlR = friday ? new BigDecimal("-1.50") : new BigDecimal("0.35");
                    BigDecimal pnl = pnlR.multiply(BigDecimal.valueOf(100));
                    return new TradeImportRequest(
                            accountId,
                            "tradovate.csv",
                            keyPrefix + "-" + i,
                            "NQ",
                            "FUTURES",
                            date,
                            date.atTime(friday ? 13 : 9, 30),
                            date.atTime(friday ? 13 : 9, 45),
                            i % 2 == 0 ? "LONG" : "SHORT",
                            friday ? SetupType.OTHER : SetupType.ORB,
                            friday ? SessionType.NY_PM : SessionType.NY_AM,
                            BigDecimal.valueOf(18_000),
                            BigDecimal.valueOf(18_010),
                            1,
                            pnl,
                            pnlR,
                            pnlR.signum() >= 0 ? OutcomeType.WIN : OutcomeType.LOSS
                    );
                })
                .toList();
    }

    private List<TradeImportRequest> dollarOnlyLeakTrades(Long accountId, String keyPrefix) {
        LocalDate start = LocalDate.of(2026, 5, 1);
        return IntStream.range(0, 24)
                .mapToObj(i -> dollarTrade(
                        accountId,
                        keyPrefix + "-" + i,
                        start.plusDays(i),
                        i % 3 == 0 ? "SHORT" : "LONG",
                        i % 3 == 0 ? new BigDecimal("-650.00") : new BigDecimal("125.00"),
                        i % 3 == 0 ? SessionType.NY_PM : SessionType.NY_AM,
                        i % 3 == 0 ? SetupType.OTHER : SetupType.ORB
                ))
                .toList();
    }

    private List<TradeImportRequest> duplicateProneDollarTrades(Long accountId, String keyPrefix) {
        LocalDate start = LocalDate.of(2026, 5, 1);
        return IntStream.range(0, 24)
                .mapToObj(i -> dollarTrade(
                        accountId,
                        keyPrefix + "-" + i,
                        start.plusDays(i),
                        i < 12 ? "SHORT" : "LONG",
                        i < 12 ? new BigDecimal("-700.00") : new BigDecimal("120.00"),
                        i < 12 ? SessionType.NY_PM : SessionType.NY_AM,
                        i < 12 ? SetupType.OTHER : SetupType.ORB
                ))
                .toList();
    }

    private List<TradeImportRequest> largeLossDollarTrades(Long accountId, String keyPrefix) {
        LocalDate start = LocalDate.of(2026, 5, 1);
        return IntStream.range(0, 24)
                .mapToObj(i -> {
                    BigDecimal pnl = i < 3
                            ? new BigDecimal("-900.00")
                            : (i % 2 == 0 ? new BigDecimal("-100.00") : new BigDecimal("140.00"));
                    return dollarTrade(
                            accountId,
                            keyPrefix + "-" + i,
                            start.plusDays(i),
                            i % 2 == 0 ? "SHORT" : "LONG",
                            pnl,
                            i < 3 ? SessionType.NY_PM : SessionType.NY_AM,
                            i < 3 ? SetupType.OTHER : SetupType.ORB
                    );
                })
                .toList();
    }

    private List<TradeImportRequest> sideBiasDollarTrades(Long accountId, String keyPrefix) {
        LocalDate start = LocalDate.of(2026, 5, 1);
        return IntStream.range(0, 24)
                .mapToObj(i -> dollarTrade(
                        accountId,
                        keyPrefix + "-" + i,
                        start.plusDays(i),
                        i < 12 ? "SHORT" : "LONG",
                        i < 12 ? new BigDecimal("-150.00") : new BigDecimal("125.00"),
                        SessionType.NY_AM,
                        i % 2 == 0 ? SetupType.ORB : SetupType.FVG
                ))
                .toList();
    }

    private List<TradeImportRequest> cappedRelatedTradeCountTrades(Long accountId, String keyPrefix) {
        LocalDate start = LocalDate.of(2026, 5, 1);
        return IntStream.range(0, 40)
                .mapToObj(i -> dollarTrade(
                        accountId,
                        keyPrefix + "-" + i,
                        start.plusDays(i),
                        i < 25 ? "SHORT" : "LONG",
                        i < 25 ? new BigDecimal("-140.00") : new BigDecimal("120.00"),
                        SessionType.NY_AM,
                        i % 2 == 0 ? SetupType.ORB : SetupType.FVG
                ))
                .toList();
    }

    private List<TradeImportRequest> dayOfWeekTitleTrades(Long accountId, String keyPrefix) {
        LocalDate start = LocalDate.of(2026, 5, 1);
        return IntStream.range(0, 24)
                .mapToObj(i -> {
                    LocalDate date = start.plusDays(i);
                    boolean friday = date.getDayOfWeek() == java.time.DayOfWeek.FRIDAY;
                    return dollarTrade(
                            accountId,
                            keyPrefix + "-" + i,
                            date,
                            i % 2 == 0 ? "LONG" : "SHORT",
                            friday ? new BigDecimal("-200.00") : new BigDecimal("125.00"),
                            friday ? SessionType.NY_PM : SessionType.NY_AM,
                            i % 2 == 0 ? SetupType.ORB : SetupType.FVG
                    );
                })
                .toList();
    }

    private TradeImportRequest dollarTrade(
            Long accountId,
            String externalTradeKey,
            LocalDate date,
            String side,
            BigDecimal pnl,
            SessionType session,
            SetupType setup
    ) {
        return new TradeImportRequest(
                accountId,
                "tradovate.csv",
                externalTradeKey,
                "NQ",
                "FUTURES",
                date,
                date.atTime(session == SessionType.NY_PM ? 13 : 9, 30),
                date.atTime(session == SessionType.NY_PM ? 13 : 9, 45),
                side,
                setup,
                session,
                BigDecimal.valueOf(18_000),
                BigDecimal.valueOf(18_010),
                1,
                pnl,
                null,
                pnl.signum() >= 0 ? OutcomeType.WIN : OutcomeType.LOSS
        );
    }

    private TradeImportRequest importTrade(Long accountId, String externalTradeKey, BigDecimal pnl) {
        return new TradeImportRequest(
                accountId,
                "tradovate.csv",
                externalTradeKey,
                "NQ",
                "FUTURES",
                LocalDate.of(2026, 5, 15),
                LocalDateTime.of(2026, 5, 15, 9, 30),
                LocalDateTime.of(2026, 5, 15, 9, 45),
                "LONG",
                SetupType.ORB,
                SessionType.NY_AM,
                BigDecimal.valueOf(18_000),
                BigDecimal.valueOf(18_010),
                1,
                pnl,
                BigDecimal.ONE,
                pnl.signum() >= 0 ? OutcomeType.WIN : OutcomeType.LOSS
        );
    }

    private CoachGenerateRequest coachRequest(Long accountId, LocalDate date, boolean forceRefresh) {
        CoachGenerateRequest request = new CoachGenerateRequest();
        request.setAccountId(accountId);
        request.setPeriodStart(date);
        request.setPeriodEnd(date);
        request.setForceRefresh(forceRefresh);
        return request;
    }

    private AiTradeReviewRequest reviewRequest(TradeResponse trade) {
        TradeSnapshot snapshot = new TradeSnapshot();
        snapshot.setId(trade.getId());
        snapshot.setSymbol(trade.getSymbol());
        snapshot.setSide(trade.getSide().name());
        snapshot.setPnl(trade.getPnlAmount().doubleValue());
        snapshot.setQuantity(trade.getQuantity());
        snapshot.setEntryPrice(trade.getEntryPrice().doubleValue());
        snapshot.setExitPrice(trade.getExitPrice().doubleValue());

        AiTradeReviewRequest request = new AiTradeReviewRequest();
        request.setTradeId(trade.getId());
        request.setTrade(snapshot);
        request.setJournalText("Stayed patient.");
        return request;
    }
}
