package com.tradepilot;

import com.tradepilot.billing.AiCreditPack;
import com.tradepilot.billing.AiCreditPurchaseRepository;
import com.tradepilot.billing.PlanType;
import com.tradepilot.billing.StripeApiClient;
import com.tradepilot.billing.SubscriptionStatus;
import com.tradepilot.entity.User;
import com.tradepilot.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("production")
@TestPropertySource(properties = {
        "app.stripe.webhook-secret=whsec_prod_live_mode",
        "app.stripe.secret-key=sk_live_redacted",
        "app.stripe.pro-price-id=price_live_monthly",
        "app.stripe.pro-yearly-price-id=price_live_yearly",
        "app.frontend-url=https://tradepilot.example",
        "app.cors.allowed-origins=https://tradepilot.example",
        "spring.datasource.url=jdbc:h2:mem:stripe_live_mode;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "JWT_SECRET=YWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWE="
})
class StripeWebhookLiveModeTests {
    @Autowired MockMvc mockMvc;
    @Autowired UserRepository userRepository;
    @Autowired AiCreditPurchaseRepository aiCreditPurchaseRepository;
    @MockBean StripeApiClient stripeApiClient;

    @Test
    void productionLivemodeFalseIsRejectedAndDoesNotChangePlan() throws Exception {
        User user = createUser("stripe-prod-test-mode-reject@example.com");
        String payload = withLivemode(checkoutCompletedPayload(user.getId()), false);

        postWebhook(payload).andExpect(status().isBadRequest());

        User updated = userRepository.findById(user.getId()).orElseThrow();
        assertThat(updated.getPlan()).isEqualTo(PlanType.FREE);
        assertThat(updated.getSubscriptionStatus()).isEqualTo(SubscriptionStatus.NONE);
    }

    @Test
    void productionLivemodeTrueIsAllowed() throws Exception {
        User user = createUser("stripe-prod-live-mode-allow@example.com");
        String payload = withLivemode(checkoutCompletedPayload(user.getId()), true);

        postWebhook(payload).andExpect(status().isOk());

        User updated = userRepository.findById(user.getId()).orElseThrow();
        assertThat(updated.getPlan()).isEqualTo(PlanType.PRO);
        assertThat(updated.getSubscriptionStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
    }

    @Test
    void signedTestModeCreditPackCannotGrantCreditsInProduction() throws Exception {
        User user = createUser("stripe-prod-credit-test-mode-reject@example.com");
        user.setPlan(PlanType.PRO);
        user.setSubscriptionStatus(SubscriptionStatus.ACTIVE);
        user.setStripeCustomerId("cus_prod_credit_test_mode");
        userRepository.save(user);
        String payload = withLivemode(creditPackCheckoutCompletedPayload(user.getId(), AiCreditPack.STARTER_25), false);

        postWebhook(payload).andExpect(status().isBadRequest());

        User updated = userRepository.findById(user.getId()).orElseThrow();
        assertThat(updated.getPurchasedAiCreditsBalance()).isZero();
        assertThat(aiCreditPurchaseRepository.findAll())
                .filteredOn(purchase -> purchase.getUserId().equals(user.getId()))
                .isEmpty();
    }

    private org.springframework.test.web.servlet.ResultActions postWebhook(String payload) throws Exception {
        return mockMvc.perform(post("/api/stripe/webhook")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Stripe-Signature", stripeSignature(payload))
                .content(payload));
    }

    private User createUser(String email) {
        return userRepository.save(User.builder()
                .name(email)
                .email(email)
                .passwordHash("test-password-hash")
                .emailVerified(true)
                .plan(PlanType.FREE)
                .subscriptionStatus(SubscriptionStatus.NONE)
                .build());
    }

    private String checkoutCompletedPayload(Long userId) {
        return """
                {
                  "id": "evt_prod_checkout_%s",
                  "type": "checkout.session.completed",
                  "created": 1000,
                  "data": {
                    "object": {
                      "id": "cs_prod_test_%s",
                      "object": "checkout.session",
                      "customer": "cus_prod_test_%s",
                      "subscription": "sub_prod_test_%s",
                      "metadata": {
                        "userId": "%s"
                      }
                    }
                  }
                }
                """.formatted(userId, userId, userId, userId, userId);
    }

    private String creditPackCheckoutCompletedPayload(Long userId, AiCreditPack pack) {
        return """
                {
                  "id": "evt_prod_credit_%s",
                  "type": "checkout.session.completed",
                  "created": 1000,
                  "data": {
                    "object": {
                      "id": "cs_prod_credit_%s",
                      "object": "checkout.session",
                      "mode": "payment",
                      "payment_status": "paid",
                      "customer": "cus_prod_credit_test_mode",
                      "payment_intent": "pi_prod_credit_%s",
                      "amount_total": %s,
                      "currency": "%s",
                      "metadata": {
                        "type": "AI_CREDIT_PACK",
                        "userId": "%s",
                        "packId": "%s"
                      }
                    }
                  }
                }
                """.formatted(userId, userId, userId, pack.amountCents(), pack.currency(), userId, pack.name());
    }

    private String withLivemode(String payload, boolean livemode) {
        return payload.replaceFirst("\\{", "{\n  \"livemode\": " + livemode + ",");
    }

    private String stripeSignature(String payload) throws Exception {
        long timestamp = Instant.now().getEpochSecond();
        String signedPayload = timestamp + "." + payload;
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec("whsec_prod_live_mode".getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] digest = mac.doFinal(signedPayload.getBytes(StandardCharsets.UTF_8));
        return "t=" + timestamp + ",v1=" + hex(digest);
    }

    private String hex(byte[] bytes) {
        StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) {
            builder.append(String.format("%02x", value));
        }
        return builder.toString();
    }
}
