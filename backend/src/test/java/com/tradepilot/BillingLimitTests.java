package com.tradepilot;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradepilot.billing.*;
import com.tradepilot.coach.dto.CoachDeterministicSummary;
import com.tradepilot.coach.dto.CoachGenerateRequest;
import com.tradepilot.coach.dto.CoachSnapshotDto;
import com.tradepilot.coach.entity.CoachSnapshotStatus;
import com.tradepilot.coach.service.AiCoachService;
import com.tradepilot.dto.admin.AdminTradeCleanupResponse;
import com.tradepilot.dto.account.TradingAccountRequest;
import com.tradepilot.dto.account.TradingAccountResponse;
import com.tradepilot.dto.ai.AiTradeReviewRequest;
import com.tradepilot.dto.ai.TradeSnapshot;
import com.tradepilot.dto.session.SessionReplayInsightsResponse;
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
import com.tradepilot.repository.TradeRepository;
import com.tradepilot.repository.UserRepository;
import com.tradepilot.service.AiTradeReviewService;
import com.tradepilot.service.SessionReviewService;
import com.tradepilot.service.TradeService;
import com.tradepilot.service.TradingAccountService;
import com.tradepilot.tradingdna.dto.TradingDnaContext;
import com.tradepilot.tradingdna.dto.TradingDnaResponse;
import com.tradepilot.tradingdna.service.TradingDnaService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class BillingLimitTests {
    @Autowired ObjectMapper objectMapper;
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
    @Autowired TradeRepository tradeRepository;

    @Test
    void leakResultDtoSerializesWithoutLombokBuilderRuntimeDependency() throws Exception {
        LeakResultDto result = LeakResultDto.of(
                LeakType.LONG_SHORT_BIAS,
                "Short Side Bias Leak",
                "Short trades are dragging performance.",
                "Reduce short-side size until confirmation improves.",
                33,
                new BigDecimal("0.00"),
                new BigDecimal("-2933.75"),
                new BigDecimal("-88.90"),
                new BigDecimal("-1257.33"),
                null,
                null,
                null,
                false,
                76,
                LeakSeverity.HIGH,
                "Reduce size on short trades.",
                33,
                List.of(1L, 2L, 3L),
                LocalDateTime.of(2026, 6, 22, 9, 30)
        );

        JsonNode json = objectMapper.readTree(objectMapper.writeValueAsString(result));

        assertThat(json.get("leakType").asText()).isEqualTo("LONG_SHORT_BIAS");
        assertThat(json.get("title").asText()).isEqualTo("Short Side Bias Leak");
        assertThat(json.get("relatedTradeCount").asInt()).isEqualTo(33);
        assertThat(json.get("hasValidRMultiple").asBoolean()).isFalse();
    }

    @Test
    void leakDetectorResponseSerializesWithoutLombokBuilderRuntimeDependency() throws Exception {
        User user = createUser("pro-leak-response-runtime@example.com", PlanType.PRO);
        TradingAccountResponse account = createAccount(user, "Leak Response Runtime");
        tradeService.importTrades(user, leakTrades(account.getId(), "leak-response-runtime"));

        LeakDetectorResponse fresh = leakDetectorService.getLeakDetector(user, account.getId());
        LeakDetectorResponse regenerated = leakDetectorService.regenerate(user, account.getId());

        JsonNode freshJson = objectMapper.readTree(objectMapper.writeValueAsString(fresh));
        JsonNode regeneratedJson = objectMapper.readTree(objectMapper.writeValueAsString(regenerated));

        assertThat(freshJson.get("accountId").asLong()).isEqualTo(account.getId());
        assertThat(freshJson.get("leaks")).isNotEmpty();
        assertThat(regeneratedJson.get("accountId").asLong()).isEqualTo(account.getId());
        assertThat(regeneratedJson.get("leaks")).isNotEmpty();
    }

    @Test
    void launchCriticalRuntimeDtosDoNotExposeLombokBuilderMethods() {
        List<Class<?>> dtoClasses = List.of(
                LeakDetectorResponse.class,
                LeakResultDto.class,
                TradingDnaContext.class,
                SessionReplayInsightsResponse.class,
                CoachDeterministicSummary.class,
                AdminTradeCleanupResponse.class
        );

        for (Class<?> dtoClass : dtoClasses) {
            assertThat(Arrays.stream(dtoClass.getDeclaredMethods()).map(method -> method.getName()))
                    .as(dtoClass.getSimpleName())
                    .doesNotContain("builder");
        }
    }

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
    void oversizedCsvImportIsRejectedBeforeProcessingRows() {
        User user = createUser("oversized-import@example.com", PlanType.PRO);
        TradingAccountResponse account = createAccount(user, "Oversized");

        assertThatThrownBy(() -> tradeService.importTrades(user, importTrades(account.getId(), 5001, "oversized")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("5000 rows");
    }

    @Test
    void cachedCoachDoesNotCountUsageButForceRefreshDoes() {
        User user = createUser("coach-cache@example.com", PlanType.PRO);
        TradingAccountResponse account = createAccount(user, "Coach");
        LocalDate date = LocalDate.of(2026, 5, 27);
        tradeService.importTrades(user, List.of(dollarTrade(
                account.getId(),
                "coach-cache-trade",
                date,
                "LONG",
                new BigDecimal("125.00"),
                SessionType.NY_AM,
                SetupType.ORB
        )));

        aiCoachService.generateDailyCoach(user, coachRequest(account.getId(), date, false));
        aiCoachService.getDailyCoach(user, account.getId(), date);

        assertThat(usageLimitService.getUsage(user).getUsageThisMonth().get("DAILY_COACH")).isEqualTo(2);

        aiCoachService.generateDailyCoach(user, coachRequest(account.getId(), date, true));

        assertThat(usageLimitService.getUsage(user).getUsageThisMonth().get("DAILY_COACH")).isEqualTo(4);
    }

    @Test
    void dailyCoachNoTradeDayReturnsDeterministicSnapshotWithoutCredits() {
        User user = createUser("daily-coach-no-trades@example.com", PlanType.PRO);
        user.setPurchasedAiCreditsBalance(75);
        user = userRepository.save(user);
        Long userId = user.getId();
        TradingAccountResponse account = createAccount(user, "No Trades Coach");
        LocalDate date = LocalDate.of(2026, 6, 24);

        CoachSnapshotDto snapshot = aiCoachService.getDailyCoach(user, account.getId(), date);

        assertThat(snapshot.getStatus()).isEqualTo(CoachSnapshotStatus.COMPLETED);
        assertThat(snapshot.getNoTradeDay()).isTrue();
        assertThat(snapshot.getAiGenerated()).isFalse();
        assertThat(snapshot.getCreditsConsumed()).isZero();
        assertThat(snapshot.getTotalTrades()).isZero();
        assertThat(snapshot.getSelectedDaySummary()).isEqualTo("No trades found for this date.");
        assertThat(snapshot.getPrimaryIssueToday()).isEqualTo("Import trades for this date before drawing conclusions.");
        assertThat(snapshot.getSetupDirectionReview()).contains("No setup/direction review available");
        assertThat(snapshot.getDataQuality()).isEqualTo("No Trades");
        assertThat(snapshot.getDataQualityReason()).contains("Import trades first");
        assertThat(snapshot.getAiResponseJson()).isNull();

        BillingUsageResponse usage = usageLimitService.getUsage(userRepository.findById(user.getId()).orElseThrow());
        assertThat(usage.getMonthlyAiCreditsUsed()).isZero();
        assertThat(usage.getPurchasedAiCreditsBalance()).isEqualTo(75);
        assertThat(usage.getTotalAiCreditsAvailable()).isEqualTo(175);
        assertThat(creditUsageRepository.findAll().stream().filter(row -> row.getUserId().equals(userId)).count()).isZero();
    }

    @Test
    void dailyCoachRegenerateNoTradeDayDoesNotConsumeCredits() {
        User user = createUser("daily-coach-no-trades-regenerate@example.com", PlanType.PRO);
        TradingAccountResponse account = createAccount(user, "No Trades Regenerate");
        LocalDate date = LocalDate.of(2026, 6, 25);

        CoachSnapshotDto first = aiCoachService.generateDailyCoach(user, coachRequest(account.getId(), date, true));
        CoachSnapshotDto second = aiCoachService.generateDailyCoach(user, coachRequest(account.getId(), date, true));

        assertThat(first.getStatus()).isEqualTo(CoachSnapshotStatus.COMPLETED);
        assertThat(second.getStatus()).isEqualTo(CoachSnapshotStatus.COMPLETED);
        assertThat(second.getNoTradeDay()).isTrue();
        assertThat(second.getAiGenerated()).isFalse();
        assertThat(usageLimitService.getUsage(user).getMonthlyAiCreditsUsed()).isZero();
        assertThat(creditUsageRepository.findAll().stream().filter(row -> row.getUserId().equals(user.getId())).count()).isZero();
    }

    @Test
    void cachedNoTradeDailyCoachCostsZeroAndTradeImportAllowsPaidGeneration() {
        User user = createUser("daily-coach-no-trade-cache-then-import@example.com", PlanType.PRO);
        TradingAccountResponse account = createAccount(user, "No Trade Cache");
        LocalDate date = LocalDate.of(2026, 6, 26);

        CoachSnapshotDto noTrade = aiCoachService.getDailyCoach(user, account.getId(), date);
        CoachSnapshotDto cachedNoTrade = aiCoachService.getDailyCoach(user, account.getId(), date);

        assertThat(noTrade.getNoTradeDay()).isTrue();
        assertThat(cachedNoTrade.getNoTradeDay()).isTrue();
        assertThat(usageLimitService.getUsage(user).getMonthlyAiCreditsUsed()).isZero();

        tradeService.importTrades(user, List.of(dollarTrade(
                account.getId(),
                "daily-coach-import-after-no-trade",
                date,
                "LONG",
                new BigDecimal("-76.25"),
                SessionType.NY_AM,
                SetupType.ORB
        )));

        CoachSnapshotDto paidGeneration = aiCoachService.generateDailyCoach(user, coachRequest(account.getId(), date, false));

        assertThat(paidGeneration.getNoTradeDay()).isFalse();
        assertThat(paidGeneration.getStatus()).isIn(CoachSnapshotStatus.PENDING, CoachSnapshotStatus.PROCESSING, CoachSnapshotStatus.COMPLETED);
        assertThat(usageLimitService.getUsage(user).getMonthlyAiCreditsUsed()).isEqualTo(2);
    }

    @Test
    void weeklyCoachNoTradeWeekReturnsDeterministicSnapshotWithoutCredits() {
        User user = createUser("weekly-coach-no-trades@example.com", PlanType.PRO);
        user.setPurchasedAiCreditsBalance(75);
        user = userRepository.save(user);
        Long userId = user.getId();
        TradingAccountResponse account = createAccount(user, "No Trades Weekly Coach");
        LocalDate weekStart = LocalDate.of(2026, 6, 15);

        CoachSnapshotDto snapshot = aiCoachService.getWeeklyCoach(user, account.getId(), weekStart);

        assertThat(snapshot.getStatus()).isEqualTo(CoachSnapshotStatus.COMPLETED);
        assertThat(snapshot.getNoTradeWeek()).isTrue();
        assertThat(snapshot.getNoTradeDay()).isFalse();
        assertThat(snapshot.getAiGenerated()).isFalse();
        assertThat(snapshot.getCreditsConsumed()).isZero();
        assertThat(snapshot.getTotalTrades()).isZero();
        assertThat(snapshot.getSelectedDaySummary()).isEqualTo("No trades found for this week.");
        assertThat(snapshot.getPrimaryIssueToday()).isEqualTo("Import trades for this week before generating weekly coaching.");
        assertThat(snapshot.getSetupDirectionReview()).contains("No setup review available");
        assertThat(snapshot.getDataQuality()).isEqualTo("No Trades");
        assertThat(snapshot.getDataQualityReason()).contains("Import trades for this week");
        assertThat(snapshot.getAiResponseJson()).isNull();

        BillingUsageResponse usage = usageLimitService.getUsage(userRepository.findById(user.getId()).orElseThrow());
        assertThat(usage.getMonthlyAiCreditsUsed()).isZero();
        assertThat(usage.getPurchasedAiCreditsBalance()).isEqualTo(75);
        assertThat(usage.getTotalAiCreditsAvailable()).isEqualTo(175);
        assertThat(creditUsageRepository.findAll().stream().filter(row -> row.getUserId().equals(userId)).count()).isZero();
    }

    @Test
    void weeklyCoachRegenerateNoTradeWeekDoesNotConsumeCredits() {
        User user = createUser("weekly-coach-no-trades-regenerate@example.com", PlanType.PRO);
        TradingAccountResponse account = createAccount(user, "No Trades Weekly Regenerate");
        LocalDate weekStart = LocalDate.of(2026, 6, 15);

        CoachSnapshotDto first = aiCoachService.generateWeeklyCoach(user, weeklyCoachRequest(account.getId(), weekStart, true));
        CoachSnapshotDto second = aiCoachService.generateWeeklyCoach(user, weeklyCoachRequest(account.getId(), weekStart, true));

        assertThat(first.getStatus()).isEqualTo(CoachSnapshotStatus.COMPLETED);
        assertThat(second.getStatus()).isEqualTo(CoachSnapshotStatus.COMPLETED);
        assertThat(second.getNoTradeWeek()).isTrue();
        assertThat(second.getAiGenerated()).isFalse();
        assertThat(usageLimitService.getUsage(user).getMonthlyAiCreditsUsed()).isZero();
        assertThat(creditUsageRepository.findAll().stream().filter(row -> row.getUserId().equals(user.getId())).count()).isZero();
    }

    @Test
    void cachedNoTradeWeeklyCoachCostsZeroAndTradeImportAllowsPaidGeneration() {
        User user = createUser("weekly-coach-no-trade-cache-then-import@example.com", PlanType.PRO);
        TradingAccountResponse account = createAccount(user, "No Trade Weekly Cache");
        LocalDate weekStart = LocalDate.of(2026, 6, 15);
        LocalDate tradeDate = weekStart.plusDays(2);

        CoachSnapshotDto noTrade = aiCoachService.getWeeklyCoach(user, account.getId(), weekStart);
        CoachSnapshotDto cachedNoTrade = aiCoachService.getWeeklyCoach(user, account.getId(), weekStart);

        assertThat(noTrade.getNoTradeWeek()).isTrue();
        assertThat(cachedNoTrade.getNoTradeWeek()).isTrue();
        assertThat(usageLimitService.getUsage(user).getMonthlyAiCreditsUsed()).isZero();

        tradeService.importTrades(user, List.of(dollarTrade(
                account.getId(),
                "weekly-coach-import-after-no-trade",
                tradeDate,
                "LONG",
                new BigDecimal("125.00"),
                SessionType.NY_AM,
                SetupType.ORB
        )));

        CoachSnapshotDto paidGeneration = aiCoachService.generateWeeklyCoach(user, weeklyCoachRequest(account.getId(), weekStart, false));

        assertThat(paidGeneration.getNoTradeWeek()).isFalse();
        assertThat(paidGeneration.getStatus()).isIn(CoachSnapshotStatus.PENDING, CoachSnapshotStatus.PROCESSING, CoachSnapshotStatus.COMPLETED);
        assertThat(usageLimitService.getUsage(user).getMonthlyAiCreditsUsed()).isEqualTo(4);
    }

    @Test
    void weeklyCoachWithTradesUsesNormalAiGenerationPath() {
        User user = createUser("weekly-coach-with-trades@example.com", PlanType.PRO);
        TradingAccountResponse account = createAccount(user, "Weekly With Trades");
        LocalDate weekStart = LocalDate.of(2026, 6, 15);
        tradeService.importTrades(user, List.of(dollarTrade(
                account.getId(),
                "weekly-coach-with-trade",
                weekStart.plusDays(1),
                "SHORT",
                new BigDecimal("-80.00"),
                SessionType.NY_PM,
                SetupType.FVG
        )));

        CoachSnapshotDto snapshot = aiCoachService.generateWeeklyCoach(user, weeklyCoachRequest(account.getId(), weekStart, false));

        assertThat(snapshot.getNoTradeWeek()).isFalse();
        assertThat(snapshot.getStatus()).isIn(CoachSnapshotStatus.PENDING, CoachSnapshotStatus.PROCESSING, CoachSnapshotStatus.COMPLETED);
        assertThat(usageLimitService.getUsage(user).getMonthlyAiCreditsUsed()).isEqualTo(4);
    }

    @Test
    void dailyCoachSeparatesSelectedDayLongLossFromHistoricalShortLeak() throws Exception {
        User user = createUser("daily-coach-historical-leak@example.com", PlanType.PRO);
        TradingAccountResponse account = createAccount(user, "Coach");
        LocalDate date = LocalDate.of(2026, 6, 22);
        tradeService.importTrades(user, List.of(new TradeImportRequest(
                account.getId(),
                "tradovate.csv",
                "daily-long-loss",
                "MESU6",
                "FUTURES",
                date,
                date.atTime(9, 30),
                date.atTime(9, 45),
                "LONG",
                null,
                SessionType.NY_AM,
                new BigDecimal("5485.00"),
                new BigDecimal("5477.50"),
                1,
                new BigDecimal("-76.25"),
                null,
                OutcomeType.LOSS,
                "MESU6",
                "MESU6",
                "IMPORTED"
        )));
        leakDetectorSnapshotRepository.save(AiLeakDetectorSnapshot.builder()
                .userId(user.getId())
                .accountId(account.getId())
                .dataHash("historical-short-leak")
                .totalTrades(35)
                .resultJson(objectMapper.writeValueAsString(List.of(LeakResultDto.of(
                        LeakType.LONG_SHORT_BIAS,
                        "Short Side Bias Leak",
                        "SHORT is dragging performance by -$2533.75 across 35 trades.",
                        "Short-side trades have underperformed recently.",
                        35,
                        BigDecimal.ZERO,
                        new BigDecimal("-2533.75"),
                        new BigDecimal("-72.39"),
                        new BigDecimal("-1085.89"),
                        null,
                        null,
                        null,
                        false,
                        82,
                        LeakSeverity.HIGH,
                        "Reduce size on short trades.",
                        35,
                        List.of(1L, 2L),
                        LocalDateTime.now()
                ))))
                .model("test")
                .build());

        aiCoachService.generateDailyCoach(user, coachRequest(account.getId(), date, false));
        CoachSnapshotDto dailyCoach = waitForDailyCoach(user, account.getId(), date);

        assertThat(dailyCoach.getSelectedDaySummary()).contains("MES LONG");
        assertThat(dailyCoach.getPrimaryIssueToday()).contains("not an overtrading day");
        assertThat(dailyCoach.getHistoricalPatternToWatch())
                .contains("Account-level pattern")
                .contains("Recent account history shows short-side trades have underperformed by -$2,533.75 across 35 trades")
                .contains("This did not apply today");
        assertThat(dailyCoach.getHistoricalPatternAppliesToday()).isFalse();
        assertThat(dailyCoach.getSelectedDaySummary()).doesNotContain("SHORT");
        assertThat(dailyCoach.getPrimaryIssueToday()).doesNotContain("SHORT");
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
    void tradeReviewMissingTradeIdDoesNotConsumeCredits() {
        User user = createUser("ai-review-missing-trade-id@example.com", PlanType.PRO);
        user.setPurchasedAiCreditsBalance(25);
        user = userRepository.save(user);
        Long userId = user.getId();
        User savedUser = user;

        assertThatThrownBy(() -> aiTradeReviewService.reviewTrade(savedUser, new AiTradeReviewRequest()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tradeId");

        BillingUsageResponse usage = usageLimitService.getUsage(userRepository.findById(userId).orElseThrow());
        assertThat(usage.getMonthlyAiCreditsUsed()).isZero();
        assertThat(usage.getPurchasedAiCreditsBalance()).isEqualTo(25);
        assertThat(creditUsageRepository.findAll().stream().filter(row -> row.getUserId().equals(userId)).count()).isZero();
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
        tradeService.importTrades(user, importTrades(account.getId(), 5, "dna-pro"));
        assertThat(tradeRepository.findByUserAndAccount_IdAndTradeDateBetweenOrderByTradeDateDesc(
                user,
                account.getId(),
                LocalDate.now().minusDays(29),
                LocalDate.now()
        )).hasSize(5);
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
    void tradingDnaNoTradesSkipsAiAndCreditsAndCachesResult() {
        User user = createUser("dna-no-trades-skip@example.com", PlanType.PRO);
        user.setPurchasedAiCreditsBalance(25);
        user = userRepository.save(user);
        Long userId = user.getId();
        TradingAccountResponse account = createAccount(user, "DNA No Trades");

        TradingDnaResponse first = tradingDnaService.getTradingDna(user, account.getId(), 30, false, false);
        TradingDnaResponse cached = tradingDnaService.getTradingDna(user, account.getId(), 30, false, false);

        assertThat(first.getStatus()).isEqualTo("NO_TRADES");
        assertThat(first.getNoTrades()).isTrue();
        assertThat(first.getAiGenerated()).isFalse();
        assertThat(first.getCreditsConsumed()).isZero();
        assertThat(cached.getCached()).isTrue();
        assertThat(cached.getNoTrades()).isTrue();
        BillingUsageResponse usage = usageLimitService.getUsage(userRepository.findById(userId).orElseThrow());
        assertThat(usage.getMonthlyAiCreditsUsed()).isZero();
        assertThat(usage.getPurchasedAiCreditsBalance()).isEqualTo(25);
        assertThat(creditUsageRepository.findAll().stream().filter(row -> row.getUserId().equals(userId)).count()).isZero();
    }

    @Test
    void tradingDnaLimitedDataSkipsAiAndImportedTradesLaterAllowGeneration() {
        User user = createUser("dna-limited-data-skip@example.com", PlanType.PRO);
        TradingAccountResponse account = createAccount(user, "DNA Limited Data");
        tradeService.importTrades(user, importTrades(account.getId(), 4, "dna-limited"));
        assertThat(tradeRepository.findByUserAndAccount_IdAndTradeDateBetweenOrderByTradeDateDesc(
                user,
                account.getId(),
                LocalDate.now().minusDays(29),
                LocalDate.now()
        )).hasSize(4);

        TradingDnaResponse limited = tradingDnaService.getTradingDna(user, account.getId(), 30, false, true);

        assertThat(limited.getStatus()).isEqualTo("LIMITED_DATA");
        assertThat(limited.getLimitedData()).isTrue();
        assertThat(limited.getAiGenerated()).isFalse();
        assertThat(usageLimitService.getUsage(user).getMonthlyAiCreditsUsed()).isZero();

        tradeService.importTrades(user, importTrades(account.getId(), 1, "dna-limited-extra"));
        assertThat(tradeRepository.findByUserAndAccount_IdAndTradeDateBetweenOrderByTradeDateDesc(
                user,
                account.getId(),
                LocalDate.now().minusDays(29),
                LocalDate.now()
        )).hasSize(5);
        TradingDnaResponse generated = tradingDnaService.getTradingDna(user, account.getId(), 30, false, false);

        assertThat(generated.getStatus()).isEqualTo("COMPLETED");
        assertThat(generated.getAiGenerated()).isTrue();
        assertThat(usageLimitService.getUsage(user).getMonthlyAiCreditsUsed()).isEqualTo(3);
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
    void leakDetectorNoTradesSkipsCreditsAndCachesResult() {
        User user = createUser("leak-no-trades-skip@example.com", PlanType.PRO);
        user.setPurchasedAiCreditsBalance(40);
        user = userRepository.save(user);
        Long userId = user.getId();
        TradingAccountResponse account = createAccount(user, "Leak No Trades");

        LeakDetectorResponse first = leakDetectorService.getLeakDetector(user, account.getId());
        LeakDetectorResponse cached = leakDetectorService.getLeakDetector(user, account.getId());

        assertThat(first.getNoTrades()).isTrue();
        assertThat(first.getAiGenerated()).isFalse();
        assertThat(first.getCreditsConsumed()).isZero();
        assertThat(first.getLeaks()).isEmpty();
        assertThat(cached.getCached()).isTrue();
        assertThat(cached.getNoTrades()).isTrue();
        BillingUsageResponse usage = usageLimitService.getUsage(userRepository.findById(userId).orElseThrow());
        assertThat(usage.getMonthlyAiCreditsUsed()).isZero();
        assertThat(usage.getPurchasedAiCreditsBalance()).isEqualTo(40);
        assertThat(creditUsageRepository.findAll().stream().filter(row -> row.getUserId().equals(userId)).count()).isZero();
    }

    @Test
    void leakDetectorLimitedDataSkipsCreditsAndTenTradesAllowGeneration() {
        User user = createUser("leak-limited-data-skip@example.com", PlanType.PRO);
        TradingAccountResponse account = createAccount(user, "Leak Limited Data");
        tradeService.importTrades(user, importTrades(account.getId(), 9, "leak-limited"));

        LeakDetectorResponse limited = leakDetectorService.regenerate(user, account.getId());

        assertThat(limited.getLimitedData()).isTrue();
        assertThat(limited.getAiGenerated()).isFalse();
        assertThat(usageLimitService.getUsage(user).getMonthlyAiCreditsUsed()).isZero();

        tradeService.importTrades(user, importTrades(account.getId(), 1, "leak-limited-extra"));
        LeakDetectorResponse generated = leakDetectorService.getLeakDetector(user, account.getId());

        assertThat(generated.getAiGenerated()).isTrue();
        assertThat(generated.getTotalTrades()).isEqualTo(10);
        assertThat(usageLimitService.getUsage(user).getMonthlyAiCreditsUsed()).isEqualTo(3);
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

    private CoachSnapshotDto waitForDailyCoach(User user, Long accountId, LocalDate date) {
        CoachSnapshotDto latest = null;
        for (int i = 0; i < 30; i++) {
            latest = aiCoachService.getDailyCoach(user, accountId, date);
            if (latest.getStatus() == CoachSnapshotStatus.COMPLETED) {
                return latest;
            }
            try {
                Thread.sleep(50);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        assertThat(latest).isNotNull();
        assertThat(latest.getStatus()).isEqualTo(CoachSnapshotStatus.COMPLETED);
        return latest;
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
    void creditDeductionUsesMonthlyCreditsBeforePurchasedCredits() {
        User user = createUser("credit-split-monthly-first@example.com", PlanType.PRO);
        user.setPurchasedAiCreditsBalance(10);
        userRepository.save(user);
        creditUsageRepository.save(AiCreditUsage.builder()
                .userId(user.getId())
                .feature(Feature.AI_TRADE_REVIEW)
                .credits(99)
                .monthlyCredits(99)
                .purchasedCredits(0)
                .createdAt(LocalDateTime.now())
                .build());

        usageLimitService.consumeCredits(user, Feature.TRADING_DNA, 3);

        User updated = userRepository.findById(user.getId()).orElseThrow();
        AiCreditUsage charge = creditUsageRepository.findAll().stream()
                .filter(row -> row.getUserId().equals(user.getId()) && row.getFeature() == Feature.TRADING_DNA)
                .findFirst()
                .orElseThrow();
        assertThat(charge.getCredits()).isEqualTo(3);
        assertThat(charge.getMonthlyCredits()).isEqualTo(1);
        assertThat(charge.getPurchasedCredits()).isEqualTo(2);
        assertThat(updated.getPurchasedAiCreditsBalance()).isEqualTo(8);
        assertThat(usageLimitService.getUsage(updated).getTotalAiCreditsAvailable()).isEqualTo(8);
    }

    @Test
    void aiRequestFailsOnlyWhenMonthlyAndPurchasedCreditsAreInsufficient() {
        User user = createUser("credit-combined-insufficient@example.com", PlanType.PRO);
        user.setPurchasedAiCreditsBalance(2);
        userRepository.save(user);
        creditUsageRepository.save(AiCreditUsage.builder()
                .userId(user.getId())
                .feature(Feature.AI_TRADE_REVIEW)
                .credits(100)
                .monthlyCredits(100)
                .purchasedCredits(0)
                .createdAt(LocalDateTime.now())
                .build());

        assertThatThrownBy(() -> usageLimitService.consumeCredits(user, Feature.TRADING_DNA, 3))
                .isInstanceOf(AiCreditLimitExceededException.class)
                .extracting("creditsRemaining")
                .isEqualTo(2);
    }

    @Test
    void purchasedCreditsAreUsedWhenMonthlyCreditsAreExhausted() {
        User user = createUser("credit-purchased-after-monthly@example.com", PlanType.PRO);
        user.setPurchasedAiCreditsBalance(5);
        userRepository.save(user);
        creditUsageRepository.save(AiCreditUsage.builder()
                .userId(user.getId())
                .feature(Feature.AI_TRADE_REVIEW)
                .credits(100)
                .monthlyCredits(100)
                .purchasedCredits(0)
                .createdAt(LocalDateTime.now())
                .build());

        usageLimitService.consumeCredits(user, Feature.SESSION_REVIEW, 2);

        User updated = userRepository.findById(user.getId()).orElseThrow();
        AiCreditUsage charge = creditUsageRepository.findAll().stream()
                .filter(row -> row.getUserId().equals(user.getId()) && row.getFeature() == Feature.SESSION_REVIEW)
                .findFirst()
                .orElseThrow();
        assertThat(charge.getMonthlyCredits()).isZero();
        assertThat(charge.getPurchasedCredits()).isEqualTo(2);
        assertThat(updated.getPurchasedAiCreditsBalance()).isEqualTo(3);
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
                        null,
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
                            pnlR.signum() >= 0 ? OutcomeType.WIN : OutcomeType.LOSS,
                            null,
                            null,
                            null
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
                pnl.signum() >= 0 ? OutcomeType.WIN : OutcomeType.LOSS,
                null,
                null,
                null
        );
    }

    private TradeImportRequest importTrade(Long accountId, String externalTradeKey, BigDecimal pnl) {
        LocalDate tradeDate = LocalDate.now().minusDays(7);
        return new TradeImportRequest(
                accountId,
                "tradovate.csv",
                externalTradeKey,
                "NQ",
                "FUTURES",
                tradeDate,
                tradeDate.atTime(9, 30),
                tradeDate.atTime(9, 45),
                "LONG",
                SetupType.ORB,
                SessionType.NY_AM,
                BigDecimal.valueOf(18_000),
                BigDecimal.valueOf(18_010),
                1,
                pnl,
                BigDecimal.ONE,
                pnl.signum() >= 0 ? OutcomeType.WIN : OutcomeType.LOSS,
                null,
                null,
                null
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

    private CoachGenerateRequest weeklyCoachRequest(Long accountId, LocalDate weekStart, boolean forceRefresh) {
        CoachGenerateRequest request = new CoachGenerateRequest();
        request.setAccountId(accountId);
        request.setPeriodStart(weekStart);
        request.setPeriodEnd(weekStart.plusDays(6));
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
