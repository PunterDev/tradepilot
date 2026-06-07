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
import com.tradepilot.observability.CacheMetricRepository;
import com.tradepilot.repository.UserRepository;
import com.tradepilot.service.AiTradeReviewService;
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
    @Autowired TradingDnaService tradingDnaService;
    @Autowired UsageLimitService usageLimitService;
    @Autowired AiUsageEventRepository usageEventRepository;
    @Autowired CacheMetricRepository cacheMetricRepository;

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
        User user = createUser("coach-cache@example.com", PlanType.FREE);
        TradingAccountResponse account = createAccount(user, "Coach");
        LocalDate date = LocalDate.of(2026, 5, 27);

        aiCoachService.generateDailyCoach(user, coachRequest(account.getId(), date, false));
        aiCoachService.getDailyCoach(user, account.getId(), date);

        assertThat(usageLimitService.getUsage(user).getUsageThisMonth().get("DAILY_COACH")).isEqualTo(1);

        aiCoachService.generateDailyCoach(user, coachRequest(account.getId(), date, true));

        assertThat(usageLimitService.getUsage(user).getUsageThisMonth().get("DAILY_COACH")).isEqualTo(2);
    }

    @Test
    void freshAiTradeReviewCountsUsage() {
        User user = createUser("ai-review-count@example.com", PlanType.FREE);
        TradingAccountResponse account = createAccount(user, "AI");
        TradeResponse trade = tradeService.importTrades(user, importTrades(account.getId(), 1, "review")).get(0);

        aiTradeReviewService.reviewTrade(user, reviewRequest(trade));

        assertThat(usageLimitService.getUsage(user).getUsageThisMonth().get("AI_TRADE_REVIEW")).isEqualTo(1);
    }

    @Test
    void freeTradingDnaLimitIsEnforcedAndCachedDnaDoesNotCount() {
        User user = createUser("dna-limit@example.com", PlanType.FREE);
        TradingAccountResponse account = createAccount(user, "DNA");
        tradeService.importTrades(user, importTrades(account.getId(), 1, "dna"));
        long cacheHitsBefore = cacheMetricRepository.countByCacheNameAndHit("trading-dna-cache-hit", true);
        long cacheMissesBefore = cacheMetricRepository.countByCacheNameAndHit("trading-dna-cache-miss", false);

        TradingDnaResponse freshResponse = tradingDnaService.getTradingDna(user, account.getId(), 30, false, false);
        TradingDnaResponse cachedResponse = tradingDnaService.getTradingDna(user, account.getId(), 30, false, false);

        assertThat(freshResponse.getCached()).isFalse();
        assertThat(cachedResponse.getCached()).isTrue();
        waitForCacheMetric("trading-dna-cache-miss", false, cacheMissesBefore + 1);
        waitForCacheMetric("trading-dna-cache-hit", true, cacheHitsBefore + 1);
        assertThat(usageLimitService.getUsage(user).getUsageThisMonth().get("TRADING_DNA")).isEqualTo(1);
        TradingDnaResponse limitResponse = tradingDnaService.getTradingDna(user, account.getId(), 30, false, true);
        assertThat(limitResponse.getLimitReached()).isTrue();
        assertThat(limitResponse.getCached()).isTrue();
        assertThat(limitResponse.getMessage()).contains("Monthly Trading DNA limit reached");
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
        usageEventRepository.save(AiUsageEvent.builder()
                .userId(user.getId())
                .usageType(UsageType.TRADING_DNA)
                .createdAt(LocalDateTime.now().minusMonths(1))
                .build());

        BillingUsageResponse usage = usageLimitService.getUsage(user);

        assertThat(usage.getUsageThisMonth().get("TRADING_DNA")).isZero();
        assertThat(usage.getRemaining().get("TRADING_DNA")).isEqualTo(1);
    }

    @Test
    void billingUsageUsesShortTtlCacheUntilInvalidated() {
        User user = createUser("billing-cache@example.com", PlanType.FREE);

        BillingUsageResponse first = usageLimitService.getUsage(user);
        usageEventRepository.save(AiUsageEvent.builder()
                .userId(user.getId())
                .usageType(UsageType.SESSION_REVIEW)
                .createdAt(LocalDateTime.now())
                .build());
        BillingUsageResponse cached = usageLimitService.getUsage(user);
        usageLimitService.invalidateUsageCache(user.getId());
        BillingUsageResponse refreshed = usageLimitService.getUsage(user);

        assertThat(first.getUsageThisMonth().get("SESSION_REVIEW")).isZero();
        assertThat(cached.getUsageThisMonth().get("SESSION_REVIEW")).isZero();
        assertThat(refreshed.getUsageThisMonth().get("SESSION_REVIEW")).isEqualTo(1);
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
