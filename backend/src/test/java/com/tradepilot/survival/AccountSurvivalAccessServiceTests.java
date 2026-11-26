package com.tradepilot.survival;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.tradepilot.billing.FeatureAccessException;
import com.tradepilot.billing.FeatureAccessService;
import com.tradepilot.billing.PlanType;
import com.tradepilot.config.FeatureDisabledException;
import com.tradepilot.config.FeatureProperties;
import com.tradepilot.entitlement.FeatureEntitlementKey;
import com.tradepilot.entitlement.FeatureEntitlementService;
import com.tradepilot.entity.User;
import com.tradepilot.enums.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AccountSurvivalAccessServiceTests {
    private final FeatureProperties properties = new FeatureProperties();
    private final FeatureAccessService featureAccessService = new FeatureAccessService();
    @Mock FeatureEntitlementService entitlements;
    private AccountSurvivalAccessService service;

    @BeforeEach
    void setUp() {
        service = new AccountSurvivalAccessService(properties, entitlements, featureAccessService);
    }

    @Test
    void disabledAndInvalidModesFailClosedForProUsers() {
        User pro = user(PlanType.PRO, UserRole.USER, 0);
        properties.setAccountSurvivalRolloutMode("disabled");
        assertThat(service.decision(pro).premiumAccess()).isFalse();
        assertThatThrownBy(() -> service.requirePremiumMutation(pro)).isInstanceOf(FeatureDisabledException.class);

        properties.setAccountSurvivalRolloutMode("unexpected-enable-value");
        assertThat(service.decision(pro).mode()).isEqualTo(AccountSurvivalRolloutMode.DISABLED);
        assertThatThrownBy(() -> service.requirePremiumMutation(pro)).isInstanceOf(FeatureDisabledException.class);
    }

    @Test
    void rolloutModeParserAcceptsOnlyDocumentedRepresentationsAndFailsClosed() {
        assertThat(AccountSurvivalRolloutMode.fromConfiguration(null)).isEqualTo(AccountSurvivalRolloutMode.DISABLED);
        assertThat(AccountSurvivalRolloutMode.fromConfiguration("")).isEqualTo(AccountSurvivalRolloutMode.DISABLED);
        assertThat(AccountSurvivalRolloutMode.fromConfiguration("   ")).isEqualTo(AccountSurvivalRolloutMode.DISABLED);
        assertThat(AccountSurvivalRolloutMode.fromConfiguration("disabled")).isEqualTo(AccountSurvivalRolloutMode.DISABLED);
        assertThat(AccountSurvivalRolloutMode.fromConfiguration("DISABLED")).isEqualTo(AccountSurvivalRolloutMode.DISABLED);
        assertThat(AccountSurvivalRolloutMode.fromConfiguration("BeTa")).isEqualTo(AccountSurvivalRolloutMode.BETA);
        assertThat(AccountSurvivalRolloutMode.fromConfiguration("general-availability"))
                .isEqualTo(AccountSurvivalRolloutMode.GENERAL_AVAILABILITY);
        assertThat(AccountSurvivalRolloutMode.fromConfiguration("GENERAL_AVAILABILITY"))
                .isEqualTo(AccountSurvivalRolloutMode.GENERAL_AVAILABILITY);
        assertThat(AccountSurvivalRolloutMode.fromConfiguration("General_Availability"))
                .isEqualTo(AccountSurvivalRolloutMode.GENERAL_AVAILABILITY);
        assertThat(AccountSurvivalRolloutMode.fromConfiguration("general availability"))
                .isEqualTo(AccountSurvivalRolloutMode.DISABLED);
        assertThat(AccountSurvivalRolloutMode.fromConfiguration("enabled"))
                .isEqualTo(AccountSurvivalRolloutMode.DISABLED);
        assertThat(AccountSurvivalRolloutMode.fromConfiguration("unknown"))
                .isEqualTo(AccountSurvivalRolloutMode.DISABLED);
    }

    @Test
    void betaRequiresBothExplicitAllowlistAndProPlan() {
        properties.setAccountSurvivalRolloutMode("beta");
        User includedPro = user(PlanType.PRO, UserRole.USER, 0);
        User excludedPro = user(PlanType.PRO, UserRole.USER, 0);
        User includedFree = user(PlanType.FREE, UserRole.USER, 100_000);
        when(entitlements.hasEntitlement(includedPro, FeatureEntitlementKey.ACCOUNT_SURVIVAL_BETA)).thenReturn(true);
        when(entitlements.hasEntitlement(excludedPro, FeatureEntitlementKey.ACCOUNT_SURVIVAL_BETA)).thenReturn(false);
        when(entitlements.hasEntitlement(includedFree, FeatureEntitlementKey.ACCOUNT_SURVIVAL_BETA)).thenReturn(true);

        assertThat(service.decision(includedPro).premiumAccess()).isTrue();
        assertThat(service.decision(excludedPro).rolloutAvailable()).isFalse();
        assertThat(service.decision(includedFree).rolloutAvailable()).isTrue();
        assertThat(service.decision(includedFree).premiumAccess()).isFalse();
        assertThatThrownBy(() -> service.requirePremiumMutation(includedFree)).isInstanceOf(FeatureAccessException.class);
    }

    @Test
    void generalAvailabilityUsesPlanAndNeverCreditsOrAdministratorRole() {
        properties.setAccountSurvivalRolloutMode("general-availability");
        User pro = user(PlanType.PRO, UserRole.USER, 0);
        User creditRichFree = user(PlanType.FREE, UserRole.USER, 100_000);
        User administrator = user(PlanType.FREE, UserRole.ADMIN, 100_000);

        assertThat(service.decision(pro).premiumAccess()).isTrue();
        assertThat(service.decision(creditRichFree).premiumAccess()).isFalse();
        assertThat(service.decision(administrator).premiumAccess()).isFalse();
        assertThatThrownBy(() -> service.requirePremiumMutation(administrator)).isInstanceOf(FeatureAccessException.class);
    }

    private User user(PlanType plan, UserRole role, int purchasedCredits) {
        return User.builder().email(role + "-" + plan + "-" + purchasedCredits + "@example.com")
                .passwordHash("hash").emailVerified(true).plan(plan).role(role)
                .purchasedAiCreditsBalance(purchasedCredits).build();
    }
}
