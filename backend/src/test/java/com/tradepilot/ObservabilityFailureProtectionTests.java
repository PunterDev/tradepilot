package com.tradepilot;

import com.tradepilot.analytics.TradeAnalyticsService;
import com.tradepilot.analytics.dto.CalendarSummaryDto;
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
import com.tradepilot.enums.UserRole;
import com.tradepilot.observability.QueryPerformanceEventRecorder;
import com.tradepilot.repository.TradeRepository;
import com.tradepilot.repository.TradingAccountRepository;
import com.tradepilot.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@SpringBootTest(properties = "app.observability.slow-query-threshold-ms=-1")
@ActiveProfiles("test")
class ObservabilityFailureProtectionTests {
    private static final AtomicInteger SEQUENCE = new AtomicInteger();

    @Autowired TradeAnalyticsService tradeAnalyticsService;
    @Autowired UserRepository userRepository;
    @Autowired TradingAccountRepository tradingAccountRepository;
    @Autowired TradeRepository tradeRepository;

    @MockBean QueryPerformanceEventRecorder queryPerformanceEventRecorder;

    @Test
    void observabilityInsertFailureDoesNotBreakCalendarSummary() {
        Fixture fixture = fixture("failure-protection");
        saveTrade(fixture, "ES", "75.00", LocalDate.of(2026, 6, 2), 10);
        doThrow(new RuntimeException("cannot execute INSERT in a read-only transaction"))
                .when(queryPerformanceEventRecorder).record(eq("calendar_summary"), anyLong());

        List<CalendarSummaryDto> summary = tradeAnalyticsService.getCalendarSummary(fixture.user(), fixture.account().getId());

        assertThat(summary).hasSize(1);
        assertThat(summary.get(0).getDate()).isEqualTo(LocalDate.of(2026, 6, 2));
        assertThat(summary.get(0).getTradeCount()).isEqualTo(1);
        assertThat(summary.get(0).getNetPnl()).isEqualByComparingTo("75.00");
        verify(queryPerformanceEventRecorder).record(eq("calendar_summary"), anyLong());
    }

    private Fixture fixture(String name) {
        int id = SEQUENCE.incrementAndGet();
        User user = userRepository.save(User.builder()
                .name("Observability " + name)
                .email("observability-" + name + "-" + id + "@example.com")
                .passwordHash("test-password-hash")
                .emailVerified(true)
                .role(UserRole.USER)
                .plan(PlanType.PRO)
                .subscriptionStatus(SubscriptionStatus.ACTIVE)
                .build());
        TradingAccount account = tradingAccountRepository.save(TradingAccount.builder()
                .user(user)
                .name("Observability Account " + name)
                .propFirm(PropFirmType.TOPSTEP)
                .accountType(TradingAccountType.PERSONAL)
                .startingBalance(BigDecimal.valueOf(50_000))
                .status(TradingAccountStatus.ACTIVE)
                .build());
        return new Fixture(user, account);
    }

    private void saveTrade(Fixture fixture, String symbol, String pnl, LocalDate date, int entryHour) {
        BigDecimal pnlAmount = new BigDecimal(pnl);
        LocalDateTime entryTime = date.atTime(entryHour, 0);
        tradeRepository.save(Trade.builder()
                .user(fixture.user())
                .account(fixture.account())
                .symbol(symbol)
                .market("FUTURES")
                .tradeDate(date)
                .entryTime(entryTime)
                .exitTime(entryTime.plusMinutes(5))
                .side("LONG")
                .setup(SetupType.OTHER)
                .session(SessionType.NY_AM)
                .entryPrice(BigDecimal.valueOf(100))
                .exitPrice(BigDecimal.valueOf(101))
                .quantity(1)
                .riskAmount(BigDecimal.valueOf(100))
                .pnlAmount(pnlAmount)
                .pnlR(pnlAmount.divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP))
                .outcome(pnlAmount.signum() >= 0 ? OutcomeType.WIN : OutcomeType.LOSS)
                .followedPlan(true)
                .externalTradeKey(fixture.user().getId() + "-" + symbol + "-" + date + "-" + entryHour)
                .build());
    }

    private record Fixture(User user, TradingAccount account) {
    }
}
