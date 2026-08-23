package com.tradepilot;

import com.tradepilot.billing.PlanType;
import com.tradepilot.billing.SubscriptionStatus;
import com.tradepilot.entity.User;
import com.tradepilot.enums.UserRole;
import com.tradepilot.featureusage.FeatureKey;
import com.tradepilot.featureusage.FeatureUsageAction;
import com.tradepilot.featureusage.FeatureUsageEvent;
import com.tradepilot.featureusage.FeatureUsageEventType;
import com.tradepilot.featureusage.FeatureUsageMetadataRegistry;
import com.tradepilot.featureusage.FeatureUsageRepository;
import com.tradepilot.featureusage.FeatureUsageService;
import com.tradepilot.repository.TradeRepository;
import com.tradepilot.repository.UserRepository;
import com.tradepilot.service.AggregateCacheService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
class FeatureUsageTelemetryTests {
    @Autowired FeatureUsageService featureUsageService;
    @Autowired FeatureUsageRepository featureUsageRepository;
    @Autowired UserRepository userRepository;

    @Test
    void everySupportedFeatureKeyCanBePersisted() {
        User user = userRepository.save(User.builder()
                .name("Feature Usage User")
                .email("feature-usage-all-values@example.com")
                .passwordHash("test-password-hash")
                .emailVerified(true)
                .role(UserRole.USER)
                .plan(PlanType.PRO)
                .subscriptionStatus(SubscriptionStatus.ACTIVE)
                .build());

        for (FeatureKey feature : FeatureKey.values()) {
            featureUsageService.track(user, feature);
        }

        Set<FeatureKey> persisted = featureUsageRepository.findAll().stream()
                .filter(event -> user.getId().equals(event.getUserId()))
                .map(FeatureUsageEvent::getFeature)
                .collect(Collectors.toSet());

        assertThat(persisted).containsExactlyInAnyOrder(FeatureKey.values());
    }

    @Test
    void everySupportedFeatureUsageActionCanBePersisted() {
        User user = userRepository.save(User.builder()
                .name("Feature Usage Action User")
                .email("feature-usage-all-actions@example.com")
                .passwordHash("test-password-hash")
                .emailVerified(true)
                .role(UserRole.USER)
                .plan(PlanType.PRO)
                .subscriptionStatus(SubscriptionStatus.ACTIVE)
                .build());

        for (FeatureUsageAction action : FeatureUsageAction.values()) {
            featureUsageService.track(user, FeatureKey.TRADING_MEMORY, action, FeatureUsageEventType.USER_ACTION, null, null);
        }

        Set<FeatureUsageAction> persisted = featureUsageRepository.findAll().stream()
                .filter(event -> user.getId().equals(event.getUserId()))
                .map(FeatureUsageEvent::getAction)
                .collect(Collectors.toSet());

        assertThat(persisted).containsExactlyInAnyOrder(FeatureUsageAction.values());
        assertThat(persisted).contains(FeatureUsageAction.TRADING_MEMORY_VIEWED);
    }

    @Test
    void telemetryPersistenceFailureIsFailOpen() {
        FeatureUsageRepository failingRepository = mock(FeatureUsageRepository.class);
        when(failingRepository.saveAndFlush(any(FeatureUsageEvent.class)))
                .thenThrow(new DataIntegrityViolationException("constraint rejected feature"));
        FeatureUsageService service = new FeatureUsageService(
                failingRepository,
                mock(UserRepository.class),
                mock(TradeRepository.class),
                mock(AggregateCacheService.class),
                new NoopTransactionManager(),
                new FeatureUsageMetadataRegistry()
        );
        User user = new User();
        user.setId(42L);

        assertThatCode(() -> service.track(user, FeatureKey.PERFORMANCE_TIMELINE))
                .doesNotThrowAnyException();
        verify(failingRepository).saveAndFlush(any(FeatureUsageEvent.class));
    }

    private static final class NoopTransactionManager extends AbstractPlatformTransactionManager {
        @Override
        protected Object doGetTransaction() {
            return new Object();
        }

        @Override
        protected void doBegin(Object transaction, TransactionDefinition definition) {
        }

        @Override
        protected void doCommit(DefaultTransactionStatus status) {
        }

        @Override
        protected void doRollback(DefaultTransactionStatus status) {
        }
    }
}
