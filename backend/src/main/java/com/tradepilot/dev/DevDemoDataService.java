package com.tradepilot.dev;

import com.tradepilot.billing.*;
import com.tradepilot.coach.entity.*;
import com.tradepilot.coach.repository.AiCoachSnapshotRepository;
import com.tradepilot.entity.*;
import com.tradepilot.enums.*;
import com.tradepilot.featureusage.FeatureKey;
import com.tradepilot.featureusage.FeatureUsageEvent;
import com.tradepilot.featureusage.FeatureUsageRepository;
import com.tradepilot.importing.FuturesPointValueService;
import com.tradepilot.dev.e2e.E2eTestDataResponse;
import com.tradepilot.ai.memory.repository.TradeMemoryRepository;
import com.tradepilot.ai.memory.service.TradeMemoryIndexService;
import com.tradepilot.repository.*;
import com.tradepilot.tradereplay.entity.TradeReplay;
import com.tradepilot.tradereplay.repository.TradeReplayInsightRepository;
import com.tradepilot.tradereplay.repository.TradeReplayRepository;
import com.tradepilot.tradereplay.service.ContractSymbolSource;
import com.tradepilot.tradereplay.service.FuturesContractCalendar;
import com.tradepilot.tradingdna.entity.TradingDnaSnapshot;
import com.tradepilot.tradingdna.repository.TradingDnaSnapshotRepository;
import com.tradepilot.survival.ProtectionCommitmentSource;
import com.tradepilot.survival.ProtectionCommitmentStatus;
import com.tradepilot.survival.AccountSurvivalTimeSource;
import com.tradepilot.survival.entity.NextSessionProtectionCommitment;
import com.tradepilot.survival.repository.NextSessionProtectionCommitmentRepository;
import com.tradepilot.security.JwtService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.util.*;

@Service
public class DevDemoDataService {
    private static final Logger log = LoggerFactory.getLogger(DevDemoDataService.class);
    private static final int DEMO_DATA_VERSION = 5;
    private static final String DEMO_TRADE_KEY_PREFIX = "DEV-DEMO-";
    private static final String DEMO_TRADE_VERSIONED_KEY_PREFIX = DEMO_TRADE_KEY_PREFIX + "v" + DEMO_DATA_VERSION + "-";
    private static final int DEMO_TRADE_COUNT = 190;
    private static final int REPLAY_COMPATIBLE_TRADE_COUNT = 10;
    private static final String E2E_PRO_EMAIL = "selenium-e2e@tradepilot.test";
    private static final String E2E_FREE_EMAIL = "selenium-free-e2e@tradepilot.test";
    private static final String E2E_UNVERIFIED_EMAIL = "selenium-unverified-e2e@tradepilot.test";
    private static final String E2E_PASSWORD = "Selenium-E2E-Only-Password-123!";
    private static final String E2E_TRADE_KEY_PREFIX = "SELENIUM-E2E-";
    private static final BigDecimal PNL_TOLERANCE = new BigDecimal("0.01");
    private static final BigDecimal MAX_DEMO_PRICE_MOVE = new BigDecimal("100.00");
    private static final List<DemoTradeFixture> REPLAY_COMPATIBLE_FIXTURES = List.of(
            // Verified against GLBX.MDP3 ohlcv-1m, MNQM6, 2026-06-04 14:12 America/Chicago.
            new DemoTradeFixture("MNQ", "MNQM6", LocalDate.of(2026, 6, 4), LocalTime.of(14, 12), 6, "SHORT", 3, new BigDecimal("30555.00"), new BigDecimal("30535.00"), new BigDecimal("120.00"), SetupType.FVG, SessionType.NY_PM, true, "MNQ short winner from a failed continuation above afternoon liquidity.", "Clean short execution; held through the planned push lower.", "Repeat the patient entry after failed continuation.", historical(c("30553.75", "30560.25", "30549.00", "30550.50"), c("30536.00", "30538.75", "30530.75", "30534.00"), "30560.25", "30530.75", "20.00", "24.25", "5.25")),
            // Verified against GLBX.MDP3 ohlcv-1m, MNQM6, 2026-06-08 05:25 America/Chicago.
            new DemoTradeFixture("MNQ", "MNQM6", LocalDate.of(2026, 6, 8), LocalTime.of(5, 25), 3, "SHORT", 3, new BigDecimal("29190.00"), new BigDecimal("29178.00"), new BigDecimal("90.00"), SetupType.LIQUIDITY_SWEEP, SessionType.LONDON, true, "MNQ London short winner after liquidity sweep.", "Patient entry and calm management into the planned push lower.", "Keep requiring displacement before holding early-session shorts.", historical(c("29188.50", "29196.25", "29180.50", "29185.00"), c("29180.00", "29187.00", "29175.75", "29181.75"), "29196.25", "29175.75", "12.00", "14.25", "6.25")),
            // Verified against GLBX.MDP3 ohlcv-1m, MNQU6, 2026-06-24 14:33 America/Chicago.
            new DemoTradeFixture("MNQ", "MNQU6", LocalDate.of(2026, 6, 24), LocalTime.of(14, 33), 11, "SHORT", 3, new BigDecimal("29395.00"), new BigDecimal("29380.00"), new BigDecimal("90.00"), SetupType.LIQUIDITY_SWEEP, SessionType.NY_PM, true, "MNQ September-contract short after June expiration.", "Clean sweep-and-fade trade using the date-correct contract.", "Confirm contract month after quarterly expiration.", historical(c("29391.25", "29401.50", "29379.00", "29388.00"), c("29378.50", "29400.00", "29373.00", "29387.50"), "29447.00", "29357.00", "15.00", "38.00", "52.00")),
            // Verified against GLBX.MDP3 ohlcv-1m, MNQU6, 2026-07-08 09:54 America/Chicago.
            new DemoTradeFixture("MNQ", "MNQU6", LocalDate.of(2026, 7, 8), LocalTime.of(9, 54), 2, "LONG", 3, new BigDecimal("29255.00"), new BigDecimal("29280.00"), new BigDecimal("90.00"), SetupType.RECLAIM, SessionType.NY_AM, true, "MNQ long winner after trend reclaim.", "Good patience and clean follow-through into target.", "Trust the plan when reclaim holds above structure.", historical(c("29265.00", "29273.50", "29237.50", "29245.50"), c("29258.50", "29291.75", "29254.50", "29256.75"), "29291.75", "29237.50", "25.00", "36.75", "17.50")),
            // Verified against GLBX.MDP3 ohlcv-1m, MESM6, 2026-06-10 14:19 America/Chicago.
            new DemoTradeFixture("MES", "MESM6", LocalDate.of(2026, 6, 10), LocalTime.of(14, 19), 1, "SHORT", 3, new BigDecimal("7285.25"), new BigDecimal("7286.00"), new BigDecimal("150.00"), SetupType.ORB, SessionType.NY_PM, true, "MES short loser after the opening range reclaim held.", "The short thesis failed when buyers reclaimed the range.", "Respect reclaim strength and exit without adding.", historical(c("7285.75", "7288.00", "7285.00", "7286.25"), c("7286.00", "7286.25", "7282.00", "7283.25"), "7288.00", "7282.00", "-0.75", "3.25", "2.75")),
            // Verified against GLBX.MDP3 ohlcv-1m, MESU6, 2026-06-30 10:04 America/Chicago.
            new DemoTradeFixture("MES", "MESU6", LocalDate.of(2026, 6, 30), LocalTime.of(10, 4), 9, "LONG", 2, new BigDecimal("7529.00"), new BigDecimal("7536.00"), new BigDecimal("100.00"), SetupType.FVG, SessionType.NY_AM, true, "MES September-contract long after June expiration.", "Patient long entry from the morning imbalance.", "Keep size steady on date-correct contract setups.", historical(c("7529.75", "7530.00", "7527.25", "7529.25"), c("7533.75", "7536.50", "7533.50", "7536.50"), "7536.50", "7525.25", "7.00", "7.50", "3.75")),
            // Verified against GLBX.MDP3 ohlcv-1m, MESU6, 2026-07-08 05:45 America/Chicago.
            new DemoTradeFixture("MES", "MESU6", LocalDate.of(2026, 7, 8), LocalTime.of(5, 45), 4, "SHORT", 3, new BigDecimal("7489.25"), new BigDecimal("7485.25"), new BigDecimal("120.00"), SetupType.ORB, SessionType.LONDON, true, "MES short winner from early-session rejection.", "Good execution with a clean rejection and controlled risk.", "Do not chase after the planned target is reached.", historical(c("7487.75", "7489.75", "7487.50", "7489.00"), c("7486.25", "7486.50", "7484.75", "7486.50"), "7490.00", "7484.75", "4.00", "4.50", "0.75")),
            // Verified against GLBX.MDP3 ohlcv-1m, MESU6, 2026-07-09 13:18 America/Chicago.
            new DemoTradeFixture("MES", "MESU6", LocalDate.of(2026, 7, 9), LocalTime.of(13, 18), 12, "LONG", 2, new BigDecimal("7591.00"), new BigDecimal("7589.00"), new BigDecimal("100.00"), SetupType.IFVG, SessionType.NY_PM, true, "MES long loser as afternoon acceptance failed.", "A valid setup failed quickly; the loss stayed contained.", "Avoid pressing NY PM when acceptance is weak.", historical(c("7591.50", "7591.75", "7590.25", "7591.00"), c("7589.50", "7590.50", "7588.50", "7588.75"), "7592.25", "7587.25", "-2.00", "1.25", "3.75")),
            // Verified against GLBX.MDP3 ohlcv-1m, MESU6, 2026-08-06 03:35 America/Chicago.
            new DemoTradeFixture("MES", "MESU6", LocalDate.of(2026, 8, 6), LocalTime.of(3, 35), 1, "SHORT", 2, new BigDecimal("7762.50"), new BigDecimal("7763.00"), new BigDecimal("100.00"), SetupType.LIQUIDITY_SWEEP, SessionType.LONDON, true, "MES London short loser after sweep failed to continue.", "The idea was valid, but sellers did not follow through.", "Require displacement before holding London shorts.", historical(c("7763.00", "7763.75", "7762.25", "7762.75"), c("7762.50", "7763.25", "7762.25", "7763.00"), "7763.75", "7762.25", "-0.50", "0.25", "1.25")),
            // Verified against GLBX.MDP3 ohlcv-1m, MNQU6, 2026-08-04 09:54 America/Chicago.
            new DemoTradeFixture("MNQ", "MNQU6", LocalDate.of(2026, 8, 4), LocalTime.of(9, 54), 16, "LONG", 3, new BigDecimal("29545.00"), new BigDecimal("29590.00"), new BigDecimal("72.00"), SetupType.RECLAIM, SessionType.NY_AM, true, "MNQ replay-ready long after trend reclaim.", "Good patience and clean follow-through into target.", "Trust the plan when reclaim holds above structure.", historical(c("29539.75", "29550.50", "29535.00", "29544.75"), c("29594.50", "29597.00", "29587.50", "29593.75"), "29597.00", "29523.25", "45.00", "52.00", "21.75"))
    );
    private static final List<DemoTradeFixture> GENERAL_DEMO_FIXTURES = List.of(
            new DemoTradeFixture("MES", "MESM6", LocalDate.of(2026, 6, 11), LocalTime.of(9, 36), 14, "LONG", 2, new BigDecimal("5320.00"), new BigDecimal("5330.00"), new BigDecimal("100.00"), SetupType.RECLAIM, SessionType.NY_AM, false, "MES long winner after reclaim and hold.", "Clean execution into target rotation.", "Keep using reclaim confirmation before entering."),
            new DemoTradeFixture("MNQ", "MNQM6", LocalDate.of(2026, 6, 5), LocalTime.of(9, 48), 10, "LONG", 2, new BigDecimal("18820.25"), new BigDecimal("18835.25"), new BigDecimal("60.00"), SetupType.IFVG, SessionType.NY_AM, false, "MNQ long winner after reclaim attempt.", "Patient entry and clean management into target.", "Keep waiting for acceptance before holding the second push."),
            new DemoTradeFixture("MNQ", "MNQM6", LocalDate.of(2026, 6, 12), LocalTime.of(10, 54), 7, "SHORT", 3, new BigDecimal("18839.25"), new BigDecimal("18820.25"), new BigDecimal("114.00"), SetupType.FVG, SessionType.NY_AM, false, "MNQ short winner from synthetic trend continuation.", "Good patience and clean follow-through into target.", "Repeat the patient entry after failed continuation."),
            new DemoTradeFixture("MES", "MESM6", LocalDate.of(2026, 6, 16), LocalTime.of(9, 30), 15, "LONG", 1, new BigDecimal("5000.00"), new BigDecimal("5010.00"), new BigDecimal("50.00"), SetupType.FVG, SessionType.NY_AM, false, "MES long winner from synthetic morning continuation.", "Clean execution into target rotation.", "Keep using confirmation before entering."),
            new DemoTradeFixture("MES", "MESU6", LocalDate.of(2026, 6, 30), LocalTime.of(10, 42), 9, "SHORT", 3, new BigDecimal("5360.75"), new BigDecimal("5381.75"), new BigDecimal("315.00"), SetupType.FVG, SessionType.NY_AM, false, "MES short loser after synthetic reclaim held.", "The idea was valid, but sellers did not follow through.", "Respect reclaim strength and exit without adding."),
            new DemoTradeFixture("MNQ", "MNQU6", LocalDate.of(2026, 7, 15), LocalTime.of(9, 35), 7, "SHORT", 2, new BigDecimal("21000.00"), new BigDecimal("20990.00"), new BigDecimal("40.00"), SetupType.LIQUIDITY_SWEEP, SessionType.NY_AM, false, "MNQ short winner from synthetic liquidity sweep.", "Clean sweep-and-fade trade.", "Keep size steady on date-correct contract setups."),
            new DemoTradeFixture("MES", "MESU6", LocalDate.of(2026, 7, 22), LocalTime.of(13, 18), 12, "LONG", 2, new BigDecimal("5420.00"), new BigDecimal("5412.00"), new BigDecimal("80.00"), SetupType.IFVG, SessionType.NY_PM, false, "MES long loser as synthetic afternoon acceptance failed.", "A valid setup failed quickly; the loss stayed contained.", "Avoid pressing NY PM when acceptance is weak."),
            new DemoTradeFixture("MNQ", "MNQU6", LocalDate.of(2026, 8, 4), LocalTime.of(9, 54), 16, "LONG", 3, new BigDecimal("18860.00"), new BigDecimal("18878.00"), new BigDecimal("108.00"), SetupType.RECLAIM, SessionType.NY_AM, false, "MNQ long winner after synthetic trend reclaim.", "Good patience and clean follow-through into target.", "Trust the plan when reclaim holds above structure."),
            new DemoTradeFixture("MES", "MESU6", LocalDate.of(2026, 8, 6), LocalTime.of(3, 35), 10, "SHORT", 2, new BigDecimal("5376.00"), new BigDecimal("5384.00"), new BigDecimal("80.00"), SetupType.LIQUIDITY_SWEEP, SessionType.LONDON, false, "MES London short loser after synthetic sweep failed.", "The idea was valid, but sellers did not follow through.", "Require displacement before holding London shorts."),
            new DemoTradeFixture("MNQ", "MNQU6", LocalDate.of(2026, 8, 7), LocalTime.of(14, 33), 11, "SHORT", 3, new BigDecimal("18842.75"), new BigDecimal("18830.75"), new BigDecimal("72.00"), SetupType.ORB, SessionType.NY_PM, false, "MNQ short winner from synthetic afternoon rejection.", "Clean execution through planned push lower.", "Do not chase after the planned target is reached.")
    );

    private static DemoHistoricalVerification historical(VerifiedCandle entryCandle, VerifiedCandle exitCandle, String windowMaxHigh, String windowMinLow, String realizedPoints, String mfePoints, String maePoints) {
        return new DemoHistoricalVerification(entryCandle, exitCandle, bd(windowMaxHigh), bd(windowMinLow), bd(realizedPoints), bd(mfePoints), bd(maePoints));
    }

    private static VerifiedCandle c(String open, String high, String low, String close) {
        return new VerifiedCandle(bd(open), bd(high), bd(low), bd(close));
    }

    private static BigDecimal bd(String value) {
        return new BigDecimal(value);
    }

    static List<DemoReplayCalibration> replayCalibrationExpectationsForTesting() {
        return REPLAY_COMPATIBLE_FIXTURES.stream()
                .map(fixture -> {
                    DemoHistoricalVerification verified = fixture.historicalVerification();
                    BigDecimal pnl = fixture.expectedGrossPnl();
                    return new DemoReplayCalibration(
                            fixture.symbol(),
                            fixture.contractSymbol(),
                            fixture.tradeDate(),
                            fixture.entryTime(),
                            fixture.entryTime().plusMinutes(fixture.durationMinutes()),
                            fixture.side(),
                            fixture.quantity(),
                            fixture.entryPrice(),
                            fixture.exitPrice(),
                            fixture.riskAmount(),
                            pnl,
                            calculateRMultiple(pnl, fixture.riskAmount()),
                            verified.entryCandle(),
                            verified.exitCandle(),
                            verified.windowMaxHigh(),
                            verified.windowMinLow(),
                            verified.expectedRealizedPoints(),
                            verified.expectedMfePoints(),
                            verified.expectedMaePoints()
                    );
                })
                .toList();
    }
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
    private final FuturesContractCalendar contractCalendar;
    private final FuturesPointValueService pointValueService;
    private final TradeReplayRepository tradeReplayRepository;
    private final TradeReplayInsightRepository tradeReplayInsightRepository;
    private final TradeMemoryRepository tradeMemoryRepository;
    private final TradeMemoryIndexService tradeMemoryIndexService;
    private final PasswordEncoder passwordEncoder;
    private final NextSessionProtectionCommitmentRepository protectionCommitmentRepository;
    private final JwtService jwtService;
    private final AccountSurvivalTimeSource accountSurvivalTimeSource;

    @Transactional
    public Map<String, Object> prepareAccountSurvivalE2eData() {
        User owner = survivalUser("survival-e2e-owner@tradepilot.test", "Account Survival E2E Owner", PlanType.PRO);
        User other = survivalUser("survival-e2e-other@tradepilot.test", "Account Survival E2E Other", PlanType.FREE);
        TradingAccount source = survivalAccount(owner, "Topstep 50K A", PropFirmType.TOPSTEP, "SURVIVAL-E2E-A");
        TradingAccount later = survivalAccount(owner, "Apex 50K B", PropFirmType.OTHER, "SURVIVAL-E2E-B");
        LocalDate first = LocalDate.of(2026, 10, 1);
        accountSurvivalTimeSource.resetE2eDate(first);
        for (int day = 1; day <= 5; day++) tradeRepository.save(survivalTrade(owner, source, first.minusDays(day), "NQ", "SHORT", "10:00:05", "10:04:05", "-400", 2, "LOSS-" + day));
        tradeRepository.save(survivalTrade(owner, source, first, "NQ", "SHORT", "10:00:05", "10:04:05", "-240", 2, "SOURCE"));
        tradeRepository.save(survivalTrade(owner, later, first, "MNQ", "SHORT", "10:12:05", "10:18:05", "-540", 20, "LATER"));
        // The real-browser acceptance test imports its later evaluation session through
        // the supported CSV endpoint, rather than bypassing the import pipeline here.
        NextSessionProtectionCommitment commitment = new NextSessionProtectionCommitment();
        commitment.setUser(owner); commitment.setAccount(source); commitment.setEffectiveSessionDate(first);
        commitment.setMaxNormalizedExposure(new BigDecimal("20")); commitment.setMaxAttempts(2);
        commitment.setMaxSessionLoss(new BigDecimal("200")); commitment.setPostLossPauseMinutes(20);
        commitment.setAllowedWindowStart(LocalTime.of(9, 30)); commitment.setAllowedWindowEnd(LocalTime.NOON);
        commitment.setRequireFreshSetupConfirmation(true); commitment.setSource(ProtectionCommitmentSource.SYSTEM_RECOMMENDED);
        commitment.setStatus(ProtectionCommitmentStatus.DRAFT);
        protectionCommitmentRepository.save(commitment);
        return Map.of("ownerToken", jwtService.generateToken(owner), "otherToken", jwtService.generateToken(other), "owner", Map.of("id", owner.getId(), "name", owner.getName(), "email", owner.getEmail(), "plan", owner.getPlan().name(), "role", owner.getRole().name()), "sourceAccountId", source.getId(), "laterAccountId", later.getId(), "sessionDate", first.toString());
    }

    public Map<String, Object> advanceAccountSurvivalE2eDate(LocalDate date) {
        accountSurvivalTimeSource.advanceE2eDate(date);
        return Map.of("date", date.toString());
    }

    private User survivalUser(String email, String name, PlanType plan) { return userRepository.save(User.builder().name(name).email(email.replace("@", "+" + UUID.randomUUID() + "@")).passwordHash(passwordEncoder.encode("test-only")).emailVerified(true).plan(plan).role(UserRole.USER).subscriptionStatus(SubscriptionStatus.ACTIVE).build()); }
    private TradingAccount survivalAccount(User user, String name, PropFirmType firm, String number) { return accountRepository.save(TradingAccount.builder().user(user).name(name).propFirm(firm).accountNumber(number + "-" + UUID.randomUUID()).accountType(TradingAccountType.FUNDED).startingBalance(new BigDecimal("50000")).status(TradingAccountStatus.ACTIVE).build()); }
    private Trade survivalTrade(User user, TradingAccount account, LocalDate date, String symbol, String side, String entry, String exit, String pnl, int quantity, String key) { return Trade.builder().user(user).account(account).symbol(symbol).market("FUTURES").tradeDate(date).entryTime(LocalDateTime.parse(date + "T" + entry)).exitTime(LocalDateTime.parse(date + "T" + exit)).side(side).session(SessionType.NY_AM).quantity(quantity).pnlAmount(new BigDecimal(pnl)).outcome(new BigDecimal(pnl).signum() < 0 ? OutcomeType.LOSS : OutcomeType.BE).externalTradeKey("SURVIVAL-E2E-" + UUID.randomUUID() + "-" + key).build(); }

    @Transactional
    public E2eTestDataResponse prepareE2eTestData() {
        E2eTestDataResponse.E2eUserData proUser = prepareE2eUser(
                E2E_PRO_EMAIL,
                "Selenium E2E Pro User",
                PlanType.PRO,
                SubscriptionStatus.ACTIVE,
                true
        );
        E2eTestDataResponse.E2eUserData freeUser = prepareE2eUser(
                E2E_FREE_EMAIL,
                "Selenium E2E Free User",
                PlanType.FREE,
                SubscriptionStatus.NONE,
                true
        );
        E2eTestDataResponse.E2eUserData unverifiedUser = prepareE2eUser(
                E2E_UNVERIFIED_EMAIL,
                "Selenium E2E Unverified User",
                PlanType.FREE,
                SubscriptionStatus.NONE,
                false
        );
        return new E2eTestDataResponse(proUser, freeUser, unverifiedUser, "Deterministic Selenium E2E data prepared.");
    }

    private E2eTestDataResponse.E2eUserData prepareE2eUser(
            String email,
            String name,
            PlanType plan,
            SubscriptionStatus subscriptionStatus,
            boolean emailVerified
    ) {
        User user = userRepository.findByEmail(email).orElseGet(() -> User.builder()
                .name(name)
                .email(email)
                .passwordHash(passwordEncoder.encode(E2E_PASSWORD))
                .emailVerified(emailVerified)
                .plan(plan)
                .role(UserRole.USER)
                .subscriptionStatus(subscriptionStatus)
                .build());
        user.setName(name);
        user.setPasswordHash(passwordEncoder.encode(E2E_PASSWORD));
        user.setEmailVerified(emailVerified);
        user.setPlan(plan);
        user.setRole(UserRole.USER);
        user.setSubscriptionStatus(subscriptionStatus);
        user.setStripeCustomerId(null);
        user.setStripeSubscriptionId(null);
        user.setStripeCancelAtPeriodEnd(false);
        user.setStripeCancelAt(null);
        user.setStripeCanceledAt(null);
        user.setPurchasedAiCreditsBalance(0);
        User savedUser = userRepository.save(user);

        clear(savedUser);

        TradingAccount primary = accountRepository.save(TradingAccount.builder()
                .user(savedUser)
                .name("Primary E2E Account")
                .propFirm(PropFirmType.TOPSTEP)
                .accountNumber("SELENIUM-E2E-PRIMARY")
                .accountType(TradingAccountType.EVALUATION)
                .startingBalance(new BigDecimal("50000.00"))
                .defaultRiskAmount(new BigDecimal("100.00"))
                .status(TradingAccountStatus.ACTIVE)
                .build());
        TradingAccount secondary = accountRepository.save(TradingAccount.builder()
                .user(savedUser)
                .name("Secondary E2E Account")
                .propFirm(PropFirmType.OTHER)
                .accountNumber("SELENIUM-E2E-SECONDARY")
                .accountType(TradingAccountType.PERSONAL)
                .startingBalance(new BigDecimal("25000.00"))
                .defaultRiskAmount(new BigDecimal("75.00"))
                .status(TradingAccountStatus.ACTIVE)
                .build());

        List<Trade> trades = tradeRepository.saveAll(List.of(
                e2eTrade(savedUser, primary, "PRIMARY", LocalDate.of(2026, 6, 4), LocalTime.of(14, 12),
                        "MNQ", "MNQM6", "SHORT", 3, "30555.00", "30535.00", "120.00",
                        SetupType.FVG, SessionType.NY_PM, "Deterministic E2E MNQ short winner."),
                e2eTrade(savedUser, primary, "SIMILAR-ONE", LocalDate.of(2026, 6, 8), LocalTime.of(5, 25),
                        "MNQ", "MNQM6", "SHORT", 3, "29190.00", "29178.00", "90.00",
                        SetupType.LIQUIDITY_SWEEP, SessionType.LONDON, "Deterministic E2E MNQ London short winner."),
                e2eTrade(savedUser, secondary, "SECONDARY", LocalDate.of(2026, 7, 8), LocalTime.of(9, 54),
                        "MES", "MESU6", "LONG", 2, "7529.00", "7536.00", "100.00",
                        SetupType.FVG, SessionType.NY_AM, "Deterministic E2E MES long winner.")
        ));

        return new E2eTestDataResponse.E2eUserData(
                savedUser.getId(),
                savedUser.getEmail(),
                E2E_PASSWORD,
                primary.getId(),
                secondary.getId(),
                trades.get(0).getId()
        );
    }

    private Trade e2eTrade(
            User user,
            TradingAccount account,
            String keySuffix,
            LocalDate tradeDate,
            LocalTime entryTime,
            String symbol,
            String contractSymbol,
            String side,
            int quantity,
            String entryPrice,
            String exitPrice,
            String riskAmount,
            SetupType setup,
            SessionType session,
            String notes
    ) {
        BigDecimal entry = new BigDecimal(entryPrice);
        BigDecimal exit = new BigDecimal(exitPrice);
        BigDecimal risk = new BigDecimal(riskAmount);
        BigDecimal pnl = calculateGrossPnl(symbol, side, entry, exit, quantity, pointValueService);
        Trade trade = Trade.builder()
                .user(user)
                .account(account)
                .symbol(symbol)
                .market("FUTURES")
                .tradeDate(tradeDate)
                .entryTime(LocalDateTime.of(tradeDate, entryTime))
                .exitTime(LocalDateTime.of(tradeDate, entryTime.plusMinutes(6)))
                .side(side)
                .setup(setup)
                .session(session)
                .entryPrice(entry)
                .stopPrice(stopPrice(side, entry))
                .targetPrice(targetPrice(side, entry))
                .exitPrice(exit)
                .quantity(quantity)
                .riskAmount(risk)
                .pnlAmount(pnl)
                .pnlR(calculateRMultiple(pnl, risk))
                .outcome(pnl.signum() > 0 ? OutcomeType.WIN : pnl.signum() < 0 ? OutcomeType.LOSS : OutcomeType.BE)
                .confidenceRating(8)
                .confidence(8)
                .emotion("Focused")
                .executionRating(8)
                .followedPlan(true)
                .mistakeTag(MistakeTag.NONE)
                .notes(notes)
                .psychologyNotes("Synthetic Selenium fixture; calm execution and controlled risk.")
                .lessonLearned("Keep waiting for confirmation before entry.")
                .externalTradeKey(E2E_TRADE_KEY_PREFIX + user.getId() + "-" + keySuffix)
                .build();
        trade.setContractSymbol(contractSymbol);
        return withDemoContract(trade);
    }

    @Transactional
    public Map<String, Object> seed(User user) {
        clear(user);
        TradingAccount topstep = accountRepository.save(TradingAccount.builder().user(user).name("Topstep Evaluation").propFirm(PropFirmType.TOPSTEP).accountNumber("TS-DEMO-50K").accountType(TradingAccountType.EVALUATION).startingBalance(BigDecimal.valueOf(50000)).status(TradingAccountStatus.ACTIVE).build());
        TradingAccount personal = accountRepository.save(TradingAccount.builder().user(user).name("Personal Tradovate").propFirm(PropFirmType.OTHER).accountNumber("TV-DEMO-LIVE").accountType(TradingAccountType.PERSONAL).startingBalance(BigDecimal.valueOf(25000)).status(TradingAccountStatus.ACTIVE).build());
        List<Trade> trades = seedTrades(user, List.of(topstep, personal));
        seedAiReviews(user, trades);
        seedCoachSnapshots(user, topstep, trades);
        seedTradingDna(user, trades);
        seedDiscipline(user, trades);
        seedUsageEvents(user);
        Map<String, Object> featureUsage = backfillFeatureUsage(user);
        return Map.of("accountsCreated", 2, "tradesCreated", trades.size(), "aiReviewsCreated", Math.min(18, trades.size()), "featureUsageEventsCreated", featureUsage.get("featureUsageEventsCreated"), "message", "Demo data seeded for current user.");
    }

    @Transactional
    public DemoDataReseedResult reseedDemoFixtures(User user, boolean dryRun) {
        validateDemoFixtures();
        List<Trade> existingDemoTrades = tradeRepository.findByUserAndExternalTradeKeyStartingWithOrderByExternalTradeKeyAsc(user, DEMO_TRADE_KEY_PREFIX);
        List<Long> tradeIds = existingDemoTrades.stream().map(Trade::getId).filter(Objects::nonNull).toList();
        List<TradeReplay> replays = tradeIds.isEmpty() ? List.of() : tradeReplayRepository.findByUserIdAndTrade_IdIn(user.getId(), tradeIds);
        List<Long> replayIds = replays.stream().map(TradeReplay::getId).filter(Objects::nonNull).toList();

        long reviewCount = tradeIds.isEmpty() ? 0 : reviewRepository.countByUserIdAndTradeIdIn(user.getId(), tradeIds);
        long analysisCount = tradeIds.isEmpty() ? 0 : analysisRepository.countByUserIdAndTradeIdIn(user.getId(), tradeIds);
        long replayInsightCount = replayIds.isEmpty() ? 0 : tradeReplayInsightRepository.countByReplayIdIn(replayIds);
        long memoryCount = tradeIds.isEmpty() ? 0 : tradeMemoryRepository.findByUserIdAndTradeIdIn(user.getId(), tradeIds).size();
        long violationCount = tradeIds.isEmpty() ? 0 : violationRepository.countByUserIdAndTradeIdIn(user.getId(), tradeIds);
        List<Integer> existingVersions = detectDemoDataVersions(existingDemoTrades);
        List<TradingAccount> accounts = accountRepository.findByUserOrderByCreatedAtDesc(user);
        int accountsToCreate = accounts.isEmpty() ? 2 : 0;

        if (dryRun) {
            return new DemoDataReseedResult(
                    true,
                    DEMO_DATA_VERSION,
                    existingVersions,
                    DEMO_TRADE_KEY_PREFIX,
                    existingDemoTrades.size(),
                    existingDemoTrades.size(),
                    DEMO_TRADE_COUNT,
                    reviewCount,
                    analysisCount,
                    replays.size(),
                    replayInsightCount,
                    memoryCount,
                    violationCount,
                    0,
                    0,
                    0,
                    0,
                    0,
                    accountsToCreate,
                    "Dry run complete. No demo fixture rows were changed."
            );
        }

        if (!replayIds.isEmpty()) {
            tradeReplayInsightRepository.deleteByReplayIdIn(replayIds);
        }
        if (!tradeIds.isEmpty()) {
            tradeReplayRepository.deleteByUserIdAndTrade_IdIn(user.getId(), tradeIds);
            reviewRepository.deleteByUserIdAndTradeIdIn(user.getId(), tradeIds);
            analysisRepository.deleteByUserIdAndTradeIdIn(user.getId(), tradeIds);
            violationRepository.deleteByUserIdAndTradeIdIn(user.getId(), tradeIds);
            for (Long tradeId : tradeIds) {
                tradeMemoryIndexService.deleteMemory(user.getId(), tradeId);
            }
            tradeRepository.deleteAll(existingDemoTrades);
        }

        DemoAccounts demoAccounts = ensureDemoAccounts(user);
        List<Trade> createdTrades = seedTrades(user, demoAccounts.accounts());
        DemoAiSeedCounts aiCounts = seedAiReviews(user, createdTrades);
        DemoDisciplineSeedCounts disciplineCounts = seedDiscipline(user, createdTrades, false);
        log.info("Dev demo data reseeded. userId={}, dryRun=false, deletedDemoTrades={}, createdDemoTrades={}, version={}",
                user.getId(), existingDemoTrades.size(), createdTrades.size(), DEMO_DATA_VERSION);

        return new DemoDataReseedResult(
                false,
                DEMO_DATA_VERSION,
                existingVersions,
                DEMO_TRADE_KEY_PREFIX,
                existingDemoTrades.size(),
                existingDemoTrades.size(),
                DEMO_TRADE_COUNT,
                reviewCount,
                analysisCount,
                replays.size(),
                replayInsightCount,
                memoryCount,
                violationCount,
                existingDemoTrades.size(),
                createdTrades.size(),
                aiCounts.reviewsCreated(),
                aiCounts.analysesCreated(),
                disciplineCounts.violationsCreated(),
                demoAccounts.accountsCreated(),
                "Demo fixture data reseeded for current user."
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
        reviewRepository.findByUserId(userId).forEach(review -> events.add(featureEvent(userId, FeatureKey.AI_REVIEW, toLocalDateTime(review.getCreatedAt()))));
        coachSnapshotRepository.findByUserId(userId).forEach(snapshot -> {
            FeatureKey feature = snapshot.getSnapshotType() == CoachSnapshotType.WEEKLY ? FeatureKey.WEEKLY_COACH : FeatureKey.DAILY_COACH;
            events.add(featureEvent(userId, feature, snapshot.getCreatedAt()));
        });
        tradingDnaSnapshotRepository.findByUserId(userId).forEach(snapshot -> events.add(featureEvent(userId, FeatureKey.TRADING_DNA, snapshot.getCreatedAt())));
        sessionReviewRepository.findByUserId(userId).forEach(review -> events.add(featureEvent(userId, FeatureKey.SESSION_REVIEW, review.getCreatedAt())));
        featureUsageRepository.saveAll(events);
        return Map.of("featureUsageEventsCreated", events.size(), "message", "Feature usage backfilled for current user.");
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
        validateDemoFixtures();
        for (int i = 0; i < DEMO_TRADE_COUNT; i++) {
            boolean replayCompatible = i < REPLAY_COMPATIBLE_TRADE_COUNT;
            int fixtureIndex = replayCompatible ? i : (i - REPLAY_COMPATIBLE_TRADE_COUNT) % GENERAL_DEMO_FIXTURES.size();
            DemoTradeFixture fixture = replayCompatible ? REPLAY_COMPATIBLE_FIXTURES.get(fixtureIndex) : GENERAL_DEMO_FIXTURES.get(fixtureIndex);
            int repeat = replayCompatible ? 0 : (i - REPLAY_COMPATIBLE_TRADE_COUNT) / GENERAL_DEMO_FIXTURES.size();
            TradingAccount account = accounts.get(i % accounts.size());
            BigDecimal priceOffset = replayCompatible
                    ? BigDecimal.ZERO
                    : BigDecimal.valueOf((repeat % 5) - 2L).multiply(new BigDecimal("0.25"));
            BigDecimal entryPrice = fixture.entryPrice().add(priceOffset);
            BigDecimal exitPrice = fixture.exitPrice().add(priceOffset);
            BigDecimal pnl = calculateGrossPnl(fixture.symbol(), fixture.side(), entryPrice, exitPrice, fixture.quantity(), pointValueService);
            BigDecimal pnlR = calculateRMultiple(pnl, fixture.riskAmount());
            LocalDateTime entry = LocalDateTime.of(fixture.tradeDate(), fixture.entryTime());
            LocalDateTime exit = entry.plusMinutes(fixture.durationMinutes());
            boolean winner = pnl.signum() > 0;
            Trade trade = Trade.builder()
                    .user(user)
                    .account(account)
                    .symbol(fixture.symbol())
                    .market("FUTURES")
                    .tradeDate(fixture.tradeDate())
                    .entryTime(entry)
                    .exitTime(exit)
                    .side(fixture.side())
                    .setup(fixture.setup())
                    .session(fixture.session())
                    .entryPrice(entryPrice)
                    .stopPrice(stopPrice(fixture.side(), entryPrice))
                    .targetPrice(targetPrice(fixture.side(), entryPrice))
                    .exitPrice(exitPrice)
                    .quantity(fixture.quantity())
                    .riskAmount(fixture.riskAmount())
                    .pnlAmount(pnl)
                    .pnlR(pnlR)
                    .outcome(winner ? OutcomeType.WIN : pnl.signum() < 0 ? OutcomeType.LOSS : OutcomeType.BE)
                    .confidenceRating(winner ? 8 : 5)
                    .executionRating(winner ? 8 : 5)
                    .followedPlan(winner)
                    .mistakeTag(winner ? MistakeTag.NONE : MistakeTag.NO_CONFIRMATION)
                    .notes(fixture.notes())
                    .psychologyNotes(fixture.psychologyNotes())
                    .lessonLearned(fixture.lessonLearned())
                    .externalTradeKey(demoTradeKey(user, fixture, i))
                    .build();
            trade.setContractSymbol(fixture.contractSymbol());
            trades.add(withDemoContract(trade));
        }
        log.info("Demo fixture validation complete. version=v{}, fixtureCount={}, replayCompatibleFixtures={}, invalidFixtures=0, generatedTrades={}",
                DEMO_DATA_VERSION, REPLAY_COMPATIBLE_FIXTURES.size() + GENERAL_DEMO_FIXTURES.size(), REPLAY_COMPATIBLE_TRADE_COUNT, trades.size());
        return tradeRepository.saveAll(trades);
    }

    private DemoAccounts ensureDemoAccounts(User user) {
        List<TradingAccount> accounts = accountRepository.findByUserOrderByCreatedAtDesc(user);
        if (!accounts.isEmpty()) {
            return new DemoAccounts(accounts, 0);
        }
        TradingAccount topstep = accountRepository.save(TradingAccount.builder().user(user).name("Topstep Evaluation").propFirm(PropFirmType.TOPSTEP).accountNumber("TS-DEMO-50K").accountType(TradingAccountType.EVALUATION).startingBalance(BigDecimal.valueOf(50000)).status(TradingAccountStatus.ACTIVE).build());
        TradingAccount personal = accountRepository.save(TradingAccount.builder().user(user).name("Personal Tradovate").propFirm(PropFirmType.OTHER).accountNumber("TV-DEMO-LIVE").accountType(TradingAccountType.PERSONAL).startingBalance(BigDecimal.valueOf(25000)).status(TradingAccountStatus.ACTIVE).build());
        return new DemoAccounts(List.of(topstep, personal), 2);
    }

    private String demoTradeKey(User user, DemoTradeFixture fixture, int index) {
        return DEMO_TRADE_VERSIONED_KEY_PREFIX + (fixture.replayCompatible() ? "REPLAY-" : "GENERAL-") + user.getId() + "-" + index;
    }

    private List<Integer> detectDemoDataVersions(List<Trade> trades) {
        return trades.stream()
                .map(Trade::getExternalTradeKey)
                .filter(Objects::nonNull)
                .map(this::detectDemoDataVersion)
                .distinct()
                .sorted()
                .toList();
    }

    private int detectDemoDataVersion(String key) {
        if (key.startsWith(DEMO_TRADE_VERSIONED_KEY_PREFIX)) {
            return DEMO_DATA_VERSION;
        }
        if (key.startsWith(DEMO_TRADE_KEY_PREFIX + "v")) {
            int dash = key.indexOf('-', DEMO_TRADE_KEY_PREFIX.length() + 1);
            if (dash > DEMO_TRADE_KEY_PREFIX.length() + 1) {
                try {
                    return Integer.parseInt(key.substring(DEMO_TRADE_KEY_PREFIX.length() + 1, dash));
                } catch (NumberFormatException ignored) {
                    return 1;
                }
            }
        }
        return 1;
    }

    private Trade withDemoContract(Trade trade) {
        contractCalendar.expectedActiveContract(trade.getSymbol(), trade.getTradeDate()).ifPresent(contract -> {
            if (trade.getContractSymbol() != null && !trade.getContractSymbol().equals(contract)) {
                throw new IllegalStateException("Demo fixture contract/date mismatch. symbol=" + trade.getSymbol()
                        + ", tradeDate=" + trade.getTradeDate()
                        + ", fixtureContract=" + trade.getContractSymbol()
                        + ", expectedContract=" + contract);
            }
            trade.setContractSymbol(contract);
            trade.setContractSymbolSource(ContractSymbolSource.ADMIN_BACKFILL.name());
        });
        return trade;
    }

    private BigDecimal stopPrice(String side, BigDecimal entryPrice) {
        return "SHORT".equals(side) ? entryPrice.add(new BigDecimal("10.00")) : entryPrice.subtract(new BigDecimal("10.00"));
    }

    private BigDecimal targetPrice(String side, BigDecimal entryPrice) {
        return "SHORT".equals(side) ? entryPrice.subtract(new BigDecimal("20.00")) : entryPrice.add(new BigDecimal("20.00"));
    }

    private void validateDemoFixtures() {
        List<DemoTradeFixture> fixtures = new ArrayList<>();
        fixtures.addAll(REPLAY_COMPATIBLE_FIXTURES);
        fixtures.addAll(GENERAL_DEMO_FIXTURES);
        if (REPLAY_COMPATIBLE_FIXTURES.size() != REPLAY_COMPATIBLE_TRADE_COUNT) {
            throw new IllegalStateException("Replay-compatible demo fixture count mismatch.");
        }
        for (DemoTradeFixture fixture : fixtures) {
            BigDecimal calculated = calculateGrossPnl(fixture.symbol(), fixture.side(), fixture.entryPrice(), fixture.exitPrice(), fixture.quantity(), pointValueService);
            validateExplicitPnl(fixture, calculated);
            validateTradeMath(fixture.symbol(), fixture.side(), fixture.entryPrice(), fixture.exitPrice(), fixture.quantity(), calculated,
                    calculateRMultiple(calculated, fixture.riskAmount()), pointValueService);
            validateReplayCompatibleFixture(fixture);
        }
    }

    private void validateExplicitPnl(DemoTradeFixture fixture, BigDecimal calculatedPnl) {
        BigDecimal expectedContractPnl = fixture.expectedGrossPnl().setScale(2, RoundingMode.HALF_UP);
        if (expectedContractPnl.subtract(calculatedPnl).abs().compareTo(PNL_TOLERANCE) > 0) {
            throw new IllegalStateException("Demo fixture P&L mismatch. symbol=" + fixture.symbol()
                    + ", contract=" + fixture.contractSymbol()
                    + ", side=" + fixture.side()
                    + ", entry=" + fixture.entryPrice()
                    + ", exit=" + fixture.exitPrice()
                    + ", quantity=" + fixture.quantity()
                    + ", expectedGrossPnl=" + expectedContractPnl
                    + ", calculatedGrossPnl=" + calculatedPnl);
        }
    }

    private void validateReplayCompatibleFixture(DemoTradeFixture fixture) {
        if (fixture.entryPrice().compareTo(BigDecimal.ZERO) <= 0 || fixture.exitPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("Demo fixture prices must be positive. contract=" + fixture.contractSymbol());
        }
        if (fixture.quantity() <= 0) {
            throw new IllegalStateException("Demo fixture quantity must be positive. contract=" + fixture.contractSymbol());
        }
        validateTickSize(fixture.symbol(), fixture.entryPrice(), "entryPrice");
        validateTickSize(fixture.symbol(), fixture.exitPrice(), "exitPrice");
            BigDecimal tradeMove = fixture.entryPrice().subtract(fixture.exitPrice()).abs();
            if (tradeMove.compareTo(MAX_DEMO_PRICE_MOVE) > 0) {
                throw new IllegalStateException("Demo fixture price move is too large for replay-compatible demo data. contract=" + fixture.contractSymbol()
                        + ", move=" + tradeMove);
            }
            if (fixture.replayCompatible()) {
                validateHistoricalReplayFixture(fixture);
            }
            BigDecimal r = calculateRMultiple(calculateGrossPnl(fixture.symbol(), fixture.side(), fixture.entryPrice(), fixture.exitPrice(), fixture.quantity(), pointValueService), fixture.riskAmount());
            if (r.signum() != 0 && r.signum() != calculateGrossPnl(fixture.symbol(), fixture.side(), fixture.entryPrice(), fixture.exitPrice(), fixture.quantity(), pointValueService).signum()) {
                throw new IllegalStateException("Demo fixture R sign contradicts P&L. contract=" + fixture.contractSymbol());
            }
            String expectedContract = contractCalendar.expectedActiveContract(fixture.symbol(), fixture.tradeDate())
                .orElseThrow(() -> new IllegalStateException("Demo fixture contract cannot be resolved. symbol=" + fixture.symbol()));
        if (!expectedContract.equals(fixture.contractSymbol())) {
            throw new IllegalStateException("Demo fixture contract/date mismatch. symbol=" + fixture.symbol()
                    + ", tradeDate=" + fixture.tradeDate()
                    + ", fixtureContract=" + fixture.contractSymbol()
                    + ", expectedContract=" + expectedContract);
        }
    }

    private void validateHistoricalReplayFixture(DemoTradeFixture fixture) {
        DemoHistoricalVerification verified = fixture.historicalVerification();
        if (verified == null) {
            throw new IllegalStateException("Replay-compatible demo fixture requires historical verification. contract=" + fixture.contractSymbol());
        }
        if (!verified.entryCandle().contains(fixture.entryPrice())) {
            throw new IllegalStateException("Replay-compatible demo entry is outside verified entry candle. contract=" + fixture.contractSymbol()
                    + ", entry=" + fixture.entryPrice()
                    + ", entryCandleLow=" + verified.entryCandle().low()
                    + ", entryCandleHigh=" + verified.entryCandle().high());
        }
        if (!verified.exitCandle().contains(fixture.exitPrice())) {
            throw new IllegalStateException("Replay-compatible demo exit is outside verified exit candle. contract=" + fixture.contractSymbol()
                    + ", exit=" + fixture.exitPrice()
                    + ", exitCandleLow=" + verified.exitCandle().low()
                    + ", exitCandleHigh=" + verified.exitCandle().high());
        }
        BigDecimal realized = realizedPoints(fixture.side(), fixture.entryPrice(), fixture.exitPrice());
        BigDecimal mfe = "SHORT".equals(fixture.side())
                ? fixture.entryPrice().subtract(verified.windowMinLow())
                : verified.windowMaxHigh().subtract(fixture.entryPrice());
        BigDecimal mae = "SHORT".equals(fixture.side())
                ? verified.windowMaxHigh().subtract(fixture.entryPrice())
                : fixture.entryPrice().subtract(verified.windowMinLow());
        assertEqual("realized", fixture, realized, verified.expectedRealizedPoints());
        assertEqual("mfe", fixture, maxZero(mfe), verified.expectedMfePoints());
        assertEqual("mae", fixture, maxZero(mae), verified.expectedMaePoints());
    }

    private BigDecimal realizedPoints(String side, BigDecimal entryPrice, BigDecimal exitPrice) {
        return "SHORT".equals(side) ? entryPrice.subtract(exitPrice) : exitPrice.subtract(entryPrice);
    }

    private BigDecimal maxZero(BigDecimal value) {
        return value.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : value;
    }

    private void assertEqual(String field, DemoTradeFixture fixture, BigDecimal actual, BigDecimal expected) {
        if (actual.setScale(2, RoundingMode.HALF_UP).compareTo(expected.setScale(2, RoundingMode.HALF_UP)) != 0) {
            throw new IllegalStateException("Replay-compatible demo " + field + " mismatch. contract=" + fixture.contractSymbol()
                    + ", actual=" + actual
                    + ", expected=" + expected);
        }
    }

    static BigDecimal calculateRMultiple(BigDecimal pnl, BigDecimal riskAmount) {
        if (pnl == null || riskAmount == null || riskAmount.compareTo(BigDecimal.ZERO) == 0) {
            throw new IllegalArgumentException("Risk amount is required to derive demo R multiple.");
        }
        return pnl.divide(riskAmount, 2, RoundingMode.HALF_UP);
    }

    static BigDecimal calculateGrossPnl(String symbol, String side, BigDecimal entryPrice, BigDecimal exitPrice, int quantity, FuturesPointValueService pointValueService) {
        BigDecimal pointValue = pointValueService.pointValueForContract(symbol)
                .orElseThrow(() -> new IllegalArgumentException("Unsupported demo futures symbol: " + symbol));
        BigDecimal pointMove = "SHORT".equals(side)
                ? entryPrice.subtract(exitPrice)
                : exitPrice.subtract(entryPrice);
        return pointMove.multiply(pointValue).multiply(BigDecimal.valueOf(quantity)).setScale(2, RoundingMode.HALF_UP);
    }

    static void validateTradeMath(String symbol, String side, BigDecimal entryPrice, BigDecimal exitPrice, int quantity, BigDecimal storedPnl, BigDecimal storedR, FuturesPointValueService pointValueService) {
        BigDecimal calculatedPnl = calculateGrossPnl(symbol, side, entryPrice, exitPrice, quantity, pointValueService);
        if (storedPnl == null || calculatedPnl.subtract(storedPnl.setScale(2, RoundingMode.HALF_UP)).abs().compareTo(PNL_TOLERANCE) > 0) {
            throw new IllegalStateException("Demo fixture P&L contradicts side/entry/exit math. symbol=" + symbol
                    + ", side=" + side
                    + ", entry=" + entryPrice
                    + ", exit=" + exitPrice
                    + ", quantity=" + quantity
                    + ", storedPnl=" + storedPnl
                    + ", calculatedPnl=" + calculatedPnl);
        }
        if (storedR == null) {
            throw new IllegalStateException("Demo fixture R multiple is required. symbol=" + symbol);
        }
        int pnlSign = calculatedPnl.signum();
        int rSign = storedR.signum();
        if ((pnlSign > 0 && rSign < 0) || (pnlSign < 0 && rSign > 0)) {
            throw new IllegalStateException("Demo fixture R multiple sign contradicts P&L. symbol=" + symbol
                    + ", pnl=" + calculatedPnl
                    + ", rMultiple=" + storedR);
        }
    }

    private static void validateTickSize(String symbol, BigDecimal price, String fieldName) {
        BigDecimal tickSize = tickSize(symbol);
        BigDecimal ticks = price.divide(tickSize, 8, RoundingMode.UNNECESSARY);
        if (ticks.stripTrailingZeros().scale() > 0) {
            throw new IllegalStateException("Demo fixture " + fieldName + " is off tick. symbol=" + symbol
                    + ", price=" + price
                    + ", tickSize=" + tickSize);
        }
    }

    private static BigDecimal tickSize(String symbol) {
        return switch (symbol) {
            case "MES", "ES", "MNQ", "NQ" -> new BigDecimal("0.25");
            default -> throw new IllegalArgumentException("Unsupported demo tick-size symbol: " + symbol);
        };
    }

    private DemoAiSeedCounts seedAiReviews(User user, List<Trade> trades) {
        Instant now = Instant.now();
        List<AiTradeReview> reviews = new ArrayList<>();
        List<AiTradeAnalysis> analyses = new ArrayList<>();
        for (int i = 0; i < Math.min(18, trades.size()); i++) {
            Trade trade = trades.get(i * 3);
            String grade = trade.getPnlAmount().signum() >= 0 ? "A-" : "C+";
            reviews.add(AiTradeReview.builder().userId(user.getId()).tradeId(trade.getId()).accountId(trade.getAccount().getId()).reviewDate(trade.getTradeDate().toString()).symbol(trade.getSymbol()).pnl(trade.getPnlAmount().doubleValue()).grade(grade).reviewVersion("dev-demo-review").reviewHash("dev-review-" + trade.getId()).summary("Demo AI review: recurring pattern is strong setup selection with occasional impatience during volatile sessions.").mistakesJson("[\"entered before confirmation\",\"managed stop too tightly\"]").strengthsJson("[\"clear market structure read\",\"good risk definition\"]").improvementPlanJson("[\"pause after one loss\",\"confirm displacement before entry\"]").tagsJson("[\"demo\",\"discipline\",\"risk\"]").status("COMPLETED").createdAt(now.minusSeconds(i * 3600L)).build());
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
        return new DemoAiSeedCounts(reviews.size(), analyses.size());
    }

    private void seedCoachSnapshots(User user, TradingAccount account, List<Trade> trades) {
        LocalDate today = LocalDate.now();
        coachSnapshotRepository.save(AiCoachSnapshot.builder().userId(user.getId()).accountId(account.getId()).snapshotType(CoachSnapshotType.DAILY).periodStart(today.minusDays(1)).periodEnd(today.minusDays(1)).status(CoachSnapshotStatus.COMPLETED).totalTrades(6).netPnl(BigDecimal.valueOf(420)).winRate(BigDecimal.valueOf(66.7)).profitFactor(BigDecimal.valueOf(1.85)).bestSetup("5m FVG continuation").worstSetup("Opening range fakeout").bestSession("NY AM").worstSession("NY PM").biggestMistake("entered before confirmation").reviewHash("dev-daily-coach").aiResponseJson("{\"headline\":\"Your best trades came from patience after the first displacement.\",\"emotionalPattern\":\"Impatience appears after missed moves.\",\"bestBehavior\":\"You respected risk when the setup was preplanned.\",\"avoidNextSession\":\"Do not chase the second move in chop.\",\"nextSessionFocus\":\"Wait for confirmation before entry.\",\"oneRuleForNextSession\":\"One loss, then five-minute reset.\"}").deterministicSummaryJson("{\"demo\":true}").build());
        coachSnapshotRepository.save(AiCoachSnapshot.builder().userId(user.getId()).accountId(account.getId()).snapshotType(CoachSnapshotType.WEEKLY).periodStart(today.minusDays(7)).periodEnd(today.minusDays(1)).status(CoachSnapshotStatus.COMPLETED).totalTrades(Math.min(42, trades.size())).netPnl(BigDecimal.valueOf(1280)).winRate(BigDecimal.valueOf(58.1)).profitFactor(BigDecimal.valueOf(1.42)).bestSetup("HTF liquidity sweep").worstSetup("Opening range fakeout").bestSession("London").worstSession("NY PM").biggestMistake("overtraded chop").reviewHash("dev-weekly-coach").aiResponseJson("{\"headline\":\"Your week improves when you avoid NY PM chop.\",\"emotionalPattern\":\"Frustration rises after two missed entries.\",\"bestBehavior\":\"Strong patience in London sweep setups.\",\"riskNote\":\"Keep size flat after wins.\",\"disciplineNote\":\"Friday/OPEX-like sessions need stricter filters.\",\"nextSessionFocus\":\"Trade only A setups in the first 90 minutes.\"}").deterministicSummaryJson("{\"demo\":true}").build());
    }

    private void seedTradingDna(User user, List<Trade> trades) {
        tradingDnaSnapshotRepository.save(TradingDnaSnapshot.builder().userId(user.getId()).accountId(null).days(30).includeAllAccounts(true).dataHash("dev-trading-dna").status("COMPLETED").totalTrades(Math.min(95, trades.size())).aiResponseJson("{\"strengths\":[\"Strong liquidity sweep recognition\",\"Good risk when setup is preplanned\"],\"weaknesses\":[\"Overtrading chop\",\"Friday volatility impatience\"],\"emotionalPatterns\":[\"Revenge impulse after missed move\"],\"riskPatterns\":[\"Occasional oversized trade after a win\"],\"bestConditions\":[\"London sweep\",\"NY AM 5m FVG continuation\"],\"worstConditions\":[\"NY PM chop\",\"OPEX-like Fridays\"],\"recurringMistakes\":[\"entered before confirmation\",\"moved stop too early\"],\"improvementTrend\":\"Improving over the last two weeks\",\"consistencyScore\":74,\"disciplineScore\":69,\"executionScore\":78,\"patienceScore\":66,\"confidenceSummary\":\"Confidence is best after one planned winner, but slips after missed continuation moves.\",\"coachingSummary\":\"Protect your edge by limiting trade count and requiring confirmation on volatile Fridays.\"}").contextJson("{\"demo\":true}").timelineJson("[{\"date\":\"%s\",\"consistency\":68,\"winRate\":54,\"emotionalStability\":61,\"discipline\":64},{\"date\":\"%s\",\"consistency\":74,\"winRate\":59,\"emotionalStability\":70,\"discipline\":72}]".formatted(LocalDate.now().minusDays(14), LocalDate.now())).build());
    }

    private DemoDisciplineSeedCounts seedDiscipline(User user, List<Trade> trades) {
        return seedDiscipline(user, trades, true);
    }

    private DemoDisciplineSeedCounts seedDiscipline(User user, List<Trade> trades, boolean includeDailyScores) {
        List<TradeRuleViolation> violations = new ArrayList<>();
        Map<LocalDate, Integer> countsByDate = new LinkedHashMap<>();
        for (int i = 0; i < trades.size(); i += 7) {
            Trade trade = trades.get(i);
            violations.add(TradeRuleViolation.builder().userId(user.getId()).tradeId(trade.getId()).tradeDate(trade.getTradeDate()).ruleCode(i % 2 == 0 ? "REVENGE_TRADE" : "NO_CONFIRMATION").ruleTitle(i % 2 == 0 ? "Revenge trade after loss" : "Entered before confirmation").category("DISCIPLINE").severity(i % 3 == 0 ? "MAJOR" : "MINOR").reason("Demo violation generated from seeded mistake pattern.").source("DEV_DEMO").build());
            countsByDate.merge(trade.getTradeDate(), 1, Integer::sum);
        }
        violationRepository.saveAll(violations);
        int scoresCreated = 0;
        if (!includeDailyScores) {
            return new DemoDisciplineSeedCounts(violations.size(), scoresCreated);
        }
        List<DailyDisciplineScore> scores = countsByDate.entrySet().stream().map(entry -> DailyDisciplineScore.builder().userId(user.getId()).tradeDate(entry.getKey()).accountId(null).score(Math.max(45, 92 - entry.getValue() * 12)).totalTrades(4 + entry.getValue()).violationCount(entry.getValue()).majorViolationCount(entry.getValue() > 1 ? 1 : 0).criticalViolationCount(0).currentCleanDayStreak(entry.getValue() == 0 ? 3 : 0).cleanDay(entry.getValue() == 0).build()).toList();
        disciplineScoreRepository.saveAll(scores);
        scoresCreated = scores.size();
        return new DemoDisciplineSeedCounts(violations.size(), scoresCreated);
    }

    private void seedUsageEvents(User user) {
        LocalDateTime now = LocalDateTime.now();
        usageEventRepository.saveAll(List.of(AiUsageEvent.builder().userId(user.getId()).usageType(UsageType.AI_TRADE_REVIEW).createdAt(now.minusDays(5)).build(), AiUsageEvent.builder().userId(user.getId()).usageType(UsageType.AI_TRADE_REVIEW).createdAt(now.minusDays(4)).build(), AiUsageEvent.builder().userId(user.getId()).usageType(UsageType.DAILY_COACH).createdAt(now.minusDays(2)).build(), AiUsageEvent.builder().userId(user.getId()).usageType(UsageType.WEEKLY_COACH).createdAt(now.minusDays(1)).build(), AiUsageEvent.builder().userId(user.getId()).usageType(UsageType.TRADING_DNA).createdAt(now.minusHours(8)).build()));
    }

    private FeatureUsageEvent featureEvent(Long userId, FeatureKey feature, LocalDateTime createdAt) {
        return FeatureUsageEvent.builder().userId(userId).feature(feature).createdAt(createdAt == null ? LocalDateTime.now() : createdAt).build();
    }

    private LocalDateTime toLocalDateTime(Instant instant) {
        return instant == null ? LocalDateTime.now() : LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
    }

    private record DemoTradeFixture(
            String symbol,
            String contractSymbol,
            LocalDate tradeDate,
            LocalTime entryTime,
            long durationMinutes,
            String side,
            int quantity,
            BigDecimal entryPrice,
            BigDecimal exitPrice,
            BigDecimal riskAmount,
            SetupType setup,
            SessionType session,
            boolean replayCompatible,
            String notes,
            String psychologyNotes,
            String lessonLearned,
            DemoHistoricalVerification historicalVerification
    ) {
        private DemoTradeFixture(String symbol, String contractSymbol, LocalDate tradeDate, LocalTime entryTime, long durationMinutes, String side, int quantity, BigDecimal entryPrice, BigDecimal exitPrice, BigDecimal riskAmount, SetupType setup, SessionType session, boolean replayCompatible, String notes, String psychologyNotes, String lessonLearned) {
            this(symbol, contractSymbol, tradeDate, entryTime, durationMinutes, side, quantity, entryPrice, exitPrice, riskAmount, setup, session, replayCompatible, notes, psychologyNotes, lessonLearned, null);
        }

        private BigDecimal expectedGrossPnl() {
            BigDecimal pointValue = switch (symbol) {
                case "MNQ" -> new BigDecimal("2");
                case "MES" -> new BigDecimal("5");
                case "NQ" -> new BigDecimal("20");
                case "ES" -> new BigDecimal("50");
                default -> throw new IllegalArgumentException("Unsupported demo fixture symbol: " + symbol);
            };
            BigDecimal pointMove = "SHORT".equals(side) ? entryPrice.subtract(exitPrice) : exitPrice.subtract(entryPrice);
            return pointMove.multiply(pointValue).multiply(BigDecimal.valueOf(quantity)).setScale(2, RoundingMode.HALF_UP);
        }
    }

    private record DemoHistoricalVerification(
            VerifiedCandle entryCandle,
            VerifiedCandle exitCandle,
            BigDecimal windowMaxHigh,
            BigDecimal windowMinLow,
            BigDecimal expectedRealizedPoints,
            BigDecimal expectedMfePoints,
            BigDecimal expectedMaePoints
    ) {}

    record VerifiedCandle(
            BigDecimal open,
            BigDecimal high,
            BigDecimal low,
            BigDecimal close
    ) {
        boolean contains(BigDecimal price) {
            return price.compareTo(low) >= 0 && price.compareTo(high) <= 0;
        }
    }

    record DemoReplayCalibration(
            String symbol,
            String contractSymbol,
            LocalDate tradeDate,
            LocalTime entryTime,
            LocalTime exitTime,
            String side,
            int quantity,
            BigDecimal entryPrice,
            BigDecimal exitPrice,
            BigDecimal riskAmount,
            BigDecimal pnl,
            BigDecimal rMultiple,
            VerifiedCandle entryCandle,
            VerifiedCandle exitCandle,
            BigDecimal windowMaxHigh,
            BigDecimal windowMinLow,
            BigDecimal expectedRealizedPoints,
            BigDecimal expectedMfePoints,
            BigDecimal expectedMaePoints
    ) {}

    private record DemoAccounts(List<TradingAccount> accounts, int accountsCreated) {
    }

    private record DemoAiSeedCounts(int reviewsCreated, int analysesCreated) {
    }

    private record DemoDisciplineSeedCounts(int violationsCreated, int dailyScoresCreated) {
    }

    @java.lang.SuppressWarnings("all")
    public DevDemoDataService(final TradingAccountRepository accountRepository, final TradeRepository tradeRepository, final AiTradeReviewRepository reviewRepository, final AiTradeAnalysisRepository analysisRepository, final AiCoachSnapshotRepository coachSnapshotRepository, final TradingDnaSnapshotRepository tradingDnaSnapshotRepository, final AiSessionReviewRepository sessionReviewRepository, final TradeRuleViolationRepository violationRepository, final DailyDisciplineScoreRepository disciplineScoreRepository, final AiUsageEventRepository usageEventRepository, final FeatureUsageRepository featureUsageRepository, final UserRepository userRepository, final FuturesContractCalendar contractCalendar, final FuturesPointValueService pointValueService, final TradeReplayRepository tradeReplayRepository, final TradeReplayInsightRepository tradeReplayInsightRepository, final TradeMemoryRepository tradeMemoryRepository, final TradeMemoryIndexService tradeMemoryIndexService, final PasswordEncoder passwordEncoder, final NextSessionProtectionCommitmentRepository protectionCommitmentRepository, final JwtService jwtService, final AccountSurvivalTimeSource accountSurvivalTimeSource) {
        this.accountRepository = accountRepository;
        this.tradeRepository = tradeRepository;
        this.reviewRepository = reviewRepository;
        this.analysisRepository = analysisRepository;
        this.coachSnapshotRepository = coachSnapshotRepository;
        this.tradingDnaSnapshotRepository = tradingDnaSnapshotRepository;
        this.sessionReviewRepository = sessionReviewRepository;
        this.violationRepository = violationRepository;
        this.disciplineScoreRepository = disciplineScoreRepository;
        this.usageEventRepository = usageEventRepository;
        this.featureUsageRepository = featureUsageRepository;
        this.userRepository = userRepository;
        this.contractCalendar = contractCalendar;
        this.pointValueService = pointValueService;
        this.tradeReplayRepository = tradeReplayRepository;
        this.tradeReplayInsightRepository = tradeReplayInsightRepository;
        this.tradeMemoryRepository = tradeMemoryRepository;
        this.tradeMemoryIndexService = tradeMemoryIndexService;
        this.passwordEncoder = passwordEncoder;
        this.protectionCommitmentRepository = protectionCommitmentRepository;
        this.jwtService = jwtService;
        this.accountSurvivalTimeSource = accountSurvivalTimeSource;
    }
}
