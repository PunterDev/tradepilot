package com.tradepilot;

import com.tradepilot.billing.PlanType;
import com.tradepilot.billing.SubscriptionStatus;
import com.tradepilot.entity.Trade;
import com.tradepilot.entity.TradingAccount;
import com.tradepilot.entity.User;
import com.tradepilot.enums.OutcomeType;
import com.tradepilot.enums.PropFirmType;
import com.tradepilot.enums.SessionType;
import com.tradepilot.enums.SetupType;
import com.tradepilot.enums.TradingAccountStatus;
import com.tradepilot.enums.TradingAccountType;
import com.tradepilot.repository.TradeRepository;
import com.tradepilot.repository.TradingAccountRepository;
import com.tradepilot.repository.UserRepository;
import com.tradepilot.security.JwtService;
import com.tradepilot.tradereplay.entity.TradeReplay;
import com.tradepilot.tradereplay.entity.TradeReplayInsight;
import com.tradepilot.tradereplay.entity.TradeReplayInsightType;
import com.tradepilot.tradereplay.entity.TradeReplayStatus;
import com.tradepilot.tradereplay.repository.TradeReplayInsightRepository;
import com.tradepilot.tradereplay.repository.TradeReplayRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = "app.dev-tools-enabled=true")
class DevToolsEnabledTests {
    @Autowired MockMvc mockMvc;
    @Autowired UserRepository userRepository;
    @Autowired TradingAccountRepository accountRepository;
    @Autowired TradeRepository tradeRepository;
    @Autowired TradeReplayRepository replayRepository;
    @Autowired TradeReplayInsightRepository replayInsightRepository;
    @Autowired JwtService jwtService;

    @Test
    void seedDataBelongsOnlyToCurrentUser() throws Exception {
        User userA = createUser("dev-seed-a@example.com");
        User userB = createUser("dev-seed-b@example.com");

        mockMvc.perform(post("/api/dev/seed-demo-data")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", bearer(userA)))
                .andExpect(status().isOk());

        assertThat(accountRepository.countByUser(userA)).isEqualTo(2);
        assertThat(tradeRepository.countByUser(userA)).isBetween(150L, 250L);
        assertThat(accountRepository.countByUser(userB)).isZero();
        assertThat(tradeRepository.countByUser(userB)).isZero();
    }

    @Test
    void clearDataOnlyDeletesCurrentUserData() throws Exception {
        User userA = createUser("dev-clear-a@example.com");
        User userB = createUser("dev-clear-b@example.com");

        mockMvc.perform(post("/api/dev/seed-demo-data").header("Authorization", bearer(userA)))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/dev/seed-demo-data").header("Authorization", bearer(userB)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/dev/clear-demo-data").header("Authorization", bearer(userA)))
                .andExpect(status().isOk());

        assertThat(accountRepository.countByUser(userA)).isZero();
        assertThat(tradeRepository.countByUser(userA)).isZero();
        assertThat(accountRepository.countByUser(userB)).isEqualTo(2);
        assertThat(tradeRepository.countByUser(userB)).isBetween(150L, 250L);
    }

    @Test
    void reseedDryRunReportsDemoFixtureReplacementWithoutChangingRows() throws Exception {
        User user = createUser("dev-reseed-dry-run@example.com");
        TradingAccount account = createAccount(user);
        Trade staleDemoTrade = tradeRepository.save(staleMesDemoTrade(user, account));

        mockMvc.perform(post("/api/dev/demo-data/reseed")
                        .param("dryRun", "true")
                        .header("Authorization", bearer(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dryRun").value(true))
                .andExpect(jsonPath("$.targetDemoDataVersion").value(5))
                .andExpect(jsonPath("$.existingDemoDataVersions[0]").value(1))
                .andExpect(jsonPath("$.tradesToDelete").value(1))
                .andExpect(jsonPath("$.tradesToCreate").value(190))
                .andExpect(jsonPath("$.tradesDeleted").value(0))
                .andExpect(jsonPath("$.tradesCreated").value(0));

        assertThat(tradeRepository.findById(staleDemoTrade.getId())).isPresent();
        assertThat(tradeRepository.countByUserAndExternalTradeKeyStartingWith(user, "DEV-DEMO-")).isEqualTo(1);
    }

    @Test
    void reseedReplacesOnlyMarkedDemoTradesAndPreservesRealTrades() throws Exception {
        User user = createUser("dev-reseed-actual@example.com");
        TradingAccount account = createAccount(user);
        Trade staleDemoTrade = tradeRepository.save(staleMesDemoTrade(user, account));
        Trade realImportedTrade = tradeRepository.save(realImportedTrade(user, account));

        mockMvc.perform(post("/api/dev/demo-data/reseed")
                        .param("dryRun", "false")
                        .header("Authorization", bearer(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dryRun").value(false))
                .andExpect(jsonPath("$.targetDemoDataVersion").value(5))
                .andExpect(jsonPath("$.tradesDeleted").value(1))
                .andExpect(jsonPath("$.tradesCreated").value(190));

        assertThat(tradeRepository.findById(staleDemoTrade.getId())).isEmpty();
        assertThat(tradeRepository.findById(realImportedTrade.getId())).isPresent();

        List<Trade> demoTrades = tradeRepository.findByUserAndExternalTradeKeyStartingWithOrderByExternalTradeKeyAsc(user, "DEV-DEMO-");
        assertThat(demoTrades).hasSize(190);
        assertThat(demoTrades).allSatisfy(trade -> {
            assertThat(trade.getExternalTradeKey()).startsWith("DEV-DEMO-v5-");
            assertThat(trade.getContractSymbol()).isNotBlank();
            assertThat(trade.getPnlAmount()).isNotNull();
            if (trade.getExternalTradeKey().contains("-REPLAY-") && "MESU6".equals(trade.getContractSymbol())) {
                assertThat(trade.getEntryPrice()).isGreaterThan(new BigDecimal("7000.00"));
            }
            if (trade.getExternalTradeKey().contains("-REPLAY-") && "MNQU6".equals(trade.getContractSymbol())) {
                assertThat(trade.getEntryPrice()).isGreaterThan(new BigDecimal("29000.00"));
            }
        });
        assertThat(demoTrades).filteredOn(trade -> trade.getExternalTradeKey().contains("-REPLAY-")).hasSize(10);
        assertThat(demoTrades).filteredOn(trade -> trade.getExternalTradeKey().contains("-GENERAL-")).hasSize(180);
        assertThat(demoTrades)
                .filteredOn(trade -> trade.getExternalTradeKey().contains("-REPLAY-") && trade.getExternalTradeKey().endsWith("-1"))
                .singleElement()
                .satisfies(trade -> {
                    assertThat(trade.getSymbol()).isEqualTo("MNQ");
                    assertThat(trade.getContractSymbol()).isEqualTo("MNQM6");
                    assertThat(trade.getEntryTime()).isEqualTo(LocalDate.of(2026, 6, 8).atTime(5, 25));
                    assertThat(trade.getExitTime()).isEqualTo(LocalDate.of(2026, 6, 8).atTime(5, 28));
                    assertThat(trade.getSide()).isEqualTo("SHORT");
                    assertThat(trade.getQuantity()).isEqualTo(3);
                    assertThat(trade.getEntryPrice()).isEqualByComparingTo(new BigDecimal("29190.00"));
                    assertThat(trade.getExitPrice()).isEqualByComparingTo(new BigDecimal("29178.00"));
                    assertThat(trade.getPnlAmount()).isEqualByComparingTo(new BigDecimal("72.00"));
                    assertThat(trade.getPnlR()).isEqualByComparingTo(new BigDecimal("0.80"));
                });
        assertThat(demoTrades).noneSatisfy(trade -> {
            assertThat(trade.getTradeDate()).isEqualTo(LocalDate.of(2026, 6, 30));
            assertThat(trade.getEntryPrice()).isEqualByComparingTo(new BigDecimal("5360.75"));
            assertThat(trade.getPnlAmount()).isEqualByComparingTo(new BigDecimal("157.50"));
        });
        assertThat(tradeRepository.countByUser(user)).isEqualTo(191);
    }

    @Test
    void resetTradeReplayDeletesOnlyReplayStateForOwnedTrade() throws Exception {
        User user = createUser("dev-replay-reset@example.com");
        TradingAccount account = createAccount(user);
        Trade trade = tradeRepository.save(realImportedTrade(user, account));
        TradeReplay replay = replayRepository.save(replayFor(user, trade, TradeReplayStatus.READY));
        replayInsightRepository.saveAll(List.of(insight(replay, 1), insight(replay, 2), insight(replay, 3)));

        mockMvc.perform(delete("/api/dev/trades/" + trade.getId() + "/replay")
                        .header("Authorization", bearer(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tradeId").value(trade.getId()))
                .andExpect(jsonPath("$.replaysDeleted").value(1))
                .andExpect(jsonPath("$.insightsDeleted").value(3))
                .andExpect(jsonPath("$.cacheEntriesEvicted").value(0))
                .andExpect(jsonPath("$.clearedTradeReplayCache").value(true));

        assertThat(replayRepository.findByUserIdAndTradeId(user.getId(), trade.getId())).isEmpty();
        assertThat(replayInsightRepository.findByReplayIdOrderByDisplayOrderAsc(replay.getId())).isEmpty();
        assertThat(tradeRepository.findById(trade.getId())).isPresent();
    }

    @Test
    void resetTradeReplayIsIdempotentWhenNoReplayExists() throws Exception {
        User user = createUser("dev-replay-reset-empty@example.com");
        TradingAccount account = createAccount(user);
        Trade trade = tradeRepository.save(realImportedTrade(user, account));

        mockMvc.perform(delete("/api/dev/trades/" + trade.getId() + "/replay")
                        .header("Authorization", bearer(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tradeId").value(trade.getId()))
                .andExpect(jsonPath("$.replaysDeleted").value(0))
                .andExpect(jsonPath("$.insightsDeleted").value(0));
    }

    @Test
    void resetTradeReplayRequiresOwnedTrade() throws Exception {
        User owner = createUser("dev-replay-reset-owner@example.com");
        User other = createUser("dev-replay-reset-other@example.com");
        TradingAccount account = createAccount(owner);
        Trade trade = tradeRepository.save(realImportedTrade(owner, account));

        mockMvc.perform(delete("/api/dev/trades/" + trade.getId() + "/replay")
                        .header("Authorization", bearer(other)))
                .andExpect(status().isNotFound());
    }

    @Test
    void mockUpgradeAndDowngradeMutateOnlyCurrentUserPlan() throws Exception {
        User user = createUser("dev-plan@example.com");

        mockMvc.perform(post("/api/dev/mock-upgrade-pro").header("Authorization", bearer(user)))
                .andExpect(status().isOk());

        User upgraded = userRepository.findById(user.getId()).orElseThrow();
        assertThat(upgraded.getPlan()).isEqualTo(PlanType.PRO);
        assertThat(upgraded.getSubscriptionStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(upgraded.getStripeCustomerId()).isEqualTo("mock_customer");
        assertThat(upgraded.getStripeSubscriptionId()).isEqualTo("mock_subscription");

        mockMvc.perform(post("/api/dev/mock-downgrade-free").header("Authorization", bearer(upgraded)))
                .andExpect(status().isOk());

        User downgraded = userRepository.findById(user.getId()).orElseThrow();
        assertThat(downgraded.getPlan()).isEqualTo(PlanType.FREE);
        assertThat(downgraded.getSubscriptionStatus()).isEqualTo(SubscriptionStatus.NONE);
        assertThat(downgraded.getStripeCustomerId()).isNull();
        assertThat(downgraded.getStripeSubscriptionId()).isNull();
    }

    private User createUser(String email) {
        return userRepository.save(User.builder()
                .name(email)
                .email(email)
                .passwordHash("test-password-hash")
                .emailVerified(true)
                .plan(PlanType.FREE)
                .subscriptionStatus(SubscriptionStatus.NONE)
                .build());
    }

    private TradingAccount createAccount(User user) {
        return accountRepository.save(TradingAccount.builder()
                .user(user)
                .name("Imported Account")
                .propFirm(PropFirmType.OTHER)
                .accountNumber("REAL-1")
                .accountType(TradingAccountType.PERSONAL)
                .startingBalance(BigDecimal.valueOf(25000))
                .status(TradingAccountStatus.ACTIVE)
                .build());
    }

    private Trade staleMesDemoTrade(User user, TradingAccount account) {
        Trade trade = Trade.builder()
                .user(user)
                .account(account)
                .symbol("MES")
                .market("FUTURES")
                .tradeDate(LocalDate.of(2026, 6, 30))
                .entryTime(LocalDateTime.of(2026, 6, 30, 10, 42))
                .exitTime(LocalDateTime.of(2026, 6, 30, 10, 51))
                .side("SHORT")
                .setup(SetupType.FVG)
                .session(SessionType.NY_AM)
                .entryPrice(new BigDecimal("5360.75"))
                .exitPrice(new BigDecimal("5381.75"))
                .quantity(3)
                .riskAmount(new BigDecimal("150.00"))
                .pnlAmount(new BigDecimal("157.50"))
                .outcome(OutcomeType.WIN)
                .externalTradeKey("DEV-DEMO-" + user.getId() + "-stale-mes")
                .build();
        trade.setContractSymbol("MESU6");
        return trade;
    }

    private Trade realImportedTrade(User user, TradingAccount account) {
        Trade trade = Trade.builder()
                .user(user)
                .account(account)
                .symbol("MNQ")
                .market("FUTURES")
                .tradeDate(LocalDate.of(2026, 6, 4))
                .entryTime(LocalDateTime.of(2026, 6, 4, 14, 12))
                .exitTime(LocalDateTime.of(2026, 6, 4, 14, 20))
                .side("SHORT")
                .setup(SetupType.FVG)
                .session(SessionType.NY_PM)
                .entryPrice(new BigDecimal("30580.00"))
                .exitPrice(new BigDecimal("30560.00"))
                .quantity(1)
                .riskAmount(new BigDecimal("75.00"))
                .pnlAmount(new BigDecimal("40.00"))
                .outcome(OutcomeType.WIN)
                .externalTradeKey("TRADOVATE-REAL-1")
                .build();
        trade.setContractSymbol("MNQM6");
        return trade;
    }

    private TradeReplay replayFor(User user, Trade trade, TradeReplayStatus status) {
        TradeReplay replay = new TradeReplay();
        replay.setUser(user);
        replay.setTrade(trade);
        replay.setStatus(status);
        replay.setOriginalBrokerSymbol(trade.getContractSymbol());
        replay.setResolvedProviderSymbol(trade.getContractSymbol());
        replay.setProvider("databento");
        replay.setCandleInterval("1m");
        replay.setWindowStart(Instant.parse("2026-06-04T19:00:00Z"));
        replay.setWindowEnd(Instant.parse("2026-06-04T19:30:00Z"));
        replay.setGeneratedAt(Instant.parse("2026-06-04T19:31:00Z"));
        replay.setMetricsJson("{\"mfePoints\":10}");
        return replay;
    }

    private TradeReplayInsight insight(TradeReplay replay, int order) {
        TradeReplayInsight insight = new TradeReplayInsight();
        insight.setReplay(replay);
        insight.setType(TradeReplayInsightType.MFE);
        insight.setEventTimestamp(Instant.parse("2026-06-04T19:1%d:00Z".formatted(order)));
        insight.setEventPrice(new BigDecimal("30560.00"));
        insight.setSeverity("INFO");
        insight.setDeterministicTitle("Replay insight " + order);
        insight.setDeterministicExplanation("Replay insight fixture.");
        insight.setSupportingMetricsJson("{}");
        insight.setDisplayOrder(order);
        return insight;
    }

    private String bearer(User user) {
        return "Bearer " + jwtService.generateToken(user);
    }
}
