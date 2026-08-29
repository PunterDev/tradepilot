package com.tradepilot.billing;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StripeProductionConfigValidatorTests {

    @Test
    void productionRejectsTestSecretKey() {
        StripeProperties properties = liveProperties();
        properties.setSecretKey("sk_test_redacted");

        assertThatThrownBy(() -> StripeProductionConfigValidator.validate(properties, productionEnvironment()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("STRIPE_SECRET_KEY must be a live Stripe secret");
    }

    @Test
    void productionRequiresWebhookSecret() {
        StripeProperties properties = liveProperties();
        properties.setWebhookSecret("");

        assertThatThrownBy(() -> StripeProductionConfigValidator.validate(properties, productionEnvironment()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("STRIPE_WEBHOOK_SECRET is required");
    }

    @Test
    void productionRequiresMonthlyAndYearlyPrices() {
        StripeProperties properties = liveProperties();
        properties.setProYearlyPriceId("");

        assertThatThrownBy(() -> StripeProductionConfigValidator.validate(properties, productionEnvironment()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("STRIPE_PRO_YEARLY_PRICE_ID is required");
    }

    @Test
    void productionRejectsObviousTestPriceId() {
        StripeProperties properties = liveProperties();
        properties.setProPriceId("price_test_redacted");

        assertThatThrownBy(() -> StripeProductionConfigValidator.validate(properties, productionEnvironment()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("STRIPE_PRO_MONTHLY_PRICE_ID must be a production Stripe price ID");
    }

    @Test
    void productionAcceptsLiveStripeConfigurationShape() {
        assertThatCode(() -> StripeProductionConfigValidator.validate(liveProperties(), productionEnvironment()))
                .doesNotThrowAnyException();
    }

    @Test
    void nonProductionAllowsTestConfiguration() {
        StripeProperties properties = new StripeProperties();
        properties.setSecretKey("sk_test_redacted");
        properties.setWebhookSecret("whsec_test_redacted");
        properties.setProPriceId("price_test_redacted");
        properties.setProYearlyPriceId("price_test_yearly_redacted");

        assertThatCode(() -> StripeProductionConfigValidator.validate(properties, new MockEnvironment()))
                .doesNotThrowAnyException();
    }

    private StripeProperties liveProperties() {
        StripeProperties properties = new StripeProperties();
        properties.setSecretKey("sk_live_redacted");
        properties.setWebhookSecret("whsec_redacted_live_shape");
        properties.setProPriceId("price_live_monthly_shape");
        properties.setProYearlyPriceId("price_live_yearly_shape");
        return properties;
    }

    private MockEnvironment productionEnvironment() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("production");
        return environment;
    }
}
