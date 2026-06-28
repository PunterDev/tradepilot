package com.tradepilot;

import com.tradepilot.dto.account.TradingAccountRequest;
import com.tradepilot.dto.account.TradingAccountResponse;
import com.tradepilot.dto.account.TradingAccountRiskRequest;
import com.tradepilot.dto.stats.DashboardSummaryResponse;
import com.tradepilot.dto.stats.MetricItemResponse;
import com.tradepilot.analytics.TradeAnalyticsService;
import com.tradepilot.analytics.dto.CalendarSummaryDto;
import com.tradepilot.entity.Trade;
import com.tradepilot.entity.TradingAccount;
import com.tradepilot.entity.User;
import com.tradepilot.enums.*;
import com.tradepilot.repository.TradeRepository;
import com.tradepilot.repository.TradingAccountRepository;
import com.tradepilot.repository.UserRepository;
import com.tradepilot.service.StatsService;
import com.tradepilot.service.TradingAccountService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class RMultipleAnalyticsTests {

    @Autowired UserRepository userRepository;
    @Autowired TradingAccountRepository tradingAccountRepository;
    @Autowired TradeRepository tradeRepository;
    @Autowired TradingAccountService tradingAccountService;
    @Autowired StatsService statsService;
    @Autowired TradeAnalyticsService tradeAnalyticsService;

    @Test
    void accountDefaultRiskCalculatesAverageR() {
        User user = user("account-risk@example.com");
        TradingAccount account = account(user, "Account Risk", new BigDecimal("200.00"));
        trade(user, account, "NQ", new BigDecimal("400.00"), null);
        trade(user, account, "ES", new BigDecimal("-100.00"), null);

        DashboardSummaryResponse summary = statsService.getDashboardSummary(user, account.getId(), null, null);
        MetricItemResponse setup = statsService.getStatsBySetup(user, account.getId(), null, null).get(0);

        assertThat(summary.getAverageR()).isEqualByComparingTo("0.75");
        assertThat(setup.getAverageR()).isEqualByComparingTo("0.75");
    }

    @Test
    void tradeRiskOverridesAccountDefaultRisk() {
        User user = user("trade-risk@example.com");
        TradingAccount account = account(user, "Override Risk", new BigDecimal("200.00"));
        trade(user, account, "NQ", new BigDecimal("400.00"), new BigDecimal("100.00"));

        DashboardSummaryResponse summary = statsService.getDashboardSummary(user, account.getId(), null, null);

        assertThat(summary.getAverageR()).isEqualByComparingTo("4.00");
    }

    @Test
    void missingRiskReturnsNullAverageRInsteadOfZero() {
        User user = user("missing-risk@example.com");
        TradingAccount account = account(user, "Missing Risk", null);
        trade(user, account, "NQ", new BigDecimal("400.00"), null);

        DashboardSummaryResponse summary = statsService.getDashboardSummary(user, account.getId(), null, null);
        MetricItemResponse setup = statsService.getStatsBySetup(user, account.getId(), null, null).get(0);

        assertThat(summary.getAverageR()).isNull();
        assertThat(setup.getAverageR()).isNull();
    }

    @Test
    void calendarSummaryIncludesDailyTotalRWhenRiskIsAvailable() {
        User user = user("calendar-risk@example.com");
        TradingAccount account = account(user, "Calendar Risk", new BigDecimal("200.00"));
        trade(user, account, "NQ", new BigDecimal("400.00"), null);
        trade(user, account, "ES", new BigDecimal("-100.00"), new BigDecimal("100.00"));

        CalendarSummaryDto day = tradeAnalyticsService.getCalendarSummary(user, account.getId()).get(0);

        assertThat(day.getNetPnl()).isEqualByComparingTo("300.00");
        assertThat(day.getTotalR()).isEqualByComparingTo("1.00");
    }

    @Test
    void calendarSummaryLeavesDailyTotalRNullWhenRiskIsUnavailable() {
        User user = user("calendar-missing-risk@example.com");
        TradingAccount account = account(user, "Calendar Missing Risk", null);
        trade(user, account, "NQ", new BigDecimal("400.00"), null);

        CalendarSummaryDto day = tradeAnalyticsService.getCalendarSummary(user, account.getId()).get(0);

        assertThat(day.getNetPnl()).isEqualByComparingTo("400.00");
        assertThat(day.getTotalR()).isNull();
    }

    @Test
    void invalidAccountRiskIsRejected() {
        User user = user("invalid-risk@example.com");
        TradingAccountResponse account = tradingAccountService.create(
                user,
                new TradingAccountRequest("Invalid Risk", PropFirmType.TOPSTEP, null, null, BigDecimal.valueOf(50_000), null, TradingAccountStatus.ACTIVE)
        );

        assertThatThrownBy(() -> tradingAccountService.updateRiskAmount(user, account.getId(), new TradingAccountRiskRequest(BigDecimal.ZERO)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("greater than 0");
    }

    private User user(String email) {
        return userRepository.save(User.builder()
                .name("R Multiple")
                .email(email)
                .passwordHash("test-password-hash")
                .emailVerified(true)
                .role(UserRole.USER)
                .plan(com.tradepilot.billing.PlanType.PRO)
                .subscriptionStatus(com.tradepilot.billing.SubscriptionStatus.ACTIVE)
                .build());
    }

    private TradingAccount account(User user, String name, BigDecimal defaultRiskAmount) {
        return tradingAccountRepository.save(TradingAccount.builder()
                .user(user)
                .name(name)
                .propFirm(PropFirmType.TOPSTEP)
                .accountType(TradingAccountType.PERSONAL)
                .startingBalance(BigDecimal.valueOf(50_000))
                .defaultRiskAmount(defaultRiskAmount)
                .status(TradingAccountStatus.ACTIVE)
                .build());
    }

    private void trade(User user, TradingAccount account, String symbol, BigDecimal pnl, BigDecimal riskAmount) {
        LocalDate date = LocalDate.of(2026, 6, 22);
        LocalDateTime entryTime = date.atTime(9, 30).plusMinutes(tradeRepository.count());
        tradeRepository.save(Trade.builder()
                .user(user)
                .account(account)
                .symbol(symbol)
                .market("FUTURES")
                .tradeDate(date)
                .entryTime(entryTime)
                .exitTime(entryTime.plusMinutes(5))
                .side("LONG")
                .setup(SetupType.ORB)
                .session(SessionType.NY_AM)
                .entryPrice(new BigDecimal("100.00"))
                .exitPrice(new BigDecimal("101.00"))
                .quantity(1)
                .riskAmount(riskAmount)
                .pnlAmount(pnl)
                .outcome(pnl.signum() >= 0 ? OutcomeType.WIN : OutcomeType.LOSS)
                .externalTradeKey(account.getId() + "-" + symbol + "-" + pnl)
                .build());
    }
}
