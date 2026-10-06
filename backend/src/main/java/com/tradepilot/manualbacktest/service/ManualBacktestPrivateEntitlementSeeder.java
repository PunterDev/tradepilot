package com.tradepilot.manualbacktest.service;

import com.tradepilot.entitlement.FeatureEntitlementKey;
import com.tradepilot.entitlement.FeatureEntitlementService;
import com.tradepilot.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import java.util.Arrays;

/** Private-beta bootstrap only; the configured address remains outside source control. */
@Component
public class ManualBacktestPrivateEntitlementSeeder implements ApplicationRunner {
    private final UserRepository users; private final FeatureEntitlementService entitlements;
    @Value("${app.manual-backtest.beta-emails:${app.backtest.beta-emails:${app.pre-analysis.beta-emails:${app.admin-email:}}}}")
    private String betaEmails;
    public ManualBacktestPrivateEntitlementSeeder(UserRepository users, FeatureEntitlementService entitlements) { this.users=users; this.entitlements=entitlements; }
    @Override public void run(ApplicationArguments args) { if (!StringUtils.hasText(betaEmails)) return; Arrays.stream(betaEmails.split(",")).map(String::trim).filter(StringUtils::hasText).forEach(email -> users.findByEmail(email).ifPresent(user -> entitlements.setEntitlement(user.getId(), FeatureEntitlementKey.MANUAL_BACKTEST_BETA, true, "Configured private research access"))); }
}
