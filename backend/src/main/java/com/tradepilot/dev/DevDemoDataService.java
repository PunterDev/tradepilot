package com.tradepilot.dev;

import com.tradepilot.billing.*;
import com.tradepilot.coach.entity.*;
import com.tradepilot.coach.repository.AiCoachSnapshotRepository;
import com.tradepilot.entity.*;
import com.tradepilot.enums.*;
import com.tradepilot.featureusage.FeatureKey;
import com.tradepilot.featureusage.FeatureUsageEvent;
import com.tradepilot.featureusage.FeatureUsageRepository;
import com.tradepilot.repository.*;
import com.tradepilot.tradingdna.entity.TradingDnaSnapshot;
import com.tradepilot.tradingdna.repository.TradingDnaSnapshotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.util.*;

@Service
@RequiredArgsConstructor
public class DevDemoDataService {
    private final TradingAccountRepository accountRepository;
    private final TradeRepository tradeRepository;
    private final AiTradeReviewRepository reviewRepository;
    private final AiTradeAnalysisRepository analysisRepository;
    private final AiCoachSnapshotRepository coachSnapshotRepository;
    private final TradingDnaSnapshotRepository tradingDnaSnapshotRepository;
    private final AiSessionReviewRepository sessionReviewRepository;
    private final TradeRuleViolationRepository violationRepository;
    private final DailyDisciplineScoreRepository disciplineScoreRepository;
    private final AiUsageEventRepository usageEventRepository;
    private final FeatureUsageRepository featureUsageRepository;
    private final UserRepository userRepository;

    private static final String[] SYMBOLS = {"NQ", "MNQ", "ES", "MES"};
    private static final SetupType[] SETUPS = {
            SetupType.FVG,
            SetupType.IFVG,
            SetupType.RECLAIM,
            SetupType.ORB,
            SetupType.LIQUIDITY_SWEEP
    };
    private static final String[] SETUP_LABELS = {
            "5m FVG continuation",
            "5m IFVG",
            "London sweep",
            "Opening range fakeout",
            "HTF liquidity sweep"
    };
    private static final SessionType[] SESSIONS = {
            SessionType.NY_AM,
            SessionType.NY_PM,
            SessionType.LONDON
    };
    private static final MistakeTag[] MISTAKES = {
            MistakeTag.REVENGE_TRADE,
            MistakeTag.MOVED_TO_BE_TOO_EARLY,
            MistakeTag.OVERTRADING,
            MistakeTag.NO_CONFIRMATION,
            MistakeTag.FOMO
    };
    private static final String[] MISTAKE_LABELS = {
            "revenge trade",
            "moved stop too early",
            "overtraded chop",
            "entered before confirmation",
            "oversized after win"
    };

    @Transactional
    public Map<String, Object> seed(User user) {
        clear(user);

        TradingAccount topstep = accountRepository.save(TradingAccount.builder()
                .user(user)
                .name("Topstep Evaluation")
                .propFirm(PropFirmType.TOPSTEP)
                .accountNumber("TS-DEMO-50K")
                .accountType(TradingAccountType.EVALUATION)
                .startingBalance(BigDecimal.valueOf(50_000))
                .status(TradingAccountStatus.ACTIVE)
                .build());

        TradingAccount personal = accountRepository.save(TradingAccount.builder()
                .user(user)
                .name("Personal Tradovate")
                .propFirm(PropFirmType.OTHER)
                .accountNumber("TV-DEMO-LIVE")
                .accountType(TradingAccountType.PERSONAL)
                .startingBalance(BigDecimal.valueOf(25_000))
                .status(TradingAccountStatus.ACTIVE)
                .build());

        List<Trade> trades = seedTrades(user, List.of(topstep, personal));
        seedAiReviews(user, trades);
        seedCoachSnapshots(user, topstep, trades);
        seedTradingDna(user, trades);
        seedDiscipline(user, trades);
        seedUsageEvents(user);
        Map<String, Object> featureUsage = backfillFeatureUsage(user);

        return Map.of(
                "accountsCreated", 2,
                "tradesCreated", trades.size(),
                "aiReviewsCreated", Math.min(18, trades.size()),
                "featureUsageEventsCreated", featureUsage.get("featureUsageEventsCreated"),
                "message", "Demo data seeded for current user."
        );
    }

    @Transactional
    public Map<String, Object> clear(User user) {
        Long userId = user.getId();
        violationRepository.deleteByUserId(userId);
        disciplineScoreRepository.deleteByUserId(userId);
        reviewRepository.deleteByUserId(userId);
        analysisRepository.deleteByUserId(userId);
        coachSnapshotRepository.deleteByUserId(userId);
        tradingDnaSnapshotRepository.deleteByUserId(userId);
        sessionReviewRepository.deleteByUserId(userId);
        usageEventRepository.deleteByUserId(userId);
        featureUsageRepository.deleteByUserId(userId);
        tradeRepository.deleteByUser(user);
        accountRepository.deleteByUser(user);
        return Map.of("message", "Demo data cleared for current user.");
    }

    @Transactional
    public Map<String, Object> backfillFeatureUsage(User user) {
        Long userId = user.getId();
        featureUsageRepository.deleteByUserId(userId);

        List<FeatureUsageEvent> events = new ArrayList<>();
        reviewRepository.findByUserId(userId).forEach(review ->
                events.add(featureEvent(userId, FeatureKey.AI_REVIEW, toLocalDateTime(review.getCreatedAt())))
        );
        coachSnapshotRepository.findByUserId(userId).forEach(snapshot -> {
            FeatureKey feature = snapshot.getSnapshotType() == CoachSnapshotType.WEEKLY
                    ? FeatureKey.WEEKLY_COACH
                    : FeatureKey.DAILY_COACH;
            events.add(featureEvent(userId, feature, snapshot.getCreatedAt()));
        });
        tradingDnaSnapshotRepository.findByUserId(userId).forEach(snapshot ->
                events.add(featureEvent(userId, FeatureKey.TRADING_DNA, snapshot.getCreatedAt()))
        );
        sessionReviewRepository.findByUserId(userId).forEach(review ->
                events.add(featureEvent(userId, FeatureKey.SESSION_REVIEW, review.getCreatedAt()))
        );

        featureUsageRepository.saveAll(events);
        return Map.of(
                "featureUsageEventsCreated", events.size(),
                "message", "Feature usage backfilled for current user."
        );
    }

    @Transactional
    public Map<String, Object> mockUpgradePro(User user) {
        user.setPlan(PlanType.PRO);
        user.setSubscriptionStatus(SubscriptionStatus.ACTIVE);
        user.setStripeCustomerId("mock_customer");
        user.setStripeSubscriptionId("mock_subscription");
        userRepository.save(user);
        return Map.of("plan", user.getPlan(), "subscriptionStatus", user.getSubscriptionStatus());
    }

    @Transactional
    public Map<String, Object> mockDowngradeFree(User user) {
        user.setPlan(PlanType.FREE);
        user.setSubscriptionStatus(SubscriptionStatus.NONE);
        user.setStripeCustomerId(null);
        user.setStripeSubscriptionId(null);
        userRepository.save(user);
        return Map.of("plan", user.getPlan(), "subscriptionStatus", user.getSubscriptionStatus());
    }

    private List<Trade> seedTrades(User user, List<TradingAccount> accounts) {
        List<Trade> trades = new ArrayList<>();
        LocalDate today = LocalDate.now();
        int tradeCount = 190;

        for (int i = 0; i < tradeCount; i++) {
            LocalDate date = today.minusDays(59L - (i % 60));
            TradingAccount account = accounts.get(i % accounts.size());
            String symbol = SYMBOLS[i % SYMBOLS.length];
            SetupType setup = SETUPS[i % SETUPS.length];
            SessionType session = SESSIONS[i % SESSIONS.length];
            boolean friday = date.getDayOfWeek() == DayOfWeek.FRIDAY;
            boolean opexLike = friday && date.getDayOfMonth() >= 15 && date.getDayOfMonth() <= 21;
            boolean plannedLoss = i % 5 == 0 || (opexLike && i % 2 == 0);
            BigDecimal risk = BigDecimal.valueOf(symbol.startsWith("M") ? 75 : 180);
            BigDecimal pnlR = plannedLoss
                    ? BigDecimal.valueOf(-0.65 - (i % 3) * 0.35)
                    : BigDecimal.valueOf(0.75 + (i % 4) * 0.45);
            BigDecimal pnl = risk.multiply(pnlR).setScale(2, RoundingMode.HALF_UP);
            LocalTime entryClock = switch (session) {
                case LONDON -> LocalTime.of(3 + (i % 3), 15 + (i % 4) * 10);
                case NY_PM -> LocalTime.of(13 + (i % 2), 5 + (i % 5) * 7);
                default -> LocalTime.of(9 + (i % 2), 30 + (i % 5) * 6);
            };
            LocalDateTime entry = LocalDateTime.of(date, entryClock);
            LocalDateTime exit = entry.plusMinutes(8 + (i % 7) * 6L);
            BigDecimal entryPrice = BigDecimal.valueOf(symbol.contains("ES") ? 5300 : 18800)
                    .add(BigDecimal.valueOf((i % 31) * 2.25));
            BigDecimal exitPrice = entryPrice.add(BigDecimal.valueOf(pnl.signum() >= 0 ? 18 + (i % 8) : -10 - (i % 7)));

            MistakeTag mistake = plannedLoss ? MISTAKES[i % MISTAKES.length] : MistakeTag.NONE;
            String mistakeLabel = plannedLoss ? MISTAKE_LABELS[i % MISTAKE_LABELS.length] : "none";
            String setupLabel = SETUP_LABELS[i % SETUP_LABELS.length];

            trades.add(Trade.builder()
                    .user(user)
                    .account(account)
                    .symbol(symbol)
                    .market("FUTURES")
                    .tradeDate(date)
                    .entryTime(entry)
                    .exitTime(exit)
                    .side(i % 2 == 0 ? "LONG" : "SHORT")
                    .setup(setup)
                    .session(session)
                    .entryPrice(entryPrice)
                    .stopPrice(entryPrice.subtract(BigDecimal.valueOf(12)))
                    .targetPrice(entryPrice.add(BigDecimal.valueOf(28)))
                    .exitPrice(exitPrice)
                    .quantity(symbol.startsWith("M") ? 2 + (i % 2) : 1)
                    .riskAmount(risk)
                    .pnlAmount(pnl)
                    .pnlR(pnlR.setScale(2, RoundingMode.HALF_UP))
                    .outcome(pnl.signum() > 0 ? OutcomeType.WIN : OutcomeType.LOSS)
                    .confidenceRating(plannedLoss ? 4 + (i % 2) : 7 + (i % 3))
                    .executionRating(plannedLoss ? 3 + (i % 3) : 7 + (i % 3))
                    .followedPlan(!plannedLoss)
                    .mistakeTag(mistake)
                    .notes("%s in %s. %s%s".formatted(
                            setupLabel,
                            session.name().replace("_", " "),
                            plannedLoss ? "Mistake: " + mistakeLabel + "." : "Clean execution into target rotation.",
                            opexLike ? " OPEX-like Friday volatility noted." : ""
                    ))
                    .psychologyNotes(plannedLoss ? "Felt urgency after prior result; focus slipped." : "Patient entry and calm management.")
                    .lessonLearned(plannedLoss ? "Wait for confirmation and cap size after emotional impulse." : "Keep repeating this setup only when context aligns.")
                    .externalTradeKey("DEV-DEMO-" + user.getId() + "-" + i)
                    .build());
        }

        return tradeRepository.saveAll(trades);
    }

    private void seedAiReviews(User user, List<Trade> trades) {
        Instant now = Instant.now();
        List<AiTradeReview> reviews = new ArrayList<>();
        List<AiTradeAnalysis> analyses = new ArrayList<>();
        for (int i = 0; i < Math.min(18, trades.size()); i++) {
            Trade trade = trades.get(i * 3);
            String grade = trade.getPnlAmount().signum() >= 0 ? "A-" : "C+";
            reviews.add(AiTradeReview.builder()
                    .userId(user.getId())
                    .tradeId(trade.getId())
                    .accountId(trade.getAccount().getId())
                    .reviewDate(trade.getTradeDate().toString())
                    .symbol(trade.getSymbol())
                    .pnl(trade.getPnlAmount().doubleValue())
                    .grade(grade)
                    .reviewVersion("dev-demo-review")
                    .reviewHash("dev-review-" + trade.getId())
                    .summary("Demo AI review: recurring pattern is strong setup selection with occasional impatience during volatile sessions.")
                    .mistakesJson("[\"entered before confirmation\",\"managed stop too tightly\"]")
                    .strengthsJson("[\"clear market structure read\",\"good risk definition\"]")
                    .improvementPlanJson("[\"pause after one loss\",\"confirm displacement before entry\"]")
                    .tagsJson("[\"demo\",\"discipline\",\"risk\"]")
                    .status("COMPLETED")
                    .createdAt(now.minusSeconds(i * 3600L))
                    .build());

            AiTradeAnalysis analysis = new AiTradeAnalysis();
            analysis.setUserId(user.getId());
            analysis.setTradeId(trade.getId());
            analysis.setReviewVersion("dev-demo-analysis");
            analysis.setReviewHash("dev-analysis-" + trade.getId());
            analysis.setStatus("COMPLETED");
            analysis.setSymbol(trade.getSymbol());
            analysis.setPnl(trade.getPnlAmount());
            analysis.setGrade(grade);
            analysis.setExecutionScore(trade.getExecutionRating());
            analysis.setDisciplineScore(Boolean.TRUE.equals(trade.getFollowedPlan()) ? 86 : 58);
            analysis.setRiskScore(Boolean.TRUE.equals(trade.getFollowedPlan()) ? 82 : 52);
            analysis.setSummary("Demo unified analysis with chart/risk/psychology context.");
            analysis.setMarketContext("Volatility expanded near liquidity; best trades waited for confirmation.");
            analysis.setEntryAnalysis("Entry quality was strongest after displacement and retest.");
            analysis.setExitAnalysis("Winners were held to planned targets; losses often exited after stop movement.");
            analysis.setRiskAnalysis("Risk was generally controlled, with oversized trades after wins appearing as a pattern.");
            analysis.setPsychologyAnalysis("Primary tendency: impatience after missed moves.");
            analysis.setChartAnalysis("Demo chart note: structure aligned with session bias.");
            analysis.setMistakesJson("[\"overtraded chop\",\"revenge trade\"]");
            analysis.setStrengthsJson("[\"FVG continuation\",\"liquidity sweep recognition\"]");
            analysis.setImprovementPlanJson("[\"one setup per session\",\"no add-ons after first loss\"]");
            analysis.setTagsJson("[\"demo\",\"analysis\"]");
            analyses.add(analysis);
        }
        reviewRepository.saveAll(reviews);
        analysisRepository.saveAll(analyses);
    }

    private void seedCoachSnapshots(User user, TradingAccount account, List<Trade> trades) {
        LocalDate today = LocalDate.now();
        coachSnapshotRepository.save(AiCoachSnapshot.builder()
                .userId(user.getId())
                .accountId(account.getId())
                .snapshotType(CoachSnapshotType.DAILY)
                .periodStart(today.minusDays(1))
                .periodEnd(today.minusDays(1))
                .status(CoachSnapshotStatus.COMPLETED)
                .totalTrades(6)
                .netPnl(BigDecimal.valueOf(420))
                .winRate(BigDecimal.valueOf(66.7))
                .profitFactor(BigDecimal.valueOf(1.85))
                .bestSetup("5m FVG continuation")
                .worstSetup("Opening range fakeout")
                .bestSession("NY AM")
                .worstSession("NY PM")
                .biggestMistake("entered before confirmation")
                .reviewHash("dev-daily-coach")
                .aiResponseJson("{\"headline\":\"Your best trades came from patience after the first displacement.\",\"emotionalPattern\":\"Impatience appears after missed moves.\",\"bestBehavior\":\"You respected risk when the setup was preplanned.\",\"avoidNextSession\":\"Do not chase the second move in chop.\",\"nextSessionFocus\":\"Wait for confirmation before entry.\",\"oneRuleForNextSession\":\"One loss, then five-minute reset.\"}")
                .deterministicSummaryJson("{\"demo\":true}")
                .build());

        coachSnapshotRepository.save(AiCoachSnapshot.builder()
                .userId(user.getId())
                .accountId(account.getId())
                .snapshotType(CoachSnapshotType.WEEKLY)
                .periodStart(today.minusDays(7))
                .periodEnd(today.minusDays(1))
                .status(CoachSnapshotStatus.COMPLETED)
                .totalTrades(Math.min(42, trades.size()))
                .netPnl(BigDecimal.valueOf(1280))
                .winRate(BigDecimal.valueOf(58.1))
                .profitFactor(BigDecimal.valueOf(1.42))
                .bestSetup("HTF liquidity sweep")
                .worstSetup("Opening range fakeout")
                .bestSession("London")
                .worstSession("NY PM")
                .biggestMistake("overtraded chop")
                .reviewHash("dev-weekly-coach")
                .aiResponseJson("{\"headline\":\"Your week improves when you avoid NY PM chop.\",\"emotionalPattern\":\"Frustration rises after two missed entries.\",\"bestBehavior\":\"Strong patience in London sweep setups.\",\"riskNote\":\"Keep size flat after wins.\",\"disciplineNote\":\"Friday/OPEX-like sessions need stricter filters.\",\"nextSessionFocus\":\"Trade only A setups in the first 90 minutes.\"}")
                .deterministicSummaryJson("{\"demo\":true}")
                .build());
    }

    private void seedTradingDna(User user, List<Trade> trades) {
        tradingDnaSnapshotRepository.save(TradingDnaSnapshot.builder()
                .userId(user.getId())
                .accountId(null)
                .days(30)
                .includeAllAccounts(true)
                .dataHash("dev-trading-dna")
                .status("COMPLETED")
                .totalTrades(Math.min(95, trades.size()))
                .aiResponseJson("{\"strengths\":[\"Strong liquidity sweep recognition\",\"Good risk when setup is preplanned\"],\"weaknesses\":[\"Overtrading chop\",\"Friday volatility impatience\"],\"emotionalPatterns\":[\"Revenge impulse after missed move\"],\"riskPatterns\":[\"Occasional oversized trade after a win\"],\"bestConditions\":[\"London sweep\",\"NY AM 5m FVG continuation\"],\"worstConditions\":[\"NY PM chop\",\"OPEX-like Fridays\"],\"recurringMistakes\":[\"entered before confirmation\",\"moved stop too early\"],\"improvementTrend\":\"Improving over the last two weeks\",\"consistencyScore\":74,\"disciplineScore\":69,\"executionScore\":78,\"patienceScore\":66,\"confidenceSummary\":\"Confidence is best after one planned winner, but slips after missed continuation moves.\",\"coachingSummary\":\"Protect your edge by limiting trade count and requiring confirmation on volatile Fridays.\"}")
                .contextJson("{\"demo\":true}")
                .timelineJson("[{\"date\":\"%s\",\"consistency\":68,\"winRate\":54,\"emotionalStability\":61,\"discipline\":64},{\"date\":\"%s\",\"consistency\":74,\"winRate\":59,\"emotionalStability\":70,\"discipline\":72}]".formatted(LocalDate.now().minusDays(14), LocalDate.now()))
                .build());
    }

    private void seedDiscipline(User user, List<Trade> trades) {
        List<TradeRuleViolation> violations = new ArrayList<>();
        Map<LocalDate, Integer> countsByDate = new LinkedHashMap<>();
        for (int i = 0; i < trades.size(); i += 7) {
            Trade trade = trades.get(i);
            violations.add(TradeRuleViolation.builder()
                    .userId(user.getId())
                    .tradeId(trade.getId())
                    .tradeDate(trade.getTradeDate())
                    .ruleCode(i % 2 == 0 ? "REVENGE_TRADE" : "NO_CONFIRMATION")
                    .ruleTitle(i % 2 == 0 ? "Revenge trade after loss" : "Entered before confirmation")
                    .category("DISCIPLINE")
                    .severity(i % 3 == 0 ? "MAJOR" : "MINOR")
                    .reason("Demo violation generated from seeded mistake pattern.")
                    .source("DEV_DEMO")
                    .build());
            countsByDate.merge(trade.getTradeDate(), 1, Integer::sum);
        }
        violationRepository.saveAll(violations);

        List<DailyDisciplineScore> scores = countsByDate.entrySet().stream()
                .map(entry -> DailyDisciplineScore.builder()
                        .userId(user.getId())
                        .tradeDate(entry.getKey())
                        .accountId(null)
                        .score(Math.max(45, 92 - entry.getValue() * 12))
                        .totalTrades(4 + entry.getValue())
                        .violationCount(entry.getValue())
                        .majorViolationCount(entry.getValue() > 1 ? 1 : 0)
                        .criticalViolationCount(0)
                        .currentCleanDayStreak(entry.getValue() == 0 ? 3 : 0)
                        .cleanDay(entry.getValue() == 0)
                        .build())
                .toList();
        disciplineScoreRepository.saveAll(scores);
    }

    private void seedUsageEvents(User user) {
        LocalDateTime now = LocalDateTime.now();
        usageEventRepository.saveAll(List.of(
                AiUsageEvent.builder().userId(user.getId()).usageType(UsageType.AI_TRADE_REVIEW).createdAt(now.minusDays(5)).build(),
                AiUsageEvent.builder().userId(user.getId()).usageType(UsageType.AI_TRADE_REVIEW).createdAt(now.minusDays(4)).build(),
                AiUsageEvent.builder().userId(user.getId()).usageType(UsageType.DAILY_COACH).createdAt(now.minusDays(2)).build(),
                AiUsageEvent.builder().userId(user.getId()).usageType(UsageType.WEEKLY_COACH).createdAt(now.minusDays(1)).build(),
                AiUsageEvent.builder().userId(user.getId()).usageType(UsageType.TRADING_DNA).createdAt(now.minusHours(8)).build()
        ));
    }

    private FeatureUsageEvent featureEvent(Long userId, FeatureKey feature, LocalDateTime createdAt) {
        return FeatureUsageEvent.builder()
                .userId(userId)
                .feature(feature)
                .createdAt(createdAt == null ? LocalDateTime.now() : createdAt)
                .build();
    }

    private LocalDateTime toLocalDateTime(Instant instant) {
        return instant == null ? LocalDateTime.now() : LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
    }
}
