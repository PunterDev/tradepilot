package com.tradepilot.backtest.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradepilot.backtest.entity.BacktestRun;
import com.tradepilot.backtest.model.BacktestConfig;
import com.tradepilot.backtest.repository.BacktestRunRepository;
import com.tradepilot.backtest.repository.BacktestTradeRepository;
import com.tradepilot.entitlement.FeatureEntitlementKey;
import com.tradepilot.entitlement.FeatureEntitlementService;
import com.tradepilot.entity.User;
import com.tradepilot.enums.UserRole;
import com.tradepilot.marketdata.repository.MarketBarRepository;
import com.tradepilot.marketdata.model.MarketInstrument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class BacktestEntitlementTests {
    private FeatureEntitlementService entitlements;
    private BacktestService service;
    private User founder;
    private User adminWithoutEntitlement;
    private User normalUser;

    @BeforeEach
    void setUp() {
        entitlements = mock(FeatureEntitlementService.class);
        BacktestRunRepository runs = mock(BacktestRunRepository.class);
        service = new BacktestService(runs, mock(BacktestTradeRepository.class), mock(BacktestDataService.class),
                mock(MarketBarRepository.class), new ObjectMapper(), mock(BacktestWorker.class), entitlements);
        founder = user(1L, UserRole.USER);
        adminWithoutEntitlement = user(2L, UserRole.ADMIN);
        normalUser = user(3L, UserRole.USER);
    }

    @Test
    void accessIsAnExplicitPrivateEntitlementRatherThanRoleOrPlan() {
        when(entitlements.hasEntitlement(founder, FeatureEntitlementKey.PLAYBOOK_BACKTEST_BETA)).thenReturn(true);
        when(entitlements.hasEntitlement(adminWithoutEntitlement, FeatureEntitlementKey.PLAYBOOK_BACKTEST_BETA)).thenReturn(false);

        assertThat(service.access(founder).allowed()).isTrue();
        assertThat(service.access(adminWithoutEntitlement).allowed()).isFalse();
        assertThat(service.access(founder).entitlementKey()).isEqualTo("PLAYBOOK_BACKTEST_BETA");
    }

    @Test
    void allReadSurfacesRequireThePrivateEntitlementBeforeAccessingData() {
        doThrow(new AccessDeniedException("blocked")).when(entitlements)
                .requireEntitlement(adminWithoutEntitlement, FeatureEntitlementKey.PLAYBOOK_BACKTEST_BETA);
        doThrow(new AccessDeniedException("blocked")).when(entitlements)
                .requireEntitlement(normalUser, FeatureEntitlementKey.PLAYBOOK_BACKTEST_BETA);

        for (User denied : List.of(adminWithoutEntitlement, normalUser)) {
            assertThatThrownBy(() -> service.list(denied)).isInstanceOf(AccessDeniedException.class);
            assertThatThrownBy(() -> service.get(denied, 10L)).isInstanceOf(AccessDeniedException.class);
            assertThatThrownBy(() -> service.trades(denied, 10L)).isInstanceOf(AccessDeniedException.class);
            assertThatThrownBy(() -> service.chart(denied, 10L, 20L, "1m")).isInstanceOf(AccessDeniedException.class);
        }
    }

    @Test
    void entitledFounderCanStartResearchWhileAnAdminWithoutEntitlementCannot() {
        BacktestConfig config = BacktestConfig.conservative(MarketInstrument.NQ, LocalDate.of(2026, 9, 7), LocalDate.of(2026, 9, 10));
        BacktestRun saved = new BacktestRun();
        BacktestRunRepository runs = mock(BacktestRunRepository.class);
        BacktestWorker worker = mock(BacktestWorker.class);
        BacktestService guardedService = new BacktestService(runs, mock(BacktestTradeRepository.class), mock(BacktestDataService.class),
                mock(MarketBarRepository.class), new ObjectMapper().findAndRegisterModules(), worker, entitlements);
        when(runs.save(any(BacktestRun.class))).thenReturn(saved);
        doThrow(new AccessDeniedException("blocked")).when(entitlements)
                .requireEntitlement(adminWithoutEntitlement, FeatureEntitlementKey.PLAYBOOK_BACKTEST_BETA);

        assertThatThrownBy(() -> guardedService.start(adminWithoutEntitlement, config)).isInstanceOf(AccessDeniedException.class);

        guardedService.start(founder, config);

        verify(entitlements).requireEntitlement(founder, FeatureEntitlementKey.PLAYBOOK_BACKTEST_BETA);
        verify(worker).run(any(), eq(config));
    }

    @Test
    void entitledFounderKeepsRunOwnershipScoping() {
        BacktestRunRepository runs = mock(BacktestRunRepository.class);
        BacktestService ownedService = new BacktestService(runs, mock(BacktestTradeRepository.class), mock(BacktestDataService.class),
                mock(MarketBarRepository.class), new ObjectMapper(), mock(BacktestWorker.class), entitlements);
        when(runs.findTop20ByUserIdOrderByCreatedAtDesc(founder.getId())).thenReturn(List.of());

        assertThat(ownedService.list(founder)).isEmpty();
        verify(entitlements).requireEntitlement(founder, FeatureEntitlementKey.PLAYBOOK_BACKTEST_BETA);
        verify(runs).findTop20ByUserIdOrderByCreatedAtDesc(founder.getId());
    }

    private User user(Long id, UserRole role) {
        return User.builder().id(id).email("user" + id + "@example.com").name("User " + id).passwordHash("hash").role(role).build();
    }
}
