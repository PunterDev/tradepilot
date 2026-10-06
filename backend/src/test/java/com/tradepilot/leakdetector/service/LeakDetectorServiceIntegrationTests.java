package com.tradepilot.leakdetector.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradepilot.billing.FeatureAccessService;
import com.tradepilot.billing.PlanType;
import com.tradepilot.billing.UsageLimitService;
import com.tradepilot.entity.Trade;
import com.tradepilot.entity.TradingAccount;
import com.tradepilot.entity.User;
import com.tradepilot.enums.OutcomeType;
import com.tradepilot.enums.SessionType;
import com.tradepilot.leakdetector.dto.LeakDetectorResponse;
import com.tradepilot.leakdetector.dto.LeakResultDto;
import com.tradepilot.leakdetector.dto.LeakType;
import com.tradepilot.leakdetector.entity.AiLeakDetectorSnapshot;
import com.tradepilot.leakdetector.repository.AiLeakDetectorSnapshotRepository;
import com.tradepilot.observability.ObservabilityService;
import com.tradepilot.repository.TradeRepository;
import com.tradepilot.service.TradingAccountService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LeakDetectorServiceIntegrationTests {
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Test
    void persistsQualifiedPostLossFindingThroughTheProductionServicePath() throws Exception {
        List<Trade> trades = postLossTrades();
        AiLeakDetectorSnapshotRepository snapshots = mock(AiLeakDetectorSnapshotRepository.class);
        TradeRepository tradeRepository = mock(TradeRepository.class);
        TradingAccountService accounts = mock(TradingAccountService.class);
        User user = User.builder().id(44L).email("insight@example.com").name("Insight").passwordHash("x").plan(PlanType.PRO).build();
        when(accounts.getAccountForImport(eq(user), eq(7L))).thenReturn(mock(TradingAccount.class));
        when(tradeRepository.findByUserAndAccount_IdOrderByTradeDateAscEntryTimeAscIdAsc(user, 7L)).thenReturn(trades);
        when(snapshots.findMatchingSnapshot(anyLong(), anyLong(), any(), any())).thenReturn(List.of());
        when(snapshots.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        LeakDetectorService service = service(accounts, tradeRepository, snapshots);
        LeakDetectorResponse response = service.getLeakDetector(user, 7L);
        LeakResultDto mapped = behavioral(response, LeakType.POST_LOSS_OVERTRADING);
        List<LeakResultDto> persisted = objectMapper.readValue(org.mockito.Mockito.mockingDetails(snapshots).getInvocations().stream()
                .filter(invocation -> invocation.getMethod().getName().equals("save"))
                .map(invocation -> ((AiLeakDetectorSnapshot) invocation.getArgument(0)).getResultJson())
                .findFirst().orElseThrow(), new TypeReference<>() {});

        assertThat(mapped.getBehavioralFinding().qualified()).isTrue();
        assertThat(mapped.getBehavioralFinding().occurrenceCount()).isEqualTo(5);
        assertThat(mapped.getBehavioralFinding().baseline().sampleSize()).isPositive();
        assertThat(mapped.getBehavioralFinding().triggered().sampleSize()).isEqualTo(5);
        assertThat(mapped.getBehavioralFinding().counterfactual()).isNotNull();
        assertThat(mapped.getBehavioralFinding().engineVersion()).isEqualTo(BehavioralFindingAnalyzer.ENGINE_VERSION);
        assertThat(persisted.stream().filter(leak -> leak.getLeakType() == LeakType.POST_LOSS_OVERTRADING).findFirst().orElseThrow().getBehavioralFinding()).isEqualTo(mapped.getBehavioralFinding());
    }

    @Test
    void mapsRealQualifiedSizeEscalationAndOvertradingFindingsWithoutFabricatingOthers() {
        LeakDetectorService service = service(mock(TradingAccountService.class), mock(TradeRepository.class), mock(AiLeakDetectorSnapshotRepository.class));

        assertThat(behavioral(service.analyze(7L, sizeEscalationTrades(), "size"), LeakType.SIZE_ESCALATION_AFTER_LOSS).getBehavioralFinding().qualified()).isTrue();
        LeakResultDto overtrading = behavioral(service.analyze(7L, overtradingTrades(), "over"), LeakType.OVERTRADING);
        assertThat(overtrading.getBehavioralFinding().qualified()).isTrue();
        assertThat(overtrading.getBehavioralFinding().threshold()).isEqualTo(3);
        assertThat(overtrading.getBehavioralFinding().triggered().sampleSize()).isEqualTo(5);
    }

    private LeakDetectorService service(TradingAccountService accounts, TradeRepository trades, AiLeakDetectorSnapshotRepository snapshots) {
        return new LeakDetectorService(mock(FeatureAccessService.class), mock(UsageLimitService.class), accounts, trades, snapshots, objectMapper, mock(ObservabilityService.class));
    }

    private LeakResultDto behavioral(LeakDetectorResponse response, LeakType type) {
        return response.getLeaks().stream().filter(leak -> leak.getLeakType() == type && leak.getBehavioralFinding() != null).findFirst().orElseThrow();
    }

    private List<Trade> postLossTrades() {
        List<Trade> trades = new ArrayList<>();
        for (int day = 0; day < 5; day++) {
            LocalDate date = LocalDate.of(2026, 1, 5).plusDays(day);
            trades.add(trade(date, "09:00", "100", 1));
            trades.add(trade(date, "09:05", "-10", 1));
            trades.add(trade(date, "09:10", "-10", 1));
            trades.add(trade(date, "09:15", "-100", 1));
        }
        return trades;
    }

    private List<Trade> sizeEscalationTrades() {
        List<Trade> trades = new ArrayList<>();
        for (int day = 0; day < 5; day++) {
            LocalDate date = LocalDate.of(2026, 2, 2).plusDays(day);
            trades.add(trade(date, "09:00", "100", 1));
            trades.add(trade(date, "09:05", "-10", 1));
            trades.add(trade(date, "09:10", "-100", 2));
        }
        return trades;
    }

    private List<Trade> overtradingTrades() {
        List<Trade> trades = new ArrayList<>();
        for (int day = 0; day < 10; day++) {
            LocalDate date = LocalDate.of(2026, 3, 2).plusDays(day);
            int count = day < 5 ? 2 : 4;
            for (int index = 0; index < count; index++) trades.add(trade(date, String.format("09:%02d", index * 5), index == 3 ? "-100" : "100", 1));
        }
        return trades;
    }

    private Trade trade(LocalDate date, String time, String pnl, int quantity) {
        LocalDateTime entry = LocalDateTime.of(date, LocalTime.parse(time));
        return Trade.builder().symbol("NQ").market("FUTURES").tradeDate(date).entryTime(entry).exitTime(entry).side("LONG").session(SessionType.NY_AM).quantity(quantity).pnlAmount(new BigDecimal(pnl)).outcome(new BigDecimal(pnl).signum() < 0 ? OutcomeType.LOSS : OutcomeType.WIN).build();
    }
}
