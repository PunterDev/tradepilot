package com.tradepilot;

import com.tradepilot.billing.PlanType;
import com.tradepilot.billing.SubscriptionStatus;
import com.tradepilot.entity.Trade;
import com.tradepilot.entity.TradingAccount;
import com.tradepilot.entity.User;
import com.tradepilot.enums.*;
import com.tradepilot.repository.TradeRepository;
import com.tradepilot.repository.TradingAccountRepository;
import com.tradepilot.repository.UserRepository;
import com.tradepilot.security.JwtService;
import com.tradepilot.tradereplay.entity.TradeReplayInsight;
import com.tradepilot.tradereplay.entity.TradeReplayInsightType;
import com.tradepilot.tradereplay.entity.TradeReplay;
import com.tradepilot.tradereplay.entity.TradeReplayStatus;
import com.tradepilot.tradereplay.service.HistoricalCandle;
import com.tradepilot.tradereplay.service.HistoricalCandleResult;
import com.tradepilot.tradereplay.service.HistoricalMarketDataProvider;
import com.tradepilot.tradereplay.service.HistoricalMarketDataProviderRegistry;
import com.tradepilot.tradereplay.service.ContractResolutionResult;
import com.tradepilot.tradereplay.service.FuturesContractResolver;
import com.tradepilot.tradereplay.service.ContractSymbolSource;
import com.tradepilot.tradereplay.repository.TradeReplayInsightRepository;
import com.tradepilot.tradereplay.repository.TradeReplayRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "tradepilot.features.v3-enabled=true",
        "tradepilot.market-data.provider=databento",
        "tradepilot.market-data.cache-enabled=false"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TradeReplayRegenerationPersistenceTests {
    @Autowired MockMvc mockMvc;
    @Autowired UserRepository userRepository;
    @Autowired TradingAccountRepository accountRepository;
    @Autowired TradeRepository tradeRepository;
    @Autowired TradeReplayRepository replayRepository;
    @Autowired TradeReplayInsightRepository insightRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired JwtService jwtService;
    @Autowired JdbcTemplate jdbcTemplate;

    @MockBean HistoricalMarketDataProviderRegistry providerRegistry;
    @MockBean FuturesContractResolver contractResolver;

    @BeforeEach
    void defaultContractResolution() {
        when(contractResolver.resolve(any())).thenReturn(ContractResolutionResult.ok("MNQ", "MNQM6", "MNQM6"));
    }

    @Test
    void regeneratePersistsStrongExcursionCaptureInsight() throws Exception {
        HistoricalMarketDataProvider provider = Mockito.mock(HistoricalMarketDataProvider.class);
        when(provider.providerName()).thenReturn("databento");
        when(provider.getCandles(any())).thenReturn(HistoricalCandleResult.success(strongCaptureCandles()));
        when(providerRegistry.activeProvider()).thenReturn(provider);

        User user = createUser("replay-regeneration-schema@example.com");
        TradingAccount account = createAccount(user);
        Trade trade = createStrongCaptureTrade(user, account);

        mockMvc.perform(post("/api/v1/trades/" + trade.getId() + "/replay/regenerate")
                        .header("Authorization", "Bearer " + jwtService.generateToken(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("READY"))
                .andExpect(jsonPath("$.metrics.realizedPoints").value(12.0))
                .andExpect(jsonPath("$.metrics.mfePoints").value(14.25))
                .andExpect(jsonPath("$.metrics.maePoints").value(6.25))
                .andExpect(jsonPath("$.metrics.excursionCapturePct").value(84.21))
                .andExpect(jsonPath("$.insights[0].type").value("STRONG_EXCURSION_CAPTURE"));

        Long replayId = replayRepository.findByUserIdAndTradeId(user.getId(), trade.getId()).orElseThrow().getId();
        List<TradeReplayInsight> insights = insightRepository.findByReplayIdOrderByDisplayOrderAsc(replayId);
        assertThat(insights).isNotEmpty();
        assertThat(insights.get(0).getType()).isEqualTo(TradeReplayInsightType.STRONG_EXCURSION_CAPTURE);
        assertThat(insights.get(0).getDisplayOrder()).isEqualTo(1);
    }

    @Test
    void activePendingReplayCannotDuplicateGeneration() throws Exception {
        HistoricalMarketDataProvider provider = Mockito.mock(HistoricalMarketDataProvider.class);
        when(provider.providerName()).thenReturn("databento");
        when(providerRegistry.activeProvider()).thenReturn(provider);
        User user = createUser("replay-active-pending@example.com");
        TradingAccount account = createAccount(user);
        Trade trade = createStrongCaptureTrade(user, account);
        TradeReplay replay = replayRepository.save(replay(user, trade, TradeReplayStatus.PENDING));

        mockMvc.perform(post("/api/v1/trades/" + trade.getId() + "/replay/regenerate")
                        .header("Authorization", "Bearer " + jwtService.generateToken(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.replayId").value(replay.getId()))
                .andExpect(jsonPath("$.status").value("PENDING"));

        verify(provider, never()).getCandles(any());
    }

    @Test
    void stalePendingReplayCanRecoverAndRegenerate() throws Exception {
        HistoricalMarketDataProvider provider = Mockito.mock(HistoricalMarketDataProvider.class);
        when(provider.providerName()).thenReturn("databento");
        when(provider.getCandles(any())).thenReturn(HistoricalCandleResult.success(strongCaptureCandles()));
        when(providerRegistry.activeProvider()).thenReturn(provider);
        User user = createUser("replay-stale-pending@example.com");
        TradingAccount account = createAccount(user);
        Trade trade = createStrongCaptureTrade(user, account);
        TradeReplay replay = replayRepository.save(replay(user, trade, TradeReplayStatus.PENDING));
        makeReplayStale(replay);

        mockMvc.perform(post("/api/v1/trades/" + trade.getId() + "/replay/regenerate")
                        .header("Authorization", "Bearer " + jwtService.generateToken(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("READY"))
                .andExpect(jsonPath("$.failureCode").doesNotExist());
    }

    @Test
    void stalePendingReplayIsMarkedFailedOnReadSoUserCanRetry() throws Exception {
        User user = createUser("replay-stale-read@example.com");
        TradingAccount account = createAccount(user);
        Trade trade = createStrongCaptureTrade(user, account);
        replayRepository.save(replay(user, trade, TradeReplayStatus.PENDING));
        TradeReplay replay = replayRepository.findByUserIdAndTradeId(user.getId(), trade.getId()).orElseThrow();
        makeReplayStale(replay);

        mockMvc.perform(get("/api/v1/trades/" + trade.getId() + "/replay")
                        .header("Authorization", "Bearer " + jwtService.generateToken(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FAILED"))
                .andExpect(jsonPath("$.failureCode").value("REPLAY_PROCESSING_STALE"));
    }

    @Test
    void providerExceptionTransitionsReplayToFailed() throws Exception {
        HistoricalMarketDataProvider provider = Mockito.mock(HistoricalMarketDataProvider.class);
        when(provider.providerName()).thenReturn("databento");
        when(provider.getCandles(any())).thenThrow(new IllegalStateException("provider blew up"));
        when(providerRegistry.activeProvider()).thenReturn(provider);
        User user = createUser("replay-provider-exception@example.com");
        TradingAccount account = createAccount(user);
        Trade trade = createStrongCaptureTrade(user, account);

        mockMvc.perform(post("/api/v1/trades/" + trade.getId() + "/replay/generate")
                        .header("Authorization", "Bearer " + jwtService.generateToken(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FAILED"))
                .andExpect(jsonPath("$.failureCode").value("REPLAY_EXECUTION_ERROR"));
    }

    @Test
    void contractResolutionExceptionTransitionsReplayToFailed() throws Exception {
        when(contractResolver.resolve(any())).thenThrow(new IllegalStateException("contract blew up"));
        User user = createUser("replay-contract-exception@example.com");
        TradingAccount account = createAccount(user);
        Trade trade = createStrongCaptureTrade(user, account);

        mockMvc.perform(post("/api/v1/trades/" + trade.getId() + "/replay/generate")
                        .header("Authorization", "Bearer " + jwtService.generateToken(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FAILED"))
                .andExpect(jsonPath("$.failureCode").value("CONTRACT_RESOLUTION_ERROR"));
    }

    @Test
    void failedReplayCanRegenerate() throws Exception {
        HistoricalMarketDataProvider provider = Mockito.mock(HistoricalMarketDataProvider.class);
        when(provider.providerName()).thenReturn("databento");
        when(provider.getCandles(any())).thenReturn(HistoricalCandleResult.success(strongCaptureCandles()));
        when(providerRegistry.activeProvider()).thenReturn(provider);
        User user = createUser("replay-failed-regenerates@example.com");
        TradingAccount account = createAccount(user);
        Trade trade = createStrongCaptureTrade(user, account);
        replayRepository.save(replay(user, trade, TradeReplayStatus.FAILED));

        mockMvc.perform(post("/api/v1/trades/" + trade.getId() + "/replay/regenerate")
                        .header("Authorization", "Bearer " + jwtService.generateToken(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("READY"));
    }

    private List<HistoricalCandle> strongCaptureCandles() {
        return List.of(
                candle("2026-06-08T10:25:00Z", "29188.50", "29196.25", "29180.50", "29185.00"),
                candle("2026-06-08T10:26:00Z", "29185.75", "29188.75", "29178.75", "29184.75"),
                candle("2026-06-08T10:27:00Z", "29184.75", "29185.75", "29177.00", "29181.00"),
                candle("2026-06-08T10:28:00Z", "29180.00", "29187.00", "29175.75", "29181.75")
        );
    }

    private HistoricalCandle candle(String timestamp, String open, String high, String low, String close) {
        return new HistoricalCandle(
                Instant.parse(timestamp),
                new BigDecimal(open),
                new BigDecimal(high),
                new BigDecimal(low),
                new BigDecimal(close),
                BigDecimal.valueOf(100)
        );
    }

    private User createUser(String email) {
        return userRepository.save(User.builder()
                .name("Replay User")
                .email(email)
                .passwordHash(passwordEncoder.encode("password123"))
                .emailVerified(true)
                .role(UserRole.USER)
                .plan(PlanType.PRO)
                .subscriptionStatus(SubscriptionStatus.ACTIVE)
                .build());
    }

    private TradingAccount createAccount(User user) {
        return accountRepository.save(TradingAccount.builder()
                .user(user)
                .name("Replay Account")
                .propFirm(PropFirmType.TOPSTEP)
                .accountType(TradingAccountType.PERSONAL)
                .startingBalance(BigDecimal.valueOf(50_000))
                .status(TradingAccountStatus.ACTIVE)
                .build());
    }

    private Trade createStrongCaptureTrade(User user, TradingAccount account) {
        Trade trade = Trade.builder()
                .user(user)
                .account(account)
                .symbol("MNQ")
                .market("FUTURES")
                .tradeDate(LocalDate.of(2026, 6, 8))
                .entryTime(LocalDateTime.of(2026, 6, 8, 5, 25))
                .exitTime(LocalDateTime.of(2026, 6, 8, 5, 28))
                .side("SHORT")
                .session(SessionType.NY_AM)
                .entryPrice(new BigDecimal("29190.00"))
                .exitPrice(new BigDecimal("29178.00"))
                .quantity(3)
                .pnlAmount(new BigDecimal("72.00"))
                .outcome(OutcomeType.WIN)
                .build();
        trade.setContractSymbol("MNQM6");
        trade.setContractSymbolSource(ContractSymbolSource.IMPORTED.name());
        trade.setOriginalBrokerSymbol("MNQM6");
        return tradeRepository.save(trade);
    }

    private TradeReplay replay(User user, Trade trade, TradeReplayStatus status) {
        TradeReplay replay = new TradeReplay();
        replay.setUser(user);
        replay.setTrade(trade);
        replay.setStatus(status);
        replay.setOriginalBrokerSymbol(trade.getContractSymbol());
        replay.setResolvedProviderSymbol(trade.getContractSymbol());
        replay.setProvider("databento");
        replay.setCandleInterval("1m");
        replay.setWindowStart(Instant.parse("2026-06-08T10:10:00Z"));
        replay.setWindowEnd(Instant.parse("2026-06-08T10:58:00Z"));
        replay.setEligibleAt(Instant.parse("2026-06-09T10:28:00Z"));
        replay.setEntryTimestamp(Instant.parse("2026-06-08T10:25:00Z"));
        replay.setFinalExitTimestamp(Instant.parse("2026-06-08T10:28:00Z"));
        if (status == TradeReplayStatus.FAILED) {
            replay.setFailureCode("TEST_FAILURE");
            replay.setSanitizedFailureMessage("Previous replay failed.");
        }
        return replay;
    }

    private void makeReplayStale(TradeReplay replay) {
        jdbcTemplate.update("update trade_replays set updated_at = ? where id = ?",
                java.sql.Timestamp.from(Instant.now().minusSeconds(600)),
                replay.getId());
    }
}
