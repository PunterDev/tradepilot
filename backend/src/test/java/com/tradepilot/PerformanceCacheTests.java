package com.tradepilot;

import com.tradepilot.analytics.TradeAnalyticsService;
import com.tradepilot.analytics.dto.HeatmapCellDto;
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
import com.tradepilot.repository.TradeRepository;
import com.tradepilot.repository.TradingAccountRepository;
import com.tradepilot.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class PerformanceCacheTests {
    private static final AtomicInteger SEQUENCE = new AtomicInteger();

    @Autowired TradeAnalyticsService tradeAnalyticsService;
    @Autowired UserRepository userRepository;
    @Autowired TradingAccountRepository tradingAccountRepository;
    @Autowired TradeRepository tradeRepository;

    @Test
    void analyticsHeatmapCacheIsScopedByUserAndAccount() {
        Fixture first = fixture("analytics-cache-a");
        Fixture second = fixture("analytics-cache-b");
        saveTrade(first, "NQ", "100.00", LocalDate.of(2026, 5, 15), 9);
        saveTrade(second, "NQ", "200.00", LocalDate.of(2026, 5, 15), 14);

        List<HeatmapCellDto> firstInitial = tradeAnalyticsService.getHeatmap(first.user(), first.account().getId());
        List<HeatmapCellDto> secondInitial = tradeAnalyticsService.getHeatmap(second.user(), second.account().getId());

        saveTrade(first, "ES", "300.00", LocalDate.of(2026, 5, 18), 14);

        List<HeatmapCellDto> firstCached = tradeAnalyticsService.getHeatmap(first.user(), first.account().getId());
        List<HeatmapCellDto> secondCached = tradeAnalyticsService.getHeatmap(second.user(), second.account().getId());

        assertThat(firstInitial).hasSize(1);
        assertThat(firstCached).hasSize(1);
        assertThat(secondInitial).hasSize(1);
        assertThat(secondCached).hasSize(1);
        assertThat(firstCached.get(0).getTotalPnl()).isEqualByComparingTo("100.00");
        assertThat(secondCached.get(0).getTotalPnl()).isEqualByComparingTo("200.00");
    }

    private Fixture fixture(String name) {
        int id = SEQUENCE.incrementAndGet();
        User user = userRepository.save(User.builder()
                .name("Performance " + name)
                .email("performance-" + name + "-" + id + "@example.com")
                .passwordHash("test-password-hash")
                .emailVerified(true)
                .role(UserRole.USER)
                .plan(PlanType.PRO)
                .subscriptionStatus(SubscriptionStatus.ACTIVE)
                .build());
        TradingAccount account = tradingAccountRepository.save(TradingAccount.builder()
                .user(user)
                .name("Performance Account " + name)
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
                .session(entryHour >= 13 ? SessionType.NY_PM : SessionType.NY_AM)
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
