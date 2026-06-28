package com.tradepilot;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradepilot.billing.PlanType;
import com.tradepilot.billing.SubscriptionStatus;
import com.tradepilot.coach.entity.AiCoachSnapshot;
import com.tradepilot.coach.entity.CoachSnapshotStatus;
import com.tradepilot.coach.entity.CoachSnapshotType;
import com.tradepilot.coach.repository.AiCoachSnapshotRepository;
import com.tradepilot.entity.AiSessionReview;
import com.tradepilot.entity.AiTradeAnalysis;
import com.tradepilot.entity.AiTradeReview;
import com.tradepilot.entity.DailyDisciplineScore;
import com.tradepilot.entity.Trade;
import com.tradepilot.entity.TradeRuleViolation;
import com.tradepilot.entity.TradingAccount;
import com.tradepilot.entity.User;
import com.tradepilot.enums.OutcomeType;
import com.tradepilot.enums.PropFirmType;
import com.tradepilot.enums.SessionType;
import com.tradepilot.enums.SetupType;
import com.tradepilot.enums.TradingAccountStatus;
import com.tradepilot.enums.TradingAccountType;
import com.tradepilot.enums.UserRole;
import com.tradepilot.leakdetector.entity.AiLeakDetectorSnapshot;
import com.tradepilot.leakdetector.repository.AiLeakDetectorSnapshotRepository;
import com.tradepilot.repository.AiSessionReviewRepository;
import com.tradepilot.repository.AiTradeAnalysisRepository;
import com.tradepilot.repository.AiTradeReviewRepository;
import com.tradepilot.repository.DailyDisciplineScoreRepository;
import com.tradepilot.repository.TradeRepository;
import com.tradepilot.repository.TradeRuleViolationRepository;
import com.tradepilot.repository.TradingAccountRepository;
import com.tradepilot.repository.UserRepository;
import com.tradepilot.tradingdna.entity.TradingDnaSnapshot;
import com.tradepilot.tradingdna.repository.TradingDnaSnapshotRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminTradeCleanupTests {
    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UserRepository userRepository;
    @Autowired TradingAccountRepository tradingAccountRepository;
    @Autowired TradeRepository tradeRepository;
    @Autowired AiTradeReviewRepository aiTradeReviewRepository;
    @Autowired AiTradeAnalysisRepository aiTradeAnalysisRepository;
    @Autowired TradeRuleViolationRepository tradeRuleViolationRepository;
    @Autowired DailyDisciplineScoreRepository dailyDisciplineScoreRepository;
    @Autowired AiSessionReviewRepository aiSessionReviewRepository;
    @Autowired AiLeakDetectorSnapshotRepository leakDetectorSnapshotRepository;
    @Autowired TradingDnaSnapshotRepository tradingDnaSnapshotRepository;
    @Autowired AiCoachSnapshotRepository coachSnapshotRepository;
    @Autowired PasswordEncoder passwordEncoder;

    @Test
    void dryRunReportsCountsWithoutDeletingRows() throws Exception {
        User admin = createUser("cleanup-admin-dry@example.com", UserRole.ADMIN, PlanType.PRO);
        User target = createUser("cleanup-target-dry@example.com", UserRole.USER, PlanType.PRO);
        TradingAccount account = createAccount(target, "Dry Run Account");
        Trade trade = createTrade(target, account, LocalDate.of(2026, 6, 16), "-25.00", "MNQU6");
        aiTradeReviewRepository.save(AiTradeReview.builder()
                .userId(target.getId())
                .accountId(account.getId())
                .tradeId(trade.getId())
                .reviewVersion("v1")
                .reviewHash("dry-review")
                .status("COMPLETED")
                .createdAt(Instant.now())
                .build());

        mockMvc.perform(delete("/api/admin/trades/cleanup-range")
                        .header("Authorization", "Bearer " + tokenFor(admin))
                        .param("userId", target.getId().toString())
                        .param("accountId", account.getId().toString())
                        .param("startDate", "2026-06-15")
                        .param("endDate", "2026-06-19")
                        .param("dryRun", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dryRun").value(true))
                .andExpect(jsonPath("$.tradesMatched").value(1))
                .andExpect(jsonPath("$.tradesDeleted").value(0))
                .andExpect(jsonPath("$.tradeReviewsDeleted").value(1));

        assertThat(tradeRepository.findById(trade.getId())).isPresent();
        assertThat(aiTradeReviewRepository.countByUserIdAndTradeIdIn(target.getId(), java.util.List.of(trade.getId()))).isEqualTo(1);
    }

    @Test
    void actualCleanupDeletesOnlySelectedRangeAndDerivedData() throws Exception {
        User admin = createUser("cleanup-admin-actual@example.com", UserRole.ADMIN, PlanType.PRO);
        User target = createUser("cleanup-target-actual@example.com", UserRole.USER, PlanType.PRO);
        target.setStripeCustomerId("cus_keep_cleanup");
        target.setStripeSubscriptionId("sub_keep_cleanup");
        userRepository.save(target);

        User otherUser = createUser("cleanup-other-user@example.com", UserRole.USER, PlanType.PRO);
        TradingAccount account = createAccount(target, "Target Cleanup Account");
        TradingAccount otherAccount = createAccount(target, "Other Cleanup Account");
        TradingAccount otherUserAccount = createAccount(otherUser, "Other User Cleanup Account");

        Trade selectedTrade = createTrade(target, account, LocalDate.of(2026, 6, 16), "-200.50", "MNQU6");
        Trade selectedTradeTwo = createTrade(target, account, LocalDate.of(2026, 6, 19), "75.00", "MESU6");
        Trade outOfRangeTrade = createTrade(target, account, LocalDate.of(2026, 6, 22), "-10.00", "MNQU6");
        Trade otherAccountTrade = createTrade(target, otherAccount, LocalDate.of(2026, 6, 16), "-30.00", "NQU6");
        Trade otherUserTrade = createTrade(otherUser, otherUserAccount, LocalDate.of(2026, 6, 16), "-40.00", "ESU6");

        seedDerivedData(target, account, selectedTrade, LocalDate.of(2026, 6, 16), LocalDate.of(2026, 6, 19));

        mockMvc.perform(delete("/api/admin/trades/cleanup-range")
                        .header("Authorization", "Bearer " + tokenFor(admin))
                        .param("userId", target.getId().toString())
                        .param("accountId", account.getId().toString())
                        .param("startDate", "2026-06-15")
                        .param("endDate", "2026-06-19")
                        .param("dryRun", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dryRun").value(false))
                .andExpect(jsonPath("$.tradesMatched").value(2))
                .andExpect(jsonPath("$.tradesDeleted").value(2))
                .andExpect(jsonPath("$.tradeReviewsDeleted").value(1))
                .andExpect(jsonPath("$.tradeAnalysesDeleted").value(1))
                .andExpect(jsonPath("$.tradeRuleViolationsDeleted").value(1))
                .andExpect(jsonPath("$.dailyDisciplineScoresDeleted").value(1))
                .andExpect(jsonPath("$.sessionReviewsDeleted").value(1))
                .andExpect(jsonPath("$.leakSnapshotsDeleted").value(1))
                .andExpect(jsonPath("$.tradingDnaSnapshotsDeleted").value(2))
                .andExpect(jsonPath("$.dailyCoachSnapshotsDeleted").value(1))
                .andExpect(jsonPath("$.weeklyCoachSnapshotsDeleted").value(1));

        assertThat(tradeRepository.findById(selectedTrade.getId())).isEmpty();
        assertThat(tradeRepository.findById(selectedTradeTwo.getId())).isEmpty();
        assertThat(tradeRepository.findById(outOfRangeTrade.getId())).isPresent();
        assertThat(tradeRepository.findById(otherAccountTrade.getId())).isPresent();
        assertThat(tradeRepository.findById(otherUserTrade.getId())).isPresent();
        assertThat(tradingAccountRepository.findById(account.getId())).isPresent();

        User reloadedTarget = userRepository.findById(target.getId()).orElseThrow();
        assertThat(reloadedTarget.getStripeCustomerId()).isEqualTo("cus_keep_cleanup");
        assertThat(reloadedTarget.getStripeSubscriptionId()).isEqualTo("sub_keep_cleanup");

        assertThat(aiTradeReviewRepository.countByUserIdAndTradeIdIn(target.getId(), java.util.List.of(selectedTrade.getId()))).isZero();
        assertThat(aiTradeAnalysisRepository.countByUserIdAndTradeIdIn(target.getId(), java.util.List.of(selectedTrade.getId()))).isZero();
        assertThat(tradeRuleViolationRepository.countByUserIdAndTradeIdIn(target.getId(), java.util.List.of(selectedTrade.getId()))).isZero();
        assertThat(dailyDisciplineScoreRepository.countByUserIdAndAccountIdAndTradeDateBetween(
                target.getId(), account.getId(), LocalDate.of(2026, 6, 15), LocalDate.of(2026, 6, 19))).isZero();
        assertThat(aiSessionReviewRepository.countByUserIdAndAccountIdAndSessionDateBetween(
                target.getId(), account.getId(), LocalDate.of(2026, 6, 15), LocalDate.of(2026, 6, 19))).isZero();
        assertThat(leakDetectorSnapshotRepository.countByUserIdAndAccountId(target.getId(), account.getId())).isZero();
        assertThat(tradingDnaSnapshotRepository.countAffectedByAccountHistory(target.getId(), account.getId())).isZero();
        assertThat(coachSnapshotRepository.countByUserIdAndAccountIdAndSnapshotTypeAndPeriodStartLessThanEqualAndPeriodEndGreaterThanEqual(
                target.getId(), account.getId(), CoachSnapshotType.DAILY, LocalDate.of(2026, 6, 19), LocalDate.of(2026, 6, 15))).isZero();
    }

    @Test
    void cleanupEndpointRequiresAdminAccess() throws Exception {
        User normal = createUser("cleanup-normal@example.com", UserRole.USER, PlanType.PRO);
        User target = createUser("cleanup-target-forbidden@example.com", UserRole.USER, PlanType.PRO);
        TradingAccount account = createAccount(target, "Forbidden Cleanup Account");

        mockMvc.perform(delete("/api/admin/trades/cleanup-range")
                        .header("Authorization", "Bearer " + tokenFor(normal))
                        .param("userId", target.getId().toString())
                        .param("accountId", account.getId().toString())
                        .param("startDate", "2026-06-15")
                        .param("endDate", "2026-06-19")
                        .param("dryRun", "true"))
                .andExpect(status().isForbidden());
    }

    private void seedDerivedData(User user, TradingAccount account, Trade trade, LocalDate startDate, LocalDate endDate) {
        aiTradeReviewRepository.save(AiTradeReview.builder()
                .userId(user.getId())
                .accountId(account.getId())
                .tradeId(trade.getId())
                .reviewVersion("v1")
                .reviewHash("actual-review")
                .status("COMPLETED")
                .createdAt(Instant.now())
                .build());

        AiTradeAnalysis analysis = new AiTradeAnalysis();
        analysis.setUserId(user.getId());
        analysis.setTradeId(trade.getId());
        analysis.setReviewVersion("v1");
        analysis.setReviewHash("actual-analysis");
        analysis.setStatus("COMPLETED");
        analysis.setSymbol(trade.getSymbol());
        analysis.setPnl(trade.getPnlAmount());
        aiTradeAnalysisRepository.save(analysis);

        tradeRuleViolationRepository.save(TradeRuleViolation.builder()
                .userId(user.getId())
                .tradeId(trade.getId())
                .ruleCode("MAX_LOSS")
                .ruleTitle("Max Loss")
                .category("RISK")
                .severity("HIGH")
                .reason("Test violation")
                .source("TEST")
                .tradeDate(startDate.plusDays(1))
                .build());

        dailyDisciplineScoreRepository.save(DailyDisciplineScore.builder()
                .userId(user.getId())
                .accountId(account.getId())
                .tradeDate(startDate.plusDays(1))
                .score(40)
                .totalTrades(2)
                .build());

        aiSessionReviewRepository.save(AiSessionReview.builder()
                .userId(user.getId())
                .accountId(account.getId())
                .sessionDate(startDate.plusDays(1))
                .reviewHash("session-review")
                .summary("Old session review")
                .build());

        leakDetectorSnapshotRepository.save(AiLeakDetectorSnapshot.builder()
                .userId(user.getId())
                .accountId(account.getId())
                .dataHash("leak-hash")
                .totalTrades(2)
                .resultJson("{}")
                .build());

        tradingDnaSnapshotRepository.save(TradingDnaSnapshot.builder()
                .userId(user.getId())
                .accountId(account.getId())
                .days(90)
                .includeAllAccounts(false)
                .dataHash("dna-account")
                .status("COMPLETED")
                .build());

        tradingDnaSnapshotRepository.save(TradingDnaSnapshot.builder()
                .userId(user.getId())
                .accountId(null)
                .days(90)
                .includeAllAccounts(true)
                .dataHash("dna-all")
                .status("COMPLETED")
                .build());

        coachSnapshotRepository.save(AiCoachSnapshot.builder()
                .userId(user.getId())
                .accountId(account.getId())
                .snapshotType(CoachSnapshotType.DAILY)
                .periodStart(startDate.plusDays(1))
                .periodEnd(startDate.plusDays(1))
                .status(CoachSnapshotStatus.COMPLETED)
                .reviewHash("daily-coach")
                .build());

        coachSnapshotRepository.save(AiCoachSnapshot.builder()
                .userId(user.getId())
                .accountId(account.getId())
                .snapshotType(CoachSnapshotType.WEEKLY)
                .periodStart(startDate.minusDays(2))
                .periodEnd(endDate.plusDays(2))
                .status(CoachSnapshotStatus.COMPLETED)
                .reviewHash("weekly-coach")
                .build());
    }

    private User createUser(String email, UserRole role, PlanType plan) {
        return userRepository.save(User.builder()
                .name(email)
                .email(email)
                .passwordHash(passwordEncoder.encode("password123"))
                .emailVerified(true)
                .role(role)
                .plan(plan)
                .subscriptionStatus(plan == PlanType.PRO ? SubscriptionStatus.ACTIVE : SubscriptionStatus.NONE)
                .build());
    }

    private TradingAccount createAccount(User user, String name) {
        return tradingAccountRepository.save(TradingAccount.builder()
                .user(user)
                .name(name)
                .propFirm(PropFirmType.APEX)
                .accountNumber("ACC-" + System.nanoTime())
                .accountType(TradingAccountType.EVALUATION)
                .startingBalance(new BigDecimal("50000.00"))
                .status(TradingAccountStatus.ACTIVE)
                .build());
    }

    private Trade createTrade(User user, TradingAccount account, LocalDate tradeDate, String pnl, String symbol) {
        BigDecimal pnlAmount = new BigDecimal(pnl);
        return tradeRepository.save(Trade.builder()
                .user(user)
                .account(account)
                .symbol(symbol)
                .market("FUTURES")
                .tradeDate(tradeDate)
                .entryTime(tradeDate.atTime(9, 30))
                .exitTime(tradeDate.atTime(9, 45))
                .side("LONG")
                .setup(SetupType.OTHER)
                .session(SessionType.NY_AM)
                .quantity(1)
                .pnlAmount(pnlAmount)
                .outcome(pnlAmount.signum() >= 0 ? OutcomeType.WIN : OutcomeType.LOSS)
                .externalTradeKey("cleanup-" + user.getId() + "-" + account.getId() + "-" + tradeDate + "-" + System.nanoTime())
                .build());
    }

    private String tokenFor(User user) throws Exception {
        String body = """
                {
                  "email": "%s",
                  "password": "password123"
                }
                """.formatted(user.getEmail());

        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode json = objectMapper.readTree(response);
        return json.get("token").asText();
    }
}
