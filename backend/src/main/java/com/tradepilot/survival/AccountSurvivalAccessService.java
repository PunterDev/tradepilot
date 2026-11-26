package com.tradepilot.survival;

import com.tradepilot.billing.Feature;
import com.tradepilot.billing.FeatureAccessService;
import com.tradepilot.config.FeatureDisabledException;
import com.tradepilot.config.FeatureProperties;
import com.tradepilot.entitlement.FeatureEntitlementKey;
import com.tradepilot.entitlement.FeatureEntitlementService;
import com.tradepilot.entity.User;
import com.tradepilot.survival.dto.AccountSurvivalAccessResponse;
import org.springframework.stereotype.Service;

@Service
public class AccountSurvivalAccessService {
    private final FeatureProperties featureProperties;
    private final FeatureEntitlementService entitlementService;
    private final FeatureAccessService featureAccessService;

    public AccountSurvivalAccessService(FeatureProperties featureProperties,
            FeatureEntitlementService entitlementService, FeatureAccessService featureAccessService) {
        this.featureProperties = featureProperties;
        this.entitlementService = entitlementService;
        this.featureAccessService = featureAccessService;
    }

    public Decision decision(User user) {
        AccountSurvivalRolloutMode mode = AccountSurvivalRolloutMode.fromConfiguration(
                featureProperties.getAccountSurvivalRolloutMode());
        boolean rolloutAvailable = switch (mode) {
            case DISABLED -> false;
            case BETA -> entitlementService.hasEntitlement(user, FeatureEntitlementKey.ACCOUNT_SURVIVAL_BETA);
            case GENERAL_AVAILABILITY -> true;
        };
        boolean pro = featureAccessService.hasFeature(user, Feature.ACCOUNT_SURVIVAL);
        return new Decision(mode, rolloutAvailable, rolloutAvailable && pro);
    }

    public void requirePremiumMutation(User user) {
        Decision decision = decision(user);
        if (!decision.rolloutAvailable()) {
            throw new FeatureDisabledException(Feature.ACCOUNT_SURVIVAL);
        }
        featureAccessService.requireFeature(user, Feature.ACCOUNT_SURVIVAL);
    }

    public AccountSurvivalAccessResponse response(User user) {
        Decision decision = decision(user);
        boolean upgradeRequired = decision.rolloutAvailable() && !decision.premiumAccess();
        String message = !decision.rolloutAvailable()
                ? "Account Survival premium actions are not available in this release. Existing records remain read-only."
                : upgradeRequired
                        ? "Upgrade to Pro to create commitments and evaluations. Existing records remain read-only."
                        : "Account Survival premium actions are available.";
        return new AccountSurvivalAccessResponse(decision.mode().name(), decision.rolloutAvailable(),
                decision.premiumAccess(), !decision.premiumAccess(), upgradeRequired, message);
    }

    public record Decision(AccountSurvivalRolloutMode mode, boolean rolloutAvailable, boolean premiumAccess) {
    }
}
