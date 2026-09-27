package com.tradepilot.billing;

import com.stripe.param.checkout.SessionCreateParams;
import com.tradepilot.entity.User;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultStripeApiClientTests {
    private final DefaultStripeApiClient stripeApiClient = new DefaultStripeApiClient();

    @Test
    void subscriptionCheckoutEnablesPromotionCodesWithoutChangingTheCheckoutContract() {
        SessionCreateParams params = stripeApiClient.subscriptionCheckoutParams(user(), "cus_subscription", "price_yearly", "https://app.tradepilotcoach.com");
        assertThat(params.getMode()).isEqualTo(SessionCreateParams.Mode.SUBSCRIPTION);
        assertThat(params.getCustomer()).isEqualTo("cus_subscription");
        assertThat(params.getAllowPromotionCodes()).isTrue();
        assertThat(params.getSuccessUrl()).isEqualTo("https://app.tradepilotcoach.com/dashboard?billing=success");
        assertThat(params.getCancelUrl()).isEqualTo("https://app.tradepilotcoach.com/dashboard?billing=cancelled");
        assertThat(params.getMetadata()).containsEntry("userId", "42").containsEntry("userEmail", "checkout@example.com");
        assertThat(params.getLineItems()).singleElement().satisfies(item -> {
            assertThat(item.getPrice()).isEqualTo("price_yearly");
            assertThat(item.getQuantity()).isEqualTo(1L);
        });
    }

    @Test
    void creditPackCheckoutDoesNotEnablePromotionCodesAndKeepsServerControlledPriceData() {
        SessionCreateParams params = stripeApiClient.creditPackCheckoutParams(user(), "cus_credits", AiCreditPack.ELITE_200, "https://app.tradepilotcoach.com");
        assertThat(params.getMode()).isEqualTo(SessionCreateParams.Mode.PAYMENT);
        assertThat(params.getAllowPromotionCodes()).isNull();
        assertThat(params.getSuccessUrl()).isEqualTo("https://app.tradepilotcoach.com/billing?credits=success");
        assertThat(params.getCancelUrl()).isEqualTo("https://app.tradepilotcoach.com/billing?credits=cancelled");
        assertThat(params.getMetadata()).containsEntry("type", "AI_CREDIT_PACK").containsEntry("packId", "ELITE_200").containsEntry("credits", "200");
        assertThat(params.getLineItems()).singleElement().satisfies(item -> {
            assertThat(item.getQuantity()).isEqualTo(1L);
            assertThat(item.getPrice()).isNull();
            assertThat(item.getPriceData().getCurrency()).isEqualTo("usd");
            assertThat(item.getPriceData().getUnitAmount()).isEqualTo(4999L);
        });
    }

    private User user() {
        return User.builder()
                .id(42L)
                .name("Checkout User")
                .email("checkout@example.com")
                .build();
    }
}
