package com.tradepilot.billing;

import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.util.StringUtils;

@Configuration
public class StripeProductionConfigValidator {

    @Bean
    ApplicationRunner stripeProductionValidator(StripeProperties stripeProperties, Environment environment) {
        return args -> validate(stripeProperties, environment);
    }

    static void validate(StripeProperties stripeProperties, Environment environment) {
        if (!environment.acceptsProfiles(Profiles.of("prod", "production"))) {
            return;
        }
        requireLiveSecret(stripeProperties.getSecretKey(), "STRIPE_SECRET_KEY");
        requireLiveWebhookSecret(stripeProperties.getWebhookSecret());
        requireLivePrice(stripeProperties.getProPriceId(), "STRIPE_PRO_MONTHLY_PRICE_ID");
        requireLivePrice(stripeProperties.getProYearlyPriceId(), "STRIPE_PRO_YEARLY_PRICE_ID");
        validateCreditPackCatalog();
    }

    private static void requireLiveSecret(String value, String name) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalStateException(name + " is required in production.");
        }
        String trimmed = value.trim();
        if (trimmed.startsWith("sk_test_") || trimmed.contains("test")) {
            throw new IllegalStateException(name + " must be a live Stripe secret in production.");
        }
        if (!trimmed.startsWith("sk_live_")) {
            throw new IllegalStateException(name + " must use Stripe live mode in production.");
        }
    }

    private static void requireLiveWebhookSecret(String value) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalStateException("STRIPE_WEBHOOK_SECRET is required in production.");
        }
        String trimmed = value.trim();
        if (!trimmed.startsWith("whsec_") || trimmed.toLowerCase().contains("test")) {
            throw new IllegalStateException("STRIPE_WEBHOOK_SECRET must be a production Stripe webhook signing secret.");
        }
    }

    private static void requireLivePrice(String value, String name) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalStateException(name + " is required in production.");
        }
        String trimmed = value.trim();
        if (!trimmed.startsWith("price_") || trimmed.toLowerCase().contains("test")) {
            throw new IllegalStateException(name + " must be a production Stripe price ID.");
        }
    }

    private static void validateCreditPackCatalog() {
        for (AiCreditPack pack : AiCreditPack.values()) {
            if (pack.credits() <= 0 || pack.amountCents() <= 0 || !"usd".equals(pack.currency())) {
                throw new IllegalStateException("AI credit-pack catalog is invalid for production.");
            }
        }
    }
}
