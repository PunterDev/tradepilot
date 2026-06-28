package com.tradepilot;

import com.tradepilot.analytics.TradeAnalyticsService;
import com.tradepilot.billing.PlanType;
import com.tradepilot.billing.SubscriptionStatus;
import com.tradepilot.coach.entity.AiCoachSnapshot;
import com.tradepilot.coach.entity.CoachSnapshotStatus;
import com.tradepilot.coach.entity.CoachSnapshotType;
import com.tradepilot.coach.repository.AiCoachSnapshotRepository;
import com.tradepilot.coach.service.AiCoachService;
import com.tradepilot.dto.account.TradingAccountRequest;
import com.tradepilot.dto.account.TradingAccountResponse;
import com.tradepilot.dto.common.PagedResponse;
import com.tradepilot.dto.stats.DashboardSummaryResponse;
import com.tradepilot.dto.trade.TradeImportRequest;
import com.tradepilot.dto.trade.TradeJournalRequest;
import com.tradepilot.dto.trade.TradeResponse;
import com.tradepilot.entity.User;
import com.tradepilot.enums.OutcomeType;
import com.tradepilot.enums.PropFirmType;
import com.tradepilot.enums.SessionType;
import com.tradepilot.enums.SetupType;
import com.tradepilot.enums.TradingAccountStatus;
import com.tradepilot.exception.ResourceNotFoundException;
import com.tradepilot.repository.UserRepository;
import com.tradepilot.service.StatsService;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class UserDataIsolationTests {

    @Autowired
    UserRepository userRepository;

    @Autowired
    TradingAccountService tradingAccountService;

    @Autowired
    TradeService tradeService;

    @Autowired
    StatsService statsService;

    @Autowired
    TradeAnalyticsService tradeAnalyticsService;

    @Autowired
    AiCoachService aiCoachService;

    @Autowired
    AiCoachSnapshotRepository coachSnapshotRepository;

    @Autowired
    TradingDnaService tradingDnaService;

    @Test
    void usersOnlySeeTheirOwnAccountsTradesAnalyticsAndCoachSnapshots() {
        User userA = createUser("Trader A", "trader-a@example.com");
        User userB = createUser("Trader B", "trader-b@example.com");

        TradingAccountResponse accountA = tradingAccountService.create(
                userA,
                new TradingAccountRequest(
                        "A Topstep 50K",
                        PropFirmType.TOPSTEP,
                        "A-001",
                        null,
                        BigDecimal.valueOf(50_000),
                        null,
                        TradingAccountStatus.ACTIVE
                )
        );

        List<TradeResponse> userATrades = tradeService.importTrades(
                userA,
                List.of(importTrade(accountA.getId(), "same-broker-key", BigDecimal.valueOf(250)))
        );

        assertThat(userATrades).hasSize(1);
        TradeResponse journalUpdated = tradeService.updateTradeJournal(
                userA,
                userATrades.get(0).getId(),
                new TradeJournalRequest("IFVG", 4, "CALM", "Waited for displacement confirmation.")
        );
        assertThat(journalUpdated.getSetup()).isEqualTo(SetupType.IFVG);
        assertThat(journalUpdated.getConfidence()).isEqualTo(4);
        assertThat(journalUpdated.getEmotion()).isEqualTo("CALM");
        assertThat(journalUpdated.getNotes()).contains("displacement");
        assertThatThrownBy(() -> tradeService.updateTradeJournal(
                userB,
                userATrades.get(0).getId(),
                new TradeJournalRequest("FVG", 1, "FOMO", "Should not save.")
        )).isInstanceOf(ResourceNotFoundException.class);
        assertThat(tradingAccountService.getAccounts(userA)).extracting(TradingAccountResponse::getId).containsExactly(accountA.getId());
        assertThat(tradingAccountService.getAccounts(userB)).isEmpty();

        PagedResponse<TradeResponse> userBTradePage = tradeService.getTrades(
                userB,
                new com.tradepilot.dto.trade.TradeFilterRequest(),
                0,
                10,
                "tradeDate",
                "desc"
        );

        assertThat(userBTradePage.getTotalElements()).isZero();

        DashboardSummaryResponse userBSummary = statsService.getDashboardSummary(userB, null, null, null);
        assertThat(userBSummary.getTotalTrades()).isZero();
        assertThat(userBSummary.getTotalPnl()).isEqualByComparingTo(BigDecimal.ZERO);

        assertThat(tradeAnalyticsService.getHeatmap(userB, null)).isEmpty();
        assertThat(tradeAnalyticsService.getFridayOpexAnalytics(userB, null).getAllFridays().getTradeCount()).isZero();
        assertThatThrownBy(() -> tradingAccountService.getAccountForImport(userB, accountA.getId()))
                .isInstanceOf(ResourceNotFoundException.class);

        TradingAccountResponse accountB = tradingAccountService.create(
                userB,
                new TradingAccountRequest(
                        "B Apex 50K",
                        PropFirmType.APEX,
                        "B-001",
                        null,
                        BigDecimal.valueOf(50_000),
                        null,
                        TradingAccountStatus.ACTIVE
                )
        );

        List<TradeResponse> userBTrades = tradeService.importTrades(
                userB,
                List.of(importTrade(accountB.getId(), "same-broker-key", BigDecimal.valueOf(-100)))
        );

        assertThat(userBTrades).hasSize(1);
        assertThat(statsService.getDashboardSummary(userA, null, null, null).getTotalTrades()).isEqualTo(1);
        assertThat(statsService.getDashboardSummary(userB, null, null, null).getTotalTrades()).isEqualTo(1);

        coachSnapshotRepository.save(AiCoachSnapshot.builder()
                .userId(userA.getId())
                .accountId(accountA.getId())
                .snapshotType(CoachSnapshotType.DAILY)
                .periodStart(LocalDate.of(2026, 5, 15))
                .periodEnd(LocalDate.of(2026, 5, 15))
                .status(CoachSnapshotStatus.COMPLETED)
                .totalTrades(1)
                .netPnl(BigDecimal.valueOf(250))
                .reviewHash("hash-a")
                .build());

        assertThat(aiCoachService.getSnapshots(userA, accountA.getId(), CoachSnapshotType.DAILY)).hasSize(1);
        assertThat(aiCoachService.getSnapshots(userB, accountB.getId(), CoachSnapshotType.DAILY)).isEmpty();
        assertThatThrownBy(() -> aiCoachService.getSnapshots(userB, accountA.getId(), CoachSnapshotType.DAILY))
                .isInstanceOf(ResourceNotFoundException.class);

        TradingDnaResponse userADna = tradingDnaService.getTradingDna(userA, accountA.getId(), 30, false, false);
        TradingDnaResponse userBDna = tradingDnaService.getTradingDna(userB, accountB.getId(), 30, false, false);
        TradingDnaResponse userBDnaCached = tradingDnaService.getTradingDna(userB, accountB.getId(), 30, false, false);

        assertThat(userADna.getAccountId()).isEqualTo(accountA.getId());
        assertThat(userADna.getTotalTrades()).isEqualTo(1);
        assertThat(userBDna.getAccountId()).isEqualTo(accountB.getId());
        assertThat(userBDna.getTotalTrades()).isEqualTo(1);
        assertThat(userBDnaCached.getCached()).isTrue();
        TradingDnaResponse invalidScope = tradingDnaService.getTradingDna(userB, accountA.getId(), 30, false, false);
        assertThat(invalidScope.getStatus()).isEqualTo("INSUFFICIENT_DATA");
        assertThat(invalidScope.getTotalTrades()).isZero();
        assertThat(invalidScope.getMessage()).contains("Selected account is no longer available");
    }

    private User createUser(String name, String email) {
        return userRepository.save(User.builder()
                .name(name)
                .email(email)
                .passwordHash("test-password-hash")
                .emailVerified(true)
                .plan(PlanType.PRO)
                .subscriptionStatus(SubscriptionStatus.ACTIVE)
                .build());
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
                pnl.signum() >= 0 ? OutcomeType.WIN : OutcomeType.LOSS
        );
    }
}
