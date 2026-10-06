package com.tradepilot.coach.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradepilot.activation.UserFeaturePreviewService;
import com.tradepilot.billing.FeatureAccessService;
import com.tradepilot.billing.PlanType;
import com.tradepilot.billing.UsageLimitService;
import com.tradepilot.coach.dto.CoachSnapshotDto;
import com.tradepilot.coach.entity.AiCoachSnapshot;
import com.tradepilot.coach.entity.CoachSnapshotStatus;
import com.tradepilot.coach.entity.CoachSnapshotType;
import com.tradepilot.coach.repository.AiCoachSnapshotRepository;
import com.tradepilot.entity.User;
import com.tradepilot.featureusage.FeatureUsageService;
import com.tradepilot.leakdetector.dto.BehavioralBaselineDto;
import com.tradepilot.leakdetector.dto.BehavioralCounterfactualDto;
import com.tradepilot.leakdetector.dto.BehavioralFindingDto;
import com.tradepilot.leakdetector.dto.EvidenceStrength;
import com.tradepilot.leakdetector.dto.LeakResultDto;
import com.tradepilot.leakdetector.dto.LeakSeverity;
import com.tradepilot.leakdetector.dto.LeakType;
import com.tradepilot.leakdetector.entity.AiLeakDetectorSnapshot;
import com.tradepilot.leakdetector.repository.AiLeakDetectorSnapshotRepository;
import com.tradepilot.observability.ObservabilityService;
import com.tradepilot.service.TradingAccountService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CoachLeakCompatibilityTests {
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Test
    void dailyCoachPreservesLegacySnapshotsAndUsesSafeStructuredContext() throws Exception {
        AiCoachService service = service(legacyLeak());
        CoachSnapshotDto response = service.getDailyCoach(user(), 7L, LocalDate.of(2026, 10, 1));

        assertThat(response.getTopLeakToWatch().getBehavioralFinding()).isNull();
        assertThat(response.getHistoricalPatternToWatch()).contains("Account-level pattern");
    }

    @Test
    void dailyAndWeeklyPreserveQualifiedDeterministicFindingWithoutPositiveClaimForNegativeCounterfactual() throws Exception {
        LeakResultDto leak = behavioralLeak(true, false);
        AiCoachService service = service(leak);

        CoachSnapshotDto daily = service.getDailyCoach(user(), 7L, LocalDate.of(2026, 10, 1));
        CoachSnapshotDto weekly = service.getWeeklyCoach(user(), 7L, LocalDate.of(2026, 9, 28));

        for (CoachSnapshotDto response : List.of(daily, weekly)) {
            assertThat(response.getTopLeakToWatch().getLeakType()).isEqualTo(LeakType.POST_LOSS_OVERTRADING);
            assertThat(response.getTopLeakToWatch().getBehavioralFinding()).isEqualTo(leak.getBehavioralFinding());
            assertThat(response.getHistoricalPatternToWatch()).contains("would not have improved historical results").doesNotContain("saved");
        }
    }

    @Test
    void dailyAndWeeklyDoNotPromoteInsufficientEvidence() throws Exception {
        AiCoachService service = service(behavioralLeak(false, false));

        assertThat(service.getDailyCoach(user(), 7L, LocalDate.of(2026, 10, 1)).getTopLeakToWatch()).isNull();
        assertThat(service.getWeeklyCoach(user(), 7L, LocalDate.of(2026, 9, 28)).getTopLeakToWatch()).isNull();
    }

    private AiCoachService service(LeakResultDto leak) throws Exception {
        AiCoachSnapshotRepository coaches = mock(AiCoachSnapshotRepository.class);
        AiLeakDetectorSnapshotRepository leaks = mock(AiLeakDetectorSnapshotRepository.class);
        TradingAccountService accounts = mock(TradingAccountService.class);
        User user = user();
        when(accounts.getAccountForImport(eq(user), eq(7L))).thenReturn(mock(com.tradepilot.entity.TradingAccount.class));
        when(coaches.findTopByUserIdAndAccountIdAndSnapshotTypeAndPeriodStartAndPeriodEndOrderByCreatedAtDesc(any(), any(), any(), any(), any())).thenAnswer(invocation -> Optional.of(coachSnapshot((CoachSnapshotType) invocation.getArgument(2), (LocalDate) invocation.getArgument(3), (LocalDate) invocation.getArgument(4))));
        when(leaks.findTopByUserIdAndAccountIdOrderByUpdatedAtDesc(44L, 7L)).thenReturn(Optional.of(AiLeakDetectorSnapshot.builder().userId(44L).accountId(7L).dataHash("leak").totalTrades(20).resultJson(objectMapper.writeValueAsString(List.of(leak))).model("test").build()));
        return new AiCoachService(coaches, new CoachSnapshotMapper(objectMapper), mock(AiCoachAsyncWorker.class), accounts, mock(UsageLimitService.class), mock(FeatureAccessService.class), mock(ObservabilityService.class), leaks, objectMapper, mock(CoachAnalyticsAggregator.class), mock(CoachHashService.class), mock(UserFeaturePreviewService.class), mock(FeatureUsageService.class));
    }

    private AiCoachSnapshot coachSnapshot(CoachSnapshotType type, LocalDate start, LocalDate end) {
        return AiCoachSnapshot.builder().id(1L).userId(44L).accountId(7L).snapshotType(type).periodStart(start).periodEnd(end).status(CoachSnapshotStatus.PENDING).build();
    }

    private User user() { return User.builder().id(44L).name("Coach").email("coach@example.com").passwordHash("x").plan(PlanType.PRO).build(); }

    private LeakResultDto legacyLeak() { return LeakResultDto.of(LeakType.LARGE_LOSS, "Large loss", "legacy summary", "", 3, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, null, null, null, false, 70, LeakSeverity.HIGH, "Rule", 3, List.of(), LocalDateTime.now()); }

    private LeakResultDto behavioralLeak(boolean qualified, boolean supported) {
        LeakResultDto leak = LeakResultDto.of(LeakType.POST_LOSS_OVERTRADING, "Post-loss overtrading", "legacy summary", "", 10, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, null, null, null, false, 70, LeakSeverity.HIGH, "Rule", 10, List.of(), LocalDateTime.now());
        leak.setBehavioralFinding(new BehavioralFindingDto(qualified, qualified ? null : "INSUFFICIENT_EVIDENCE", qualified ? 10 : 2, 40, 10, qualified ? EvidenceStrength.MODERATE : EvidenceStrength.INSUFFICIENT,
                new BehavioralBaselineDto(30, BigDecimal.valueOf(55), BigDecimal.valueOf(.5), BigDecimal.valueOf(100)), new BehavioralBaselineDto(10, BigDecimal.valueOf(20), BigDecimal.valueOf(-.5), BigDecimal.valueOf(-100)),
                new BehavioralCounterfactualDto(true, supported, 4, 1, 3, BigDecimal.valueOf(300), BigDecimal.valueOf(100), BigDecimal.ZERO, BigDecimal.valueOf(-200), BigDecimal.valueOf(-200)), "insight-trust-v1", "post-loss-overtrading-v1", null));
        return leak;
    }
}
