package com.tradepilot.backtest.service;

import com.tradepilot.entitlement.FeatureEntitlementKey;
import com.tradepilot.entitlement.FeatureEntitlementService;
import com.tradepilot.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Arrays;

/** Mirrors the existing private-beta entitlement bootstrap without embedding a founder identity. */
@Component
public class BacktestPrivateEntitlementSeeder implements ApplicationRunner {
    private final UserRepository userRepository;
    private final FeatureEntitlementService entitlementService;

    @Value("${app.backtest.beta-emails:${app.pre-analysis.beta-emails:${app.admin-email:}}}")
    private String betaEmails;

    @Override
    public void run(ApplicationArguments args) {
        if (!StringUtils.hasText(betaEmails)) return;
        Arrays.stream(betaEmails.split(",")).map(String::trim).filter(StringUtils::hasText).forEach(email ->
                userRepository.findByEmail(email).ifPresent(user -> entitlementService.setEntitlement(
                        user.getId(), FeatureEntitlementKey.PLAYBOOK_BACKTEST_BETA, true, "Configured private research access")));
    }

    public BacktestPrivateEntitlementSeeder(UserRepository userRepository, FeatureEntitlementService entitlementService) {
        this.userRepository = userRepository;
        this.entitlementService = entitlementService;
    }
}
