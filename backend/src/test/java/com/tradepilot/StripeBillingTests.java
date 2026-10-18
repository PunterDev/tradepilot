package com.tradepilot;

import com.tradepilot.billing.AiCreditPack;
import com.tradepilot.billing.AiCreditPurchase;
import com.tradepilot.billing.AiCreditPurchaseRepository;
import com.tradepilot.billing.AiCreditPurchaseStatus;
import com.tradepilot.billing.AiCreditLimitExceededException;
import com.tradepilot.billing.Feature;
import com.tradepilot.billing.PlanLimitExceededException;
import com.tradepilot.billing.PlanType;
import com.tradepilot.billing.ProcessedStripeEventRepository;
import com.tradepilot.billing.StripeApiClient;
import com.tradepilot.billing.StripeBillingService;
import com.tradepilot.billing.StripeCheckoutAttempt;
import com.tradepilot.billing.StripeCheckoutAttemptRepository;
import com.tradepilot.billing.StripeCheckoutAttemptStatus;
import com.tradepilot.billing.SubscriptionStatus;
import com.tradepilot.billing.UsageLimitService;
import com.tradepilot.billing.UsageType;
import com.stripe.exception.InvalidRequestException;
import com.stripe.model.Customer;
import com.stripe.model.Invoice;
import com.stripe.model.Price;
import com.stripe.model.Subscription;
import com.stripe.model.SubscriptionItem;
import com.stripe.model.SubscriptionItemCollection;
import com.stripe.model.checkout.Session;
import com.tradepilot.dto.admin.AdminPlanChangeRequest;
import com.tradepilot.dto.account.TradingAccountRequest;
import com.tradepilot.dto.account.TradingAccountResponse;
import com.tradepilot.entity.User;
import com.tradepilot.featureusage.FeatureKey;
import com.tradepilot.featureusage.FeatureUsageAction;
import com.tradepilot.featureusage.FeatureUsageEvent;
import com.tradepilot.featureusage.FeatureUsageEventType;
import com.tradepilot.featureusage.FeatureUsageRepository;
import com.tradepilot.featurefunnel.FeatureFunnelCalculationMode;
import com.tradepilot.featurefunnel.FeatureFunnelKey;
import com.tradepilot.featurefunnel.FeatureFunnelService;
import com.tradepilot.featurefunnel.dto.FeatureFunnelSummaryResponse;
import com.tradepilot.enums.PropFirmType;
import com.tradepilot.enums.TradingAccountStatus;
import com.tradepilot.repository.UserRepository;
import com.tradepilot.service.AdminUserService;
import com.tradepilot.service.TradingAccountService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "app.stripe.webhook-secret=whsec_test",
        "app.stripe.secret-key=sk_test_mock",
        "app.stripe.pro-price-id=price_test_pro",
        "app.stripe.pro-yearly-price-id=price_test_pro_yearly"
})
class StripeBillingTests {
    @Autowired MockMvc mockMvc;
    @Autowired UserRepository userRepository;
    @Autowired TradingAccountService tradingAccountService;
    @Autowired AdminUserService adminUserService;
    @Autowired StripeBillingService stripeBillingService;
    @Autowired ProcessedStripeEventRepository processedStripeEventRepository;
    @Autowired StripeCheckoutAttemptRepository stripeCheckoutAttemptRepository;
    @Autowired AiCreditPurchaseRepository aiCreditPurchaseRepository;
    @Autowired FeatureUsageRepository featureUsageRepository;
    @Autowired FeatureFunnelService featureFunnelService;
    @Autowired UsageLimitService usageLimitService;
    @MockBean StripeApiClient stripeApiClient;

    @Test
    void createCheckoutRequiresAuth() throws Exception {
        mockMvc.perform(post("/api/billing/create-checkout-session"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void webhookRejectsInvalidSignature() throws Exception {
        mockMvc.perform(post("/api/stripe/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Stripe-Signature", "t=1,v1=not-valid")
                        .content(checkoutCompletedPayload(999L)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void webhookPostIsPublicAndInvalidSignatureReturnsBadRequestNotForbidden() throws Exception {
        mockMvc.perform(post("/api/stripe/webhook")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void checkoutCompletedUpgradesUserToPro() throws Exception {
        User user = createUser("stripe-checkout@example.com");
        prepareValidSubscriptionCheckout(user, "price_test_pro");
        StripeCheckoutAttempt attempt = saveCheckoutAttempt(user, "cs_test_123", "monthly", LocalDateTime.now().plusMinutes(1));
        String payload = checkoutCompletedPayload(user.getId());

        mockMvc.perform(post("/api/stripe/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Stripe-Signature", stripeSignature(payload))
                        .content(payload))
                .andExpect(status().isOk());

        User updated = userRepository.findById(user.getId()).orElseThrow();
        assertThat(updated.getPlan()).isEqualTo(PlanType.PRO);
        assertThat(updated.getSubscriptionStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(updated.getStripeCustomerId()).isEqualTo("cus_test_123");
        assertThat(updated.getStripeSubscriptionId()).isEqualTo("sub_test_123");
        assertThat(processedStripeEventRepository.existsByEventId("evt_checkout_" + user.getId())).isTrue();
        assertThat(stripeCheckoutAttemptRepository.findById(attempt.getId()).orElseThrow().getStatus()).isEqualTo(StripeCheckoutAttemptStatus.COMPLETED);
        assertLifecycleEventCount(user, FeatureKey.PLAYBOOK, FeatureUsageAction.CHECKOUT_COMPLETED, 1);
        assertLifecycleEventCount(user, FeatureKey.PLAYBOOK, FeatureUsageAction.SUBSCRIPTION_ACTIVATED, 1);
    }

    @Test
    void checkoutCompletedWithYearlyProPriceUpgradesUserToPro() throws Exception {
        User user = createUser("stripe-checkout-yearly@example.com");
        prepareValidSubscriptionCheckout(user, "price_test_pro_yearly");

        postWebhook(checkoutCompletedPayload(user.getId()));

        User updated = userRepository.findById(user.getId()).orElseThrow();
        assertThat(updated.getPlan()).isEqualTo(PlanType.PRO);
        assertThat(updated.getSubscriptionStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(lifecycleEvents(user, FeatureUsageAction.SUBSCRIPTION_ACTIVATED)).singleElement()
                .extracting(FeatureUsageEvent::getMetadataJson).asString().contains("YEARLY");
    }

    @Test
    void authoritativeSubscriptionEventsCompleteTheExistingUpgradeFunnel() throws Exception {
        User user = createUser("stripe-funnel-upgrade@example.com");
        recordUpgradeFunnelPrelude(user);
        prepareValidSubscriptionCheckout(user, "price_test_pro");
        when(stripeApiClient.retrieveCustomer("cus_test_123")).thenReturn(customer("cus_test_123", false));
        when(stripeApiClient.createCheckoutSession(any(User.class), eq("cus_test_123"), eq("price_test_pro"), eq("http://localhost:5173")))
                .thenReturn(stripeCheckoutSession("cs_test_123", "cus_test_123", "subscription", "open", "https://checkout.test/upgrade", "sub_test_123"));
        stripeBillingService.createCheckoutSession(user);

        postWebhook(checkoutCompletedPayload(user.getId()));

        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "admin", "N/A", AuthorityUtils.createAuthorityList("ROLE_ADMIN")));
        try {
            FeatureFunnelSummaryResponse summary = featureFunnelService.detail(
                    FeatureFunnelKey.FREE_TO_PRO_UPGRADE, "LAST_30_DAYS", null, null, null, null,
                    false, true, FeatureFunnelCalculationMode.UNIQUE_USERS, null);
            assertThat(summary.completedUsers()).isEqualTo(1);
            assertThat(summary.steps()).allSatisfy(step -> assertThat(step.usersReached()).isEqualTo(1));
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    void authoritativeCreditPurchaseEventsCompleteTheExistingCreditFunnel() throws Exception {
        User user = createProStripeUser("stripe-funnel-credits@example.com", "cus_credit_funnel");
        usageLimitService.consumeCredits(user, Feature.DAILY_COACH, 100);
        assertThatThrownBy(() -> usageLimitService.enforceAiUsage(user, UsageType.DAILY_COACH))
                .isInstanceOf(AiCreditLimitExceededException.class);

        when(stripeApiClient.retrieveCustomer("cus_credit_funnel")).thenReturn(customer("cus_credit_funnel", false));
        when(stripeApiClient.createCreditPackCheckoutSession(user, "cus_credit_funnel", AiCreditPack.STARTER_25, "http://localhost:5173"))
                .thenReturn(session("cus_credit_funnel", "https://checkout.test/credit-funnel"));
        stripeBillingService.createCreditPackCheckoutSession(user, AiCreditPack.STARTER_25);
        postWebhook(creditPackCheckoutCompletedPayload(user.getId(), "evt_credit_funnel", "cs_credit_funnel", AiCreditPack.STARTER_25,
                "cus_credit_funnel", "payment", "paid", "usd", 999));
        usageLimitService.consumeCredits(userRepository.findById(user.getId()).orElseThrow(), Feature.DAILY_COACH, 2);

        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "admin", "N/A", AuthorityUtils.createAuthorityList("ROLE_ADMIN")));
        try {
            FeatureFunnelSummaryResponse summary = featureFunnelService.detail(
                    FeatureFunnelKey.AI_CREDIT_PURCHASE, "LAST_30_DAYS", null, null, null, null,
                    false, true, FeatureFunnelCalculationMode.UNIQUE_USERS, null);
            assertThat(summary.completedUsers()).isEqualTo(1);
            assertThat(summary.steps()).allSatisfy(step -> assertThat(step.usersReached()).isEqualTo(1));
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    void checkoutCompletedWithUnknownProPriceDoesNotGrantPro() throws Exception {
        User user = createUser("stripe-checkout-unknown-price@example.com");
        prepareValidSubscriptionCheckout(user, "price_unrelated");

        postWebhook(checkoutCompletedPayload(user.getId()));

        assertThat(userRepository.findById(user.getId()).orElseThrow().getPlan()).isEqualTo(PlanType.FREE);
    }

    @Test
    void checkoutCompletedWithWrongStripeCustomerDoesNotGrantPro() throws Exception {
        User user = createUser("stripe-checkout-wrong-customer@example.com");
        user.setStripeCustomerId("cus_test_123");
        userRepository.save(user);
        when(stripeApiClient.retrieveSubscription("sub_test_123"))
                .thenReturn(subscriptionWithPrice("sub_test_123", "cus_other_customer", "active", "price_test_pro"));

        postWebhook(checkoutCompletedPayload(user.getId()));

        assertThat(userRepository.findById(user.getId()).orElseThrow().getPlan()).isEqualTo(PlanType.FREE);
    }

    @Test
    void checkoutCompletedWithUnacceptableSubscriptionStatusDoesNotGrantPro() throws Exception {
        User user = createUser("stripe-checkout-incomplete@example.com");
        prepareValidSubscriptionCheckout(user, "price_test_pro", "incomplete");

        postWebhook(checkoutCompletedPayload(user.getId()));

        assertThat(userRepository.findById(user.getId()).orElseThrow().getPlan()).isEqualTo(PlanType.FREE);
    }

    @Test
    void testProfileAllowsLivemodeFalseWebhookEvents() throws Exception {
        User user = createUser("stripe-test-mode-allowed@example.com");
        prepareValidSubscriptionCheckout(user, "price_test_pro");
        String payload = withLivemode(checkoutCompletedPayload(user.getId()), false);

        postWebhook(payload);

        User updated = userRepository.findById(user.getId()).orElseThrow();
        assertThat(updated.getPlan()).isEqualTo(PlanType.PRO);
        assertThat(updated.getSubscriptionStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
    }

    @Test
    void proUserCanCreateCreditPackCheckoutSession() throws Exception {
        User user = createUser("stripe-credit-pack-pro@example.com");
        user.setPlan(PlanType.PRO);
        user.setSubscriptionStatus(SubscriptionStatus.ACTIVE);
        user.setStripeCustomerId("cus_credit_pack_pro");
        userRepository.save(user);
        when(stripeApiClient.retrieveCustomer("cus_credit_pack_pro")).thenReturn(customer("cus_credit_pack_pro", false));
        when(stripeApiClient.createCreditPackCheckoutSession(user, "cus_credit_pack_pro", AiCreditPack.STARTER_25, "http://localhost:5173"))
                .thenReturn(session("cus_credit_pack_pro", "https://checkout.test/credits"));

        String checkoutUrl = stripeBillingService.createCreditPackCheckoutSession(user, AiCreditPack.STARTER_25);

        assertThat(checkoutUrl).isEqualTo("https://checkout.test/credits");
        verify(stripeApiClient).createCreditPackCheckoutSession(user, "cus_credit_pack_pro", AiCreditPack.STARTER_25, "http://localhost:5173");
        assertLifecycleEventCount(user, FeatureKey.DAILY_COACH, FeatureUsageAction.BUY_CREDITS_CTA_CLICKED, 1);
        assertLifecycleEventCount(user, FeatureKey.DAILY_COACH, FeatureUsageAction.CREDIT_PACK_SELECTED, 1);
    }

    @Test
    void freeUserCannotCreateCreditPackCheckoutSession() {
        User user = createUser("stripe-credit-pack-free@example.com");

        assertThatThrownBy(() -> stripeBillingService.createCreditPackCheckoutSession(user, AiCreditPack.STARTER_25))
                .isInstanceOf(PlanLimitExceededException.class)
                .hasMessageContaining("Upgrade to Pro");
    }

    @Test
    void creditPackWebhookAddsPurchasedCreditsOnlyOnce() throws Exception {
        User user = createUser("stripe-credit-pack-webhook@example.com");
        user.setPlan(PlanType.PRO);
        user.setSubscriptionStatus(SubscriptionStatus.ACTIVE);
        user.setStripeCustomerId("cus_credit_pack_123");
        userRepository.save(user);
        String payload = creditPackCheckoutCompletedPayload(user.getId(), "evt_credit_pack_once", "cs_credit_pack_once");
        String signature = stripeSignature(payload);

        mockMvc.perform(post("/api/stripe/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Stripe-Signature", signature)
                        .content(payload))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/stripe/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Stripe-Signature", signature)
                        .content(payload))
                .andExpect(status().isOk());

        User updated = userRepository.findById(user.getId()).orElseThrow();
        assertThat(updated.getPurchasedAiCreditsBalance()).isEqualTo(25);
        assertThat(aiCreditPurchaseRepository.findAll())
                .filteredOn(purchase -> purchase.getUserId().equals(user.getId()))
                .hasSize(1);
        assertLifecycleEventCount(user, FeatureKey.DAILY_COACH, FeatureUsageAction.CHECKOUT_COMPLETED, 1);
    }

    @Test
    void duplicateWebhookEventIsIgnored() throws Exception {
        User user = createUser("stripe-duplicate@example.com");
        prepareValidSubscriptionCheckout(user, "price_test_pro");
        String payload = checkoutCompletedPayload(user.getId());
        String signature = stripeSignature(payload);

        mockMvc.perform(post("/api/stripe/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Stripe-Signature", signature)
                        .content(payload))
                .andExpect(status().isOk());

        User manuallyChanged = userRepository.findById(user.getId()).orElseThrow();
        manuallyChanged.setPlan(PlanType.FREE);
        manuallyChanged.setSubscriptionStatus(SubscriptionStatus.NONE);
        userRepository.save(manuallyChanged);

        mockMvc.perform(post("/api/stripe/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Stripe-Signature", signature)
                        .content(payload))
                .andExpect(status().isOk());

        User afterDuplicate = userRepository.findById(user.getId()).orElseThrow();
        assertThat(afterDuplicate.getPlan()).isEqualTo(PlanType.FREE);
        assertThat(afterDuplicate.getSubscriptionStatus()).isEqualTo(SubscriptionStatus.NONE);
    }

    @Test
    void staleSubscriptionDeletedAfterNewerUpdateIsIgnored() throws Exception {
        User user = createUser("stripe-stale-delete@example.com");
        user.setPlan(PlanType.PRO);
        user.setSubscriptionStatus(SubscriptionStatus.ACTIVE);
        user.setStripeCustomerId("cus_stale_delete_123");
        user.setStripeSubscriptionId("sub_stale_delete_123");
        userRepository.save(user);

        String newerUpdate = subscriptionUpdatedPayload(
                "evt_stale_delete_newer_update",
                2_000L,
                "cus_stale_delete_123",
                "sub_stale_delete_123",
                "active"
        );
        String olderDelete = subscriptionDeletedPayload(
                "evt_stale_delete_older_delete",
                1_000L,
                "cus_stale_delete_123",
                "sub_stale_delete_123"
        );

        postWebhook(newerUpdate);
        postWebhook(olderDelete);

        User updated = userRepository.findById(user.getId()).orElseThrow();
        assertThat(updated.getPlan()).isEqualTo(PlanType.PRO);
        assertThat(updated.getSubscriptionStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
    }

    @Test
    void staleSubscriptionUpdatedAfterNewerDeletedIsIgnored() throws Exception {
        User user = createUser("stripe-stale-update@example.com");
        user.setPlan(PlanType.PRO);
        user.setSubscriptionStatus(SubscriptionStatus.ACTIVE);
        user.setStripeCustomerId("cus_stale_update_123");
        user.setStripeSubscriptionId("sub_stale_update_123");
        userRepository.save(user);

        String newerDelete = subscriptionDeletedPayload(
                "evt_stale_update_newer_delete",
                2_000L,
                "cus_stale_update_123",
                "sub_stale_update_123"
        );
        String olderUpdate = subscriptionUpdatedPayload(
                "evt_stale_update_older_update",
                1_000L,
                "cus_stale_update_123",
                "sub_stale_update_123",
                "active"
        );

        postWebhook(newerDelete);
        postWebhook(olderUpdate);

        User updated = userRepository.findById(user.getId()).orElseThrow();
        assertThat(updated.getPlan()).isEqualTo(PlanType.FREE);
        assertThat(updated.getSubscriptionStatus()).isEqualTo(SubscriptionStatus.CANCELED);
    }

    @Test
    void sameSecondActiveAndDeletedEventsUseCurrentStripeSubscriptionState() throws Exception {
        User user = createProStripeUser("stripe-same-second-active-delete@example.com", "cus_same_second_active");
        user.setStripeSubscriptionId("sub_same_second_active");
        userRepository.save(user);
        when(stripeApiClient.retrieveSubscription("sub_same_second_active"))
                .thenReturn(subscription("sub_same_second_active", "cus_same_second_active", "active"));

        postWebhook(subscriptionUpdatedPayload("evt_same_second_active_update", 1_000L, "cus_same_second_active", "sub_same_second_active", "active"));
        postWebhook(subscriptionDeletedPayload("evt_same_second_active_delete", 1_000L, "cus_same_second_active", "sub_same_second_active"));

        User updated = userRepository.findById(user.getId()).orElseThrow();
        assertThat(updated.getPlan()).isEqualTo(PlanType.PRO);
        assertThat(updated.getSubscriptionStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
    }

    @Test
    void sameSecondSubscriptionUpdatedAndDeletedUseCanceledCurrentState() throws Exception {
        User user = createProStripeUser("stripe-same-second-canceled@example.com", "cus_same_second_canceled");
        user.setStripeSubscriptionId("sub_same_second_canceled");
        userRepository.save(user);
        when(stripeApiClient.retrieveSubscription("sub_same_second_canceled"))
                .thenReturn(subscription("sub_same_second_canceled", "cus_same_second_canceled", "canceled"));

        postWebhook(subscriptionUpdatedPayload("evt_same_second_canceled_update", 1_000L, "cus_same_second_canceled", "sub_same_second_canceled", "active"));
        postWebhook(subscriptionDeletedPayload("evt_same_second_canceled_delete", 1_000L, "cus_same_second_canceled", "sub_same_second_canceled"));

        User updated = userRepository.findById(user.getId()).orElseThrow();
        assertThat(updated.getPlan()).isEqualTo(PlanType.FREE);
        assertThat(updated.getSubscriptionStatus()).isEqualTo(SubscriptionStatus.CANCELED);
    }

    @Test
    void sameSecondPastDueAndActiveEventsUseCurrentActiveSubscriptionState() throws Exception {
        User user = createProStripeUser("stripe-same-second-pastdue-active@example.com", "cus_same_second_recovered");
        user.setStripeSubscriptionId("sub_same_second_recovered");
        userRepository.save(user);
        when(stripeApiClient.retrieveSubscription("sub_same_second_recovered"))
                .thenReturn(subscription("sub_same_second_recovered", "cus_same_second_recovered", "active"));

        postWebhook(invoicePaymentFailedPayload("evt_same_second_failed", 1_000L, "cus_same_second_recovered", "sub_same_second_recovered"));
        postWebhook(invoicePaymentSucceededPayload("evt_same_second_paid", 1_000L, "cus_same_second_recovered", "sub_same_second_recovered"));

        User updated = userRepository.findById(user.getId()).orElseThrow();
        assertThat(updated.getPlan()).isEqualTo(PlanType.PRO);
        assertThat(updated.getSubscriptionStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
    }

    @Test
    void sameSecondDifferentEventIdsDoNotLetDeletedPayloadOverrideActiveStripeState() throws Exception {
        User user = createProStripeUser("stripe-same-second-different-events@example.com", "cus_same_second_different");
        user.setStripeSubscriptionId("sub_same_second_different");
        userRepository.save(user);
        when(stripeApiClient.retrieveSubscription("sub_same_second_different"))
                .thenReturn(subscription("sub_same_second_different", "cus_same_second_different", "active"));

        postWebhook(subscriptionDeletedPayload("evt_same_second_different_delete", 1_000L, "cus_same_second_different", "sub_same_second_different"));
        postWebhook(subscriptionUpdatedPayload("evt_same_second_different_update", 1_000L, "cus_same_second_different", "sub_same_second_different", "past_due"));

        User updated = userRepository.findById(user.getId()).orElseThrow();
        assertThat(updated.getPlan()).isEqualTo(PlanType.PRO);
        assertThat(updated.getSubscriptionStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
    }

    @Test
    void staleDeletedEventDoesNotDowngradeWhenStripeSubscriptionIsActive() throws Exception {
        User user = createProStripeUser("stripe-source-active-delete@example.com", "cus_source_active_delete");
        user.setStripeSubscriptionId("sub_source_active_delete");
        userRepository.save(user);
        when(stripeApiClient.retrieveSubscription("sub_source_active_delete"))
                .thenReturn(subscription("sub_source_active_delete", "cus_source_active_delete", "active"));

        postWebhook(subscriptionDeletedPayload("evt_source_active_delete", 1_000L, "cus_source_active_delete", "sub_source_active_delete"));

        User updated = userRepository.findById(user.getId()).orElseThrow();
        assertThat(updated.getPlan()).isEqualTo(PlanType.PRO);
        assertThat(updated.getSubscriptionStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
    }

    @Test
    void staleUpdatedEventDowngradesWhenStripeSubscriptionIsCanceled() throws Exception {
        User user = createProStripeUser("stripe-source-canceled-update@example.com", "cus_source_canceled_update");
        user.setStripeSubscriptionId("sub_source_canceled_update");
        userRepository.save(user);
        when(stripeApiClient.retrieveSubscription("sub_source_canceled_update"))
                .thenReturn(subscription("sub_source_canceled_update", "cus_source_canceled_update", "canceled"));

        postWebhook(subscriptionUpdatedPayload("evt_source_canceled_update", 1_000L, "cus_source_canceled_update", "sub_source_canceled_update", "active"));

        User updated = userRepository.findById(user.getId()).orElseThrow();
        assertThat(updated.getPlan()).isEqualTo(PlanType.FREE);
        assertThat(updated.getSubscriptionStatus()).isEqualTo(SubscriptionStatus.CANCELED);
    }

    @Test
    void sameSecondInvoicePaidAndPaymentFailedUseCurrentActiveSubscriptionState() throws Exception {
        User user = createProStripeUser("stripe-same-second-paid-failed@example.com", "cus_same_second_invoice");
        user.setSubscriptionStatus(SubscriptionStatus.PAST_DUE);
        user.setStripeSubscriptionId("sub_same_second_invoice");
        userRepository.save(user);
        when(stripeApiClient.retrieveSubscription("sub_same_second_invoice"))
                .thenReturn(subscription("sub_same_second_invoice", "cus_same_second_invoice", "active"));

        postWebhook(invoicePaymentSucceededPayload("evt_same_second_invoice_paid", 1_000L, "cus_same_second_invoice", "sub_same_second_invoice"));
        postWebhook(invoicePaymentFailedPayload("evt_same_second_invoice_failed", 1_000L, "cus_same_second_invoice", "sub_same_second_invoice"));

        User updated = userRepository.findById(user.getId()).orElseThrow();
        assertThat(updated.getPlan()).isEqualTo(PlanType.PRO);
        assertThat(updated.getSubscriptionStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
    }

    @Test
    void paymentFailedEventKeepsActiveWhenCurrentSubscriptionIsActive() throws Exception {
        User user = createProStripeUser("stripe-source-payment-active@example.com", "cus_source_payment_active");
        user.setStripeSubscriptionId("sub_source_payment_active");
        userRepository.save(user);
        when(stripeApiClient.retrieveSubscription("sub_source_payment_active"))
                .thenReturn(subscription("sub_source_payment_active", "cus_source_payment_active", "active"));

        postWebhook(invoicePaymentFailedPayload("evt_source_payment_active", 1_000L, "cus_source_payment_active", "sub_source_payment_active"));

        User updated = userRepository.findById(user.getId()).orElseThrow();
        assertThat(updated.getPlan()).isEqualTo(PlanType.PRO);
        assertThat(updated.getSubscriptionStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
    }

    @Test
    void invoicePaidAfterRecoveryUsesCurrentActiveSubscription() throws Exception {
        User user = createProStripeUser("stripe-source-recovered@example.com", "cus_source_recovered");
        user.setSubscriptionStatus(SubscriptionStatus.PAST_DUE);
        user.setStripeSubscriptionId("sub_source_recovered");
        userRepository.save(user);
        when(stripeApiClient.retrieveSubscription("sub_source_recovered"))
                .thenReturn(subscription("sub_source_recovered", "cus_source_recovered", "active"));

        postWebhook(invoicePaymentSucceededPayload("evt_source_recovered_paid", 1_000L, "cus_source_recovered", "sub_source_recovered"));

        User updated = userRepository.findById(user.getId()).orElseThrow();
        assertThat(updated.getPlan()).isEqualTo(PlanType.PRO);
        assertThat(updated.getSubscriptionStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
    }

    @Test
    void stripeSubscriptionLookupFailureDoesNotApplyDestructiveDowngrade() throws Exception {
        User user = createProStripeUser("stripe-source-lookup-failure@example.com", "cus_source_lookup_failure");
        user.setStripeSubscriptionId("sub_source_lookup_failure");
        userRepository.save(user);
        when(stripeApiClient.retrieveSubscription("sub_source_lookup_failure"))
                .thenThrow(noSuchCustomer("sub_source_lookup_failure"));

        String payload = subscriptionDeletedPayload("evt_source_lookup_failure", 1_000L, "cus_source_lookup_failure", "sub_source_lookup_failure");

        mockMvc.perform(post("/api/stripe/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Stripe-Signature", stripeSignature(payload))
                        .content(payload))
                .andExpect(status().is5xxServerError());

        User updated = userRepository.findById(user.getId()).orElseThrow();
        assertThat(updated.getPlan()).isEqualTo(PlanType.PRO);
        assertThat(updated.getSubscriptionStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
    }

    @Test
    void sourceOfTruthCustomerMismatchDoesNotMutateUser() throws Exception {
        User user = createProStripeUser("stripe-source-customer-mismatch@example.com", "cus_source_expected");
        user.setStripeSubscriptionId("sub_source_customer_mismatch");
        userRepository.save(user);
        when(stripeApiClient.retrieveSubscription("sub_source_customer_mismatch"))
                .thenReturn(subscription("sub_source_customer_mismatch", "cus_source_other", "canceled"));

        postWebhook(subscriptionDeletedPayload("evt_source_customer_mismatch", 1_000L, "cus_source_expected", "sub_source_customer_mismatch"));

        User updated = userRepository.findById(user.getId()).orElseThrow();
        assertThat(updated.getPlan()).isEqualTo(PlanType.PRO);
        assertThat(updated.getSubscriptionStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
    }

    @Test
    void sourceOfTruthSubscriptionMismatchDoesNotMutateUser() throws Exception {
        User user = createProStripeUser("stripe-source-subscription-mismatch@example.com", "cus_source_sub_mismatch");
        user.setStripeSubscriptionId("sub_source_expected");
        userRepository.save(user);
        when(stripeApiClient.retrieveSubscription("sub_source_expected"))
                .thenReturn(subscription("sub_source_other", "cus_source_sub_mismatch", "canceled"));

        postWebhook(subscriptionDeletedPayload("evt_source_sub_mismatch", 1_000L, "cus_source_sub_mismatch", "sub_source_expected"));

        User updated = userRepository.findById(user.getId()).orElseThrow();
        assertThat(updated.getPlan()).isEqualTo(PlanType.PRO);
        assertThat(updated.getSubscriptionStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
    }

    @Test
    void subscriptionDeletedDowngradesUserToFree() throws Exception {
        User user = createUser("stripe-deleted@example.com");
        user.setPlan(PlanType.PRO);
        user.setSubscriptionStatus(SubscriptionStatus.ACTIVE);
        user.setStripeCustomerId("cus_delete_123");
        user.setStripeSubscriptionId("sub_delete_123");
        userRepository.save(user);

        String payload = subscriptionDeletedPayload();

        mockMvc.perform(post("/api/stripe/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Stripe-Signature", stripeSignature(payload))
                        .content(payload))
                .andExpect(status().isOk());

        User updated = userRepository.findById(user.getId()).orElseThrow();
        assertThat(updated.getPlan()).isEqualTo(PlanType.FREE);
        assertThat(updated.getSubscriptionStatus()).isEqualTo(SubscriptionStatus.CANCELED);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void manualAdminUpgradeClearsStripeStateSoStaleStripeDeletionDoesNotDowngradeUser() throws Exception {
        User admin = createUser("stripe-admin-manual-upgrade@example.com");
        User target = createUser("stripe-manual-upgrade-target@example.com");
        target.setPlan(PlanType.FREE);
        target.setSubscriptionStatus(SubscriptionStatus.NONE);
        target.setStripeCustomerId("cus_stale_manual_123");
        target.setStripeSubscriptionId("sub_stale_manual_123");
        userRepository.save(target);

        adminUserService.upgradeToPro(
                admin,
                target.getId(),
                new AdminPlanChangeRequest("Manual comp for production readiness test", dateTime(1_782_864_000L))
        );

        User manuallyUpgraded = userRepository.findById(target.getId()).orElseThrow();
        assertThat(manuallyUpgraded.getPlan()).isEqualTo(PlanType.PRO);
        assertThat(manuallyUpgraded.getSubscriptionStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(manuallyUpgraded.getStripeCustomerId()).isNull();
        assertThat(manuallyUpgraded.getStripeSubscriptionId()).isNull();

        String payload = subscriptionDeletedPayload(
                "evt_deleted_stale_manual",
                "cus_stale_manual_123",
                "sub_stale_manual_123"
        );
        mockMvc.perform(post("/api/stripe/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Stripe-Signature", stripeSignature(payload))
                        .content(payload))
                .andExpect(status().isOk());

        User afterStaleWebhook = userRepository.findById(target.getId()).orElseThrow();
        assertThat(afterStaleWebhook.getPlan()).isEqualTo(PlanType.PRO);
        assertThat(afterStaleWebhook.getSubscriptionStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
    }

    @Test
    void subscriptionUpdatedPersistsItemCurrentPeriod() throws Exception {
        User user = createUser("stripe-subscription-updated@example.com");
        user.setPlan(PlanType.FREE);
        user.setSubscriptionStatus(SubscriptionStatus.NONE);
        user.setStripeCustomerId("cus_update_123");
        user.setStripeSubscriptionId("sub_update_123");
        userRepository.save(user);

        String payload = subscriptionUpdatedPayload();

        mockMvc.perform(post("/api/stripe/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Stripe-Signature", stripeSignature(payload))
                        .content(payload))
                .andExpect(status().isOk());

        User updated = userRepository.findById(user.getId()).orElseThrow();
        assertThat(updated.getPlan()).isEqualTo(PlanType.PRO);
        assertThat(updated.getSubscriptionStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(updated.getCurrentPeriodStart()).isEqualTo(dateTime(1_751_328_000L));
        assertThat(updated.getCurrentPeriodEnd()).isEqualTo(dateTime(1_754_006_400L));
        assertThat(updated.getStripeCancelAtPeriodEnd()).isFalse();
        assertThat(updated.getStripeCancelAt()).isNull();
        assertThat(updated.getStripeCanceledAt()).isNull();
    }

    @Test
    void subscriptionUpdatedPersistsJulyFirstPeriodEnd() throws Exception {
        User user = createUser("stripe-subscription-july-end@example.com");
        user.setPlan(PlanType.FREE);
        user.setSubscriptionStatus(SubscriptionStatus.NONE);
        user.setStripeCustomerId("cus_july_123");
        user.setStripeSubscriptionId("sub_july_123");
        userRepository.save(user);

        String payload = subscriptionUpdatedJulyFirstPayload();

        mockMvc.perform(post("/api/stripe/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Stripe-Signature", stripeSignature(payload))
                        .content(payload))
                .andExpect(status().isOk());

        User updated = userRepository.findById(user.getId()).orElseThrow();
        assertThat(updated.getPlan()).isEqualTo(PlanType.PRO);
        assertThat(updated.getSubscriptionStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(updated.getCurrentPeriodStart()).isEqualTo(dateTime(1_780_272_000L));
        assertThat(updated.getCurrentPeriodEnd()).isEqualTo(dateTime(1_782_864_000L));
    }

    @Test
    void subscriptionUpdatedCancelAtPeriodEndKeepsUserProUntilPeriodEnds() throws Exception {
        User user = createUser("stripe-cancel-at-period-end@example.com");
        user.setPlan(PlanType.PRO);
        user.setSubscriptionStatus(SubscriptionStatus.ACTIVE);
        user.setStripeCustomerId("cus_cancel_period_123");
        user.setStripeSubscriptionId("sub_cancel_period_123");
        userRepository.save(user);

        String payload = subscriptionCancelAtPeriodEndPayload();

        mockMvc.perform(post("/api/stripe/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Stripe-Signature", stripeSignature(payload))
                        .content(payload))
                .andExpect(status().isOk());

        User updated = userRepository.findById(user.getId()).orElseThrow();
        assertThat(updated.getPlan()).isEqualTo(PlanType.PRO);
        assertThat(updated.getSubscriptionStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(updated.getCurrentPeriodStart()).isEqualTo(dateTime(1_751_328_000L));
        assertThat(updated.getCurrentPeriodEnd()).isEqualTo(dateTime(1_754_006_400L));
        assertThat(updated.getStripeCancelAtPeriodEnd()).isTrue();
        assertThat(updated.getStripeCancelAt()).isEqualTo(dateTime(1_754_006_400L));
        assertThat(updated.getStripeCanceledAt()).isEqualTo(dateTime(1_751_760_000L));
    }

    @Test
    void invoicePaymentPaidRenewsActiveSubscription() throws Exception {
        User user = createUser("stripe-invoice-payment-paid@example.com");
        user.setPlan(PlanType.FREE);
        user.setSubscriptionStatus(SubscriptionStatus.PAST_DUE);
        user.setStripeCustomerId("cus_invoice_paid_123");
        user.setStripeSubscriptionId("sub_invoice_paid_123");
        userRepository.save(user);

        String payload = invoicePaymentPaidPayload();

        mockMvc.perform(post("/api/stripe/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Stripe-Signature", stripeSignature(payload))
                        .content(payload))
                .andExpect(status().isOk());

        User updated = userRepository.findById(user.getId()).orElseThrow();
        assertThat(updated.getPlan()).isEqualTo(PlanType.PRO);
        assertThat(updated.getSubscriptionStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(updated.getStripeCustomerId()).isEqualTo("cus_invoice_paid_123");
        assertThat(updated.getStripeSubscriptionId()).isEqualTo("sub_invoice_paid_123");
        assertThat(updated.getCurrentPeriodStart()).isEqualTo(dateTime(1_740_873_600L));
        assertThat(updated.getCurrentPeriodEnd()).isEqualTo(dateTime(1_743_465_600L));
        assertThat(processedStripeEventRepository.existsByEventId("evt_invoice_payment_paid")).isTrue();
    }

    @Test
    void invoicePaymentFailedMarksPastDueButPreservesProEntitlement() throws Exception {
        User user = createUser("stripe-payment-failed@example.com");
        user.setPlan(PlanType.PRO);
        user.setSubscriptionStatus(SubscriptionStatus.ACTIVE);
        user.setStripeCustomerId("cus_payment_failed_123");
        user.setStripeSubscriptionId("sub_payment_failed_123");
        userRepository.save(user);

        postWebhook(invoicePaymentFailedPayload(
                "evt_payment_failed_preserve",
                1_000L,
                "cus_payment_failed_123",
                "sub_payment_failed_123"
        ));

        User updated = userRepository.findById(user.getId()).orElseThrow();
        assertThat(updated.getPlan()).isEqualTo(PlanType.PRO);
        assertThat(updated.getSubscriptionStatus()).isEqualTo(SubscriptionStatus.PAST_DUE);
    }

    @Test
    void stalePaymentFailedAfterNewerPaidRecoveryIsIgnored() throws Exception {
        User user = createUser("stripe-payment-failed-stale@example.com");
        user.setPlan(PlanType.PRO);
        user.setSubscriptionStatus(SubscriptionStatus.PAST_DUE);
        user.setStripeCustomerId("cus_payment_recovered_123");
        user.setStripeSubscriptionId("sub_payment_recovered_123");
        userRepository.save(user);

        postWebhook(invoicePaymentSucceededPayload(
                "evt_payment_recovered_newer",
                2_000L,
                "cus_payment_recovered_123",
                "sub_payment_recovered_123"
        ));
        postWebhook(invoicePaymentFailedPayload(
                "evt_payment_failed_older",
                1_000L,
                "cus_payment_recovered_123",
                "sub_payment_recovered_123"
        ));

        User updated = userRepository.findById(user.getId()).orElseThrow();
        assertThat(updated.getPlan()).isEqualTo(PlanType.PRO);
        assertThat(updated.getSubscriptionStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
    }

    @Test
    void paymentFailureThenEventualCancellationDowngradesToFree() throws Exception {
        User user = createUser("stripe-payment-cancelled@example.com");
        user.setPlan(PlanType.PRO);
        user.setSubscriptionStatus(SubscriptionStatus.ACTIVE);
        user.setStripeCustomerId("cus_payment_cancelled_123");
        user.setStripeSubscriptionId("sub_payment_cancelled_123");
        userRepository.save(user);

        postWebhook(invoicePaymentFailedPayload(
                "evt_payment_cancelled_failed",
                1_000L,
                "cus_payment_cancelled_123",
                "sub_payment_cancelled_123"
        ));
        postWebhook(subscriptionDeletedPayload(
                "evt_payment_cancelled_deleted",
                2_000L,
                "cus_payment_cancelled_123",
                "sub_payment_cancelled_123"
        ));

        User updated = userRepository.findById(user.getId()).orElseThrow();
        assertThat(updated.getPlan()).isEqualTo(PlanType.FREE);
        assertThat(updated.getSubscriptionStatus()).isEqualTo(SubscriptionStatus.CANCELED);
    }

    @Test
    void stalePaymentFailedAfterNewerDeletedIsIgnored() throws Exception {
        User user = createUser("stripe-payment-failed-after-delete@example.com");
        user.setPlan(PlanType.PRO);
        user.setSubscriptionStatus(SubscriptionStatus.ACTIVE);
        user.setStripeCustomerId("cus_failed_after_delete_123");
        user.setStripeSubscriptionId("sub_failed_after_delete_123");
        userRepository.save(user);

        postWebhook(subscriptionDeletedPayload(
                "evt_failed_after_delete_newer",
                2_000L,
                "cus_failed_after_delete_123",
                "sub_failed_after_delete_123"
        ));
        postWebhook(invoicePaymentFailedPayload(
                "evt_failed_after_delete_older",
                1_000L,
                "cus_failed_after_delete_123",
                "sub_failed_after_delete_123"
        ));

        User updated = userRepository.findById(user.getId()).orElseThrow();
        assertThat(updated.getPlan()).isEqualTo(PlanType.FREE);
        assertThat(updated.getSubscriptionStatus()).isEqualTo(SubscriptionStatus.CANCELED);
    }

    @Test
    void invoicePaymentPaidWithTextualInvoiceIdRetrievesInvoiceOutsideWebhookTransaction() throws Exception {
        User user = createUser("stripe-invoice-payment-paid-textual@example.com");
        user.setPlan(PlanType.FREE);
        user.setSubscriptionStatus(SubscriptionStatus.PAST_DUE);
        user.setStripeCustomerId("cus_invoice_paid_textual_123");
        user.setStripeSubscriptionId("sub_invoice_paid_textual_123");
        userRepository.save(user);

        when(stripeApiClient.retrieveInvoice("in_textual_123")).thenAnswer(invocation -> {
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
            return invoice(
                    "in_textual_123",
                    "cus_invoice_paid_textual_123",
                    "sub_invoice_paid_textual_123"
            );
        });
        when(stripeApiClient.retrieveSubscription("sub_invoice_paid_textual_123")).thenAnswer(invocation -> {
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
            return subscription("sub_invoice_paid_textual_123", 1_780_272_000L, 1_782_864_000L);
        });

        String payload = invoicePaymentPaidTextualInvoicePayload();

        mockMvc.perform(post("/api/stripe/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Stripe-Signature", stripeSignature(payload))
                        .content(payload))
                .andExpect(status().isOk());

        User updated = userRepository.findById(user.getId()).orElseThrow();
        assertThat(updated.getPlan()).isEqualTo(PlanType.PRO);
        assertThat(updated.getSubscriptionStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(updated.getStripeCustomerId()).isEqualTo("cus_invoice_paid_textual_123");
        assertThat(updated.getStripeSubscriptionId()).isEqualTo("sub_invoice_paid_textual_123");
        assertThat(updated.getCurrentPeriodStart()).isEqualTo(dateTime(1_780_272_000L));
        assertThat(updated.getCurrentPeriodEnd()).isEqualTo(dateTime(1_782_864_000L));
        assertThat(processedStripeEventRepository.existsByEventId("evt_invoice_payment_paid_textual")).isTrue();
    }

    @Test
    void invoicePaymentSucceededWithBadSamePeriodDoesNotOverwriteExistingPeriod() throws Exception {
        User user = createUser("stripe-invoice-bad-period@example.com");
        user.setPlan(PlanType.PRO);
        user.setSubscriptionStatus(SubscriptionStatus.ACTIVE);
        user.setStripeCustomerId("cus_invoice_bad_123");
        user.setStripeSubscriptionId("sub_invoice_bad_123");
        user.setCurrentPeriodStart(dateTime(1_780_272_000L));
        user.setCurrentPeriodEnd(dateTime(1_782_864_000L));
        userRepository.save(user);
        when(stripeApiClient.retrieveSubscription("sub_invoice_bad_123")).thenReturn(null);

        String payload = invoicePaymentSucceededBadSamePeriodPayload();

        mockMvc.perform(post("/api/stripe/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Stripe-Signature", stripeSignature(payload))
                        .content(payload))
                .andExpect(status().isOk());

        User updated = userRepository.findById(user.getId()).orElseThrow();
        assertThat(updated.getPlan()).isEqualTo(PlanType.PRO);
        assertThat(updated.getSubscriptionStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(updated.getCurrentPeriodStart()).isEqualTo(dateTime(1_780_272_000L));
        assertThat(updated.getCurrentPeriodEnd()).isEqualTo(dateTime(1_782_864_000L));
    }

    @Test
    void invoicePaymentSucceededRetrievesSubscriptionPeriod() throws Exception {
        User user = createUser("stripe-invoice-subscription-period@example.com");
        user.setPlan(PlanType.FREE);
        user.setSubscriptionStatus(SubscriptionStatus.PAST_DUE);
        user.setStripeCustomerId("cus_invoice_subscription_123");
        user.setStripeSubscriptionId("sub_invoice_subscription_123");
        userRepository.save(user);
        when(stripeApiClient.retrieveSubscription("sub_invoice_subscription_123"))
                .thenReturn(subscription("sub_invoice_subscription_123", 1_780_272_000L, 1_782_864_000L));

        String payload = invoicePaymentSucceededWithBadInvoicePeriodPayload();

        mockMvc.perform(post("/api/stripe/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Stripe-Signature", stripeSignature(payload))
                        .content(payload))
                .andExpect(status().isOk());

        User updated = userRepository.findById(user.getId()).orElseThrow();
        assertThat(updated.getPlan()).isEqualTo(PlanType.PRO);
        assertThat(updated.getSubscriptionStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(updated.getCurrentPeriodStart()).isEqualTo(dateTime(1_780_272_000L));
        assertThat(updated.getCurrentPeriodEnd()).isEqualTo(dateTime(1_782_864_000L));
    }

    @Test
    void webhookUpgradeLetsUserBypassFreeAccountLimit() throws Exception {
        User user = createUser("stripe-limit-bypass@example.com");
        prepareValidSubscriptionCheckout(user, "price_test_pro");
        String payload = checkoutCompletedPayload(user.getId());

        mockMvc.perform(post("/api/stripe/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Stripe-Signature", stripeSignature(payload))
                        .content(payload))
                .andExpect(status().isOk());

        User upgraded = userRepository.findById(user.getId()).orElseThrow();
        TradingAccountResponse first = createAccount(upgraded, "Stripe Pro One");
        TradingAccountResponse second = createAccount(upgraded, "Stripe Pro Two");

        assertThat(first.getId()).isNotNull();
        assertThat(second.getId()).isNotNull();
    }

    @Test
    void checkoutCreatesCustomerWhenUserHasNoStripeCustomerId() throws Exception {
        User user = createUser("stripe-missing-customer@example.com");
        when(stripeApiClient.createCustomer(any(User.class))).thenReturn(customer("cus_new_missing", false));
        when(stripeApiClient.createCheckoutSession(any(User.class), eq("cus_new_missing"), eq("price_test_pro"), eq("http://localhost:5173")))
                .thenReturn(session("cus_new_missing", "https://checkout.test/missing"));

        String checkoutUrl = stripeBillingService.createCheckoutSession(user);

        User updated = userRepository.findById(user.getId()).orElseThrow();
        assertThat(checkoutUrl).isEqualTo("https://checkout.test/missing");
        assertThat(updated.getStripeCustomerId()).isEqualTo("cus_new_missing");
        verify(stripeApiClient, never()).retrieveCustomer(anyString());
        StripeCheckoutAttempt attempt = stripeCheckoutAttemptRepository.findAll().stream()
                .filter(candidate -> candidate.getUserId().equals(user.getId()))
                .findFirst()
                .orElseThrow();
        assertThat(attempt.getStatus()).isEqualTo(StripeCheckoutAttemptStatus.PENDING);
        assertThat(attempt.getBillingCycle()).isEqualTo("monthly");
    }

    @Test
    void yearlyCheckoutUsesYearlyPriceId() throws Exception {
        User user = createUser("stripe-yearly-checkout@example.com");
        when(stripeApiClient.createCustomer(any(User.class))).thenReturn(customer("cus_yearly", false));
        when(stripeApiClient.createCheckoutSession(any(User.class), eq("cus_yearly"), eq("price_test_pro_yearly"), eq("http://localhost:5173")))
                .thenReturn(session("cus_yearly", "https://checkout.test/yearly"));

        String checkoutUrl = stripeBillingService.createCheckoutSession(user, "yearly");

        assertThat(checkoutUrl).isEqualTo("https://checkout.test/yearly");
        verify(stripeApiClient).createCheckoutSession(any(User.class), eq("cus_yearly"), eq("price_test_pro_yearly"), eq("http://localhost:5173"));
        verify(stripeApiClient, never()).createCheckoutSession(any(User.class), eq("cus_yearly"), eq("price_test_pro"), eq("http://localhost:5173"));
    }

    @Test
    void checkoutRecreatesDeletedStripeCustomerId() throws Exception {
        User user = createUser("stripe-deleted-customer@example.com");
        user.setStripeCustomerId("cus_deleted_old");
        userRepository.save(user);
        when(stripeApiClient.retrieveCustomer("cus_deleted_old")).thenReturn(customer("cus_deleted_old", true));
        when(stripeApiClient.createCustomer(any(User.class))).thenReturn(customer("cus_deleted_new", false));
        when(stripeApiClient.createCheckoutSession(any(User.class), eq("cus_deleted_new"), eq("price_test_pro"), eq("http://localhost:5173")))
                .thenReturn(session("cus_deleted_new", "https://checkout.test/deleted"));

        String checkoutUrl = stripeBillingService.createCheckoutSession(user);

        User updated = userRepository.findById(user.getId()).orElseThrow();
        assertThat(checkoutUrl).isEqualTo("https://checkout.test/deleted");
        assertThat(updated.getStripeCustomerId()).isEqualTo("cus_deleted_new");
    }

    @Test
    void checkoutRecreatesCustomerFromAnotherStripeEnvironment() throws Exception {
        User user = createUser("stripe-other-env@example.com");
        user.setStripeCustomerId("cus_other_env");
        userRepository.save(user);
        when(stripeApiClient.retrieveCustomer("cus_other_env")).thenThrow(noSuchCustomer("cus_other_env"));
        when(stripeApiClient.createCustomer(any(User.class))).thenReturn(customer("cus_other_env_recovered", false));
        when(stripeApiClient.createCheckoutSession(any(User.class), eq("cus_other_env_recovered"), eq("price_test_pro"), eq("http://localhost:5173")))
                .thenReturn(session("cus_other_env_recovered", "https://checkout.test/other-env"));

        String checkoutUrl = stripeBillingService.createCheckoutSession(user);

        User updated = userRepository.findById(user.getId()).orElseThrow();
        assertThat(checkoutUrl).isEqualTo("https://checkout.test/other-env");
        assertThat(updated.getStripeCustomerId()).isEqualTo("cus_other_env_recovered");
    }

    @Test
    void checkoutRetriesOnceWhenStripeRejectsStoredCustomer() throws Exception {
        User user = createUser("stripe-retry@example.com");
        user.setStripeCustomerId("cus_retry_old");
        userRepository.save(user);
        when(stripeApiClient.retrieveCustomer("cus_retry_old")).thenReturn(customer("cus_retry_old", false));
        when(stripeApiClient.createCustomer(any(User.class))).thenReturn(customer("cus_retry_new", false));
        when(stripeApiClient.createCheckoutSession(any(User.class), eq("cus_retry_old"), eq("price_test_pro"), eq("http://localhost:5173")))
                .thenThrow(noSuchCustomer("cus_retry_old"));
        when(stripeApiClient.createCheckoutSession(any(User.class), eq("cus_retry_new"), eq("price_test_pro"), eq("http://localhost:5173")))
                .thenReturn(session("cus_retry_new", "https://checkout.test/retry"));

        String checkoutUrl = stripeBillingService.createCheckoutSession(user);

        User updated = userRepository.findById(user.getId()).orElseThrow();
        assertThat(checkoutUrl).isEqualTo("https://checkout.test/retry");
        assertThat(updated.getStripeCustomerId()).isEqualTo("cus_retry_new");
        verify(stripeApiClient).createCheckoutSession(any(User.class), eq("cus_retry_old"), eq("price_test_pro"), eq("http://localhost:5173"));
        verify(stripeApiClient).createCheckoutSession(any(User.class), eq("cus_retry_new"), eq("price_test_pro"), eq("http://localhost:5173"));
    }

    @Test
    void existingActiveSubscriptionBlocksNewCheckout() throws Exception {
        User user = createProStripeUser("stripe-checkout-active-block@example.com", "cus_checkout_active");
        user.setStripeSubscriptionId("sub_checkout_active");
        userRepository.save(user);

        assertThatThrownBy(() -> stripeBillingService.createCheckoutSession(user))
                .isInstanceOf(PlanLimitExceededException.class)
                .hasMessageContaining("already have an active Pro subscription");
        verify(stripeApiClient, never()).createCheckoutSession(any(), anyString(), anyString(), anyString());
    }

    @Test
    void trialingSubscriptionBlocksNewCheckout() throws Exception {
        User user = createUser("stripe-checkout-trialing-block@example.com");
        user.setPlan(PlanType.PRO);
        user.setSubscriptionStatus(SubscriptionStatus.TRIALING);
        user.setStripeCustomerId("cus_checkout_trialing");
        user.setStripeSubscriptionId("sub_checkout_trialing");
        userRepository.save(user);

        assertThatThrownBy(() -> stripeBillingService.createCheckoutSession(user))
                .isInstanceOf(PlanLimitExceededException.class);
        verify(stripeApiClient, never()).createCheckoutSession(any(), anyString(), anyString(), anyString());
    }

    @Test
    void pastDueSubscriptionBlocksNewCheckout() throws Exception {
        User user = createUser("stripe-checkout-past-due-block@example.com");
        user.setPlan(PlanType.PRO);
        user.setSubscriptionStatus(SubscriptionStatus.PAST_DUE);
        user.setStripeCustomerId("cus_checkout_past_due");
        user.setStripeSubscriptionId("sub_checkout_past_due");
        userRepository.save(user);

        assertThatThrownBy(() -> stripeBillingService.createCheckoutSession(user))
                .isInstanceOf(PlanLimitExceededException.class);
        verify(stripeApiClient, never()).createCheckoutSession(any(), anyString(), anyString(), anyString());
    }

    @Test
    void canceledSubscriptionAllowsNewCheckout() throws Exception {
        User user = createUser("stripe-checkout-canceled-allowed@example.com");
        user.setPlan(PlanType.FREE);
        user.setSubscriptionStatus(SubscriptionStatus.CANCELED);
        userRepository.save(user);
        when(stripeApiClient.createCustomer(any(User.class))).thenReturn(customer("cus_checkout_canceled_new", false));
        when(stripeApiClient.createCheckoutSession(any(User.class), eq("cus_checkout_canceled_new"), eq("price_test_pro"), eq("http://localhost:5173")))
                .thenReturn(session("cus_checkout_canceled_new", "https://checkout.test/canceled-allowed"));

        String checkoutUrl = stripeBillingService.createCheckoutSession(user);

        assertThat(checkoutUrl).isEqualTo("https://checkout.test/canceled-allowed");
        verify(stripeApiClient).createCheckoutSession(any(User.class), eq("cus_checkout_canceled_new"), eq("price_test_pro"), eq("http://localhost:5173"));
    }

    @Test
    void staleLocalStateButStripeShowsActiveBlocksNewCheckout() throws Exception {
        User user = createUser("stripe-checkout-stale-local-active@example.com");
        user.setPlan(PlanType.FREE);
        user.setSubscriptionStatus(SubscriptionStatus.CANCELED);
        user.setStripeCustomerId("cus_checkout_stale_active");
        user.setStripeSubscriptionId("sub_checkout_stale_active");
        userRepository.save(user);
        when(stripeApiClient.retrieveSubscription("sub_checkout_stale_active"))
                .thenReturn(subscription("sub_checkout_stale_active", "cus_checkout_stale_active", "active"));

        assertThatThrownBy(() -> stripeBillingService.createCheckoutSession(user))
                .isInstanceOf(PlanLimitExceededException.class);

        User updated = userRepository.findById(user.getId()).orElseThrow();
        assertThat(updated.getPlan()).isEqualTo(PlanType.PRO);
        assertThat(updated.getSubscriptionStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        verify(stripeApiClient, never()).createCheckoutSession(any(), anyString(), anyString(), anyString());
    }

    @Test
    void openPendingSubscriptionCheckoutIsReusedInsteadOfReturningForbidden() throws Exception {
        User user = createUser("stripe-checkout-double-click@example.com");
        when(stripeApiClient.createCustomer(any(User.class))).thenReturn(customer("cus_checkout_double_click", false));
        when(stripeApiClient.createCheckoutSession(any(User.class), eq("cus_checkout_double_click"), eq("price_test_pro"), eq("http://localhost:5173")))
                .thenReturn(stripeCheckoutSession("cs_checkout_double_click", "cus_checkout_double_click", "subscription", "open", "https://checkout.test/double-click", null));

        assertThat(stripeBillingService.createCheckoutSession(user)).isEqualTo("https://checkout.test/double-click");
        when(stripeApiClient.retrieveCheckoutSession("cs_checkout_double_click"))
                .thenReturn(stripeCheckoutSession("cs_checkout_double_click", "cus_checkout_double_click", "subscription", "open", "https://checkout.test/double-click", null));

        assertThat(stripeBillingService.createCheckoutSession(user)).isEqualTo("https://checkout.test/double-click");

        verify(stripeApiClient, times(1)).createCheckoutSession(any(User.class), anyString(), eq("price_test_pro"), eq("http://localhost:5173"));
    }

    @Test
    void expiredPendingSubscriptionCheckoutCreatesAReplacementSession() throws Exception {
        User user = createUser("stripe-checkout-expired@example.com");
        StripeCheckoutAttempt attempt = saveCheckoutAttempt(user, "cs_expired", "monthly", LocalDateTime.now().plusMinutes(1));
        when(stripeApiClient.retrieveCheckoutSession("cs_expired"))
                .thenReturn(stripeCheckoutSession("cs_expired", "cus_expired", "subscription", "expired", null, null));
        when(stripeApiClient.createCustomer(any(User.class))).thenReturn(customer("cus_replacement", false));
        when(stripeApiClient.createCheckoutSession(any(User.class), eq("cus_replacement"), eq("price_test_pro"), eq("http://localhost:5173")))
                .thenReturn(stripeCheckoutSession("cs_replacement", "cus_replacement", "subscription", "open", "https://checkout.test/replacement", null));

        assertThat(stripeBillingService.createCheckoutSession(user)).isEqualTo("https://checkout.test/replacement");

        assertThat(stripeCheckoutAttemptRepository.findById(attempt.getId()).orElseThrow().getStripeCheckoutSessionId()).isEqualTo("cs_replacement");
        verify(stripeApiClient).createCheckoutSession(any(User.class), eq("cus_replacement"), eq("price_test_pro"), eq("http://localhost:5173"));
    }

    @Test
    void missingPendingSubscriptionCheckoutCreatesAReplacementSession() throws Exception {
        User user = createUser("stripe-checkout-missing@example.com");
        saveCheckoutAttempt(user, "cs_missing", "monthly", LocalDateTime.now().plusMinutes(1));
        when(stripeApiClient.retrieveCheckoutSession("cs_missing")).thenThrow(noSuchCheckoutSession("cs_missing"));
        when(stripeApiClient.createCustomer(any(User.class))).thenReturn(customer("cus_missing_replacement", false));
        when(stripeApiClient.createCheckoutSession(any(User.class), eq("cus_missing_replacement"), eq("price_test_pro"), eq("http://localhost:5173")))
                .thenReturn(stripeCheckoutSession("cs_missing_replacement", "cus_missing_replacement", "subscription", "open", "https://checkout.test/missing-replacement", null));

        assertThat(stripeBillingService.createCheckoutSession(user)).isEqualTo("https://checkout.test/missing-replacement");

        verify(stripeApiClient).createCheckoutSession(any(User.class), eq("cus_missing_replacement"), eq("price_test_pro"), eq("http://localhost:5173"));
    }

    @Test
    void completedPendingSubscriptionCheckoutReconcilesAndPreventsDuplicateCheckout() throws Exception {
        User user = createUser("stripe-checkout-complete@example.com");
        user.setStripeCustomerId("cus_complete");
        userRepository.save(user);
        StripeCheckoutAttempt attempt = saveCheckoutAttempt(user, "cs_complete", "monthly", LocalDateTime.now().plusMinutes(1));
        when(stripeApiClient.retrieveCheckoutSession("cs_complete"))
                .thenReturn(stripeCheckoutSession("cs_complete", "cus_complete", "subscription", "complete", null, "sub_complete"));
        when(stripeApiClient.retrieveSubscription("sub_complete"))
                .thenReturn(subscription("sub_complete", "cus_complete", "active"));

        assertThatThrownBy(() -> stripeBillingService.createCheckoutSession(user))
                .isInstanceOf(PlanLimitExceededException.class)
                .hasMessageContaining("checkout is complete");

        assertThat(userRepository.findById(user.getId()).orElseThrow().getPlan()).isEqualTo(PlanType.PRO);
        assertThat(stripeCheckoutAttemptRepository.findById(attempt.getId()).orElseThrow().getStatus()).isEqualTo(StripeCheckoutAttemptStatus.COMPLETED);
        verify(stripeApiClient, never()).createCheckoutSession(any(), anyString(), anyString(), anyString());
    }

    @Test
    void stalePendingAttemptWithoutSessionIdCanCreateACheckoutSession() throws Exception {
        User user = createUser("stripe-checkout-no-session@example.com");
        StripeCheckoutAttempt attempt = StripeCheckoutAttempt.pending(user.getId(), LocalDateTime.now().minusMinutes(1));
        attempt.setBillingCycle("monthly");
        stripeCheckoutAttemptRepository.saveAndFlush(attempt);
        when(stripeApiClient.createCustomer(any(User.class))).thenReturn(customer("cus_no_session", false));
        when(stripeApiClient.createCheckoutSession(any(User.class), eq("cus_no_session"), eq("price_test_pro"), eq("http://localhost:5173")))
                .thenReturn(stripeCheckoutSession("cs_no_session", "cus_no_session", "subscription", "open", "https://checkout.test/no-session", null));

        assertThat(stripeBillingService.createCheckoutSession(user)).isEqualTo("https://checkout.test/no-session");

        verify(stripeApiClient).createCheckoutSession(any(User.class), eq("cus_no_session"), eq("price_test_pro"), eq("http://localhost:5173"));
    }

    @Test
    void recentPendingAttemptWithoutSessionIdKeepsOnlyTheShortCreationLock() throws Exception {
        User user = createUser("stripe-checkout-creation-lock@example.com");
        StripeCheckoutAttempt attempt = StripeCheckoutAttempt.pending(user.getId(), LocalDateTime.now().plusMinutes(1));
        attempt.setBillingCycle("monthly");
        stripeCheckoutAttemptRepository.saveAndFlush(attempt);

        assertThatThrownBy(() -> stripeBillingService.createCheckoutSession(user))
                .isInstanceOf(PlanLimitExceededException.class)
                .hasMessageContaining("being prepared");

        verify(stripeApiClient, never()).createCheckoutSession(any(), anyString(), anyString(), anyString());
    }

    @Test
    void monthlyPendingCheckoutIsExpiredBeforeCreatingYearlyCheckout() throws Exception {
        assertCrossCycleCheckoutReplacement("monthly", "yearly", "price_test_pro_yearly");
    }

    @Test
    void yearlyPendingCheckoutIsExpiredBeforeCreatingMonthlyCheckout() throws Exception {
        assertCrossCycleCheckoutReplacement("yearly", "monthly", "price_test_pro");
    }

    @Test
    void duplicateCheckoutAttemptIsRejectedByDatabaseUniqueness() {
        User user = createUser("stripe-checkout-concurrent-unique@example.com");
        stripeCheckoutAttemptRepository.saveAndFlush(StripeCheckoutAttempt.pending(user.getId(), LocalDateTime.now().plusMinutes(30)));

        assertThatThrownBy(() -> stripeCheckoutAttemptRepository.saveAndFlush(
                StripeCheckoutAttempt.pending(user.getId(), LocalDateTime.now().plusMinutes(30))
        )).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void creditPackWrongAmountGrantsZeroCredits() throws Exception {
        assertCreditPackRejected(creditPackCheckoutCompletedPayload(
                0L, "evt_credit_wrong_amount", "cs_credit_wrong_amount", AiCreditPack.ELITE_200, "cus_credit_wrong_amount", "payment", "paid", "usd", 5999
        ), "stripe-credit-wrong-amount@example.com", "cus_credit_wrong_amount");
    }

    @Test
    void creditPackWrongCurrencyGrantsZeroCredits() throws Exception {
        assertCreditPackRejected(creditPackCheckoutCompletedPayload(
                0L, "evt_credit_wrong_currency", "cs_credit_wrong_currency", AiCreditPack.STARTER_25, "cus_credit_wrong_currency", "payment", "paid", "eur", 999
        ), "stripe-credit-wrong-currency@example.com", "cus_credit_wrong_currency");
    }

    @Test
    void creditPackUnpaidSessionGrantsZeroCredits() throws Exception {
        assertCreditPackRejected(creditPackCheckoutCompletedPayload(
                0L, "evt_credit_unpaid", "cs_credit_unpaid", AiCreditPack.STARTER_25, "cus_credit_unpaid", "payment", "unpaid", "usd", 999
        ), "stripe-credit-unpaid@example.com", "cus_credit_unpaid");
    }

    @Test
    void creditPackNoPaymentRequiredSessionGrantsZeroCredits() throws Exception {
        assertCreditPackRejected(creditPackCheckoutCompletedPayload(
                0L, "evt_credit_no_payment_required", "cs_credit_no_payment_required", AiCreditPack.STARTER_25, "cus_credit_no_payment_required", "payment", "no_payment_required", "usd", 999
        ), "stripe-credit-no-payment-required@example.com", "cus_credit_no_payment_required");
    }

    @Test
    void creditPackWrongCheckoutModeGrantsZeroCredits() throws Exception {
        assertCreditPackRejected(creditPackCheckoutCompletedPayload(
                0L, "evt_credit_wrong_mode", "cs_credit_wrong_mode", AiCreditPack.STARTER_25, "cus_credit_wrong_mode", "subscription", "paid", "usd", 999
        ), "stripe-credit-wrong-mode@example.com", "cus_credit_wrong_mode");
    }

    @Test
    void creditPackMissingCustomerGrantsZeroCredits() throws Exception {
        assertCreditPackRejected(creditPackCheckoutCompletedPayload(
                0L, "evt_credit_missing_customer", "cs_credit_missing_customer", AiCreditPack.STARTER_25, "", "payment", "paid", "usd", 999
        ), "stripe-credit-missing-customer@example.com", "cus_credit_missing_customer");
    }

    @Test
    void creditPackWrongCustomerGrantsZeroCredits() throws Exception {
        User user = createProStripeUser("stripe-credit-wrong-customer@example.com", "cus_expected_customer");
        String payload = creditPackCheckoutCompletedPayload(
                user.getId(), "evt_credit_wrong_customer", "cs_credit_wrong_customer", AiCreditPack.STARTER_25, "cus_other_customer", "payment", "paid", "usd", 999
        );

        postWebhook(payload);

        User updated = userRepository.findById(user.getId()).orElseThrow();
        assertThat(updated.getPurchasedAiCreditsBalance()).isZero();
        assertThat(aiCreditPurchaseRepository.findByStripeCheckoutSessionId("cs_credit_wrong_customer")).isEmpty();
    }

    @Test
    void creditPackUnknownPackIdGrantsZeroCredits() throws Exception {
        User user = createProStripeUser("stripe-credit-unknown-pack@example.com", "cus_credit_unknown_pack");
        String payload = creditPackCheckoutCompletedPayload(
                user.getId(), "evt_credit_unknown_pack", "cs_credit_unknown_pack", "UNKNOWN_PACK", "cus_credit_unknown_pack", "payment", "paid", "usd", 999
        );

        postWebhook(payload);

        User updated = userRepository.findById(user.getId()).orElseThrow();
        assertThat(updated.getPurchasedAiCreditsBalance()).isZero();
        assertThat(aiCreditPurchaseRepository.findByStripeCheckoutSessionId("cs_credit_unknown_pack")).isEmpty();
    }

    @Test
    void validStarterPowerAndEliteCreditPackPurchasesGrantCatalogCredits() throws Exception {
        assertValidCreditPack("stripe-credit-starter@example.com", "cus_valid_starter", AiCreditPack.STARTER_25, 25);
        assertValidCreditPack("stripe-credit-power@example.com", "cus_valid_power", AiCreditPack.POWER_75, 75);
        assertValidCreditPack("stripe-credit-elite@example.com", "cus_valid_elite", AiCreditPack.ELITE_200, 200);
    }

    @Test
    void creditPackCatalogUsesThePublishedUsdAmounts() {
        assertThat(AiCreditPack.STARTER_25.amountCents()).isEqualTo(999);
        assertThat(AiCreditPack.POWER_75.amountCents()).isEqualTo(2499);
        assertThat(AiCreditPack.ELITE_200.amountCents()).isEqualTo(4999);
        assertThat(AiCreditPack.values()).allSatisfy(pack -> assertThat(pack.currency()).isEqualTo("usd"));
    }

    @Test
    void alreadyProcessedWebhookEventIsTreatedAsDuplicateSuccess() throws Exception {
        User user = createUser("stripe-preprocessed-duplicate@example.com");
        String payload = checkoutCompletedPayload(user.getId());
        processedStripeEventRepository.save(com.tradepilot.billing.ProcessedStripeEvent.of("evt_checkout_" + user.getId(), "checkout.session.completed", dateTime(1_000L)));

        postWebhook(payload);

        User updated = userRepository.findById(user.getId()).orElseThrow();
        assertThat(updated.getPlan()).isEqualTo(PlanType.FREE);
    }

    @Test
    void duplicateProcessedEventIdIsRejectedByDatabaseUniqueness() {
        processedStripeEventRepository.saveAndFlush(com.tradepilot.billing.ProcessedStripeEvent.of(
                "evt_processed_unique",
                "checkout.session.completed",
                dateTime(1_000L)
        ));

        assertThatThrownBy(() -> processedStripeEventRepository.saveAndFlush(com.tradepilot.billing.ProcessedStripeEvent.of(
                "evt_processed_unique",
                "checkout.session.completed",
                dateTime(1_000L)
        ))).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void starterFullRefundReversesPurchasedCredits() throws Exception {
        User user = createPurchasedCreditUser("stripe-refund-starter@example.com", "cus_refund_starter", AiCreditPack.STARTER_25);
        createCompletedCreditPackPurchase(user, AiCreditPack.STARTER_25, "cs_refund_starter", "pi_refund_starter", "ch_refund_starter");

        postWebhook(chargeRefundedPayload("evt_refund_starter", "cus_refund_starter", "pi_refund_starter", "ch_refund_starter", 999));

        assertRefunded(user.getId(), "pi_refund_starter", 0, 25);
    }

    @Test
    void powerFullRefundReversesPurchasedCredits() throws Exception {
        User user = createPurchasedCreditUser("stripe-refund-power@example.com", "cus_refund_power", AiCreditPack.POWER_75);
        createCompletedCreditPackPurchase(user, AiCreditPack.POWER_75, "cs_refund_power", "pi_refund_power", "ch_refund_power");

        postWebhook(chargeRefundedPayload("evt_refund_power", "cus_refund_power", "pi_refund_power", "ch_refund_power", 2499));

        assertRefunded(user.getId(), "pi_refund_power", 0, 75);
    }

    @Test
    void eliteFullRefundReversesPurchasedCredits() throws Exception {
        User user = createPurchasedCreditUser("stripe-refund-elite@example.com", "cus_refund_elite", AiCreditPack.ELITE_200);
        createCompletedCreditPackPurchase(user, AiCreditPack.ELITE_200, "cs_refund_elite", "pi_refund_elite", "ch_refund_elite");

        postWebhook(chargeRefundedPayload("evt_refund_elite", "cus_refund_elite", "pi_refund_elite", "ch_refund_elite", 4999));

        assertRefunded(user.getId(), "pi_refund_elite", 0, 200);
    }

    @Test
    void duplicateRefundEventDoesNotReverseTwice() throws Exception {
        User user = createPurchasedCreditUser("stripe-refund-duplicate@example.com", "cus_refund_duplicate", AiCreditPack.STARTER_25);
        createCompletedCreditPackPurchase(user, AiCreditPack.STARTER_25, "cs_refund_duplicate", "pi_refund_duplicate", "ch_refund_duplicate");

        postWebhook(chargeRefundedPayload("evt_refund_duplicate_1", "cus_refund_duplicate", "pi_refund_duplicate", "ch_refund_duplicate", 999));
        postWebhook(chargeRefundedPayload("evt_refund_duplicate_2", "cus_refund_duplicate", "pi_refund_duplicate", "ch_refund_duplicate", 999));

        assertRefunded(user.getId(), "pi_refund_duplicate", 0, 25);
    }

    @Test
    void fullRefundThenDisputeWonDoesNotRestoreRefundReversal() throws Exception {
        User user = createPurchasedCreditUser("stripe-refund-then-dispute-won@example.com", "cus_refund_then_dispute_won", AiCreditPack.STARTER_25);
        createCompletedCreditPackPurchase(user, AiCreditPack.STARTER_25, "cs_refund_then_dispute_won", "pi_refund_then_dispute_won", "ch_refund_then_dispute_won");

        postWebhook(chargeRefundedPayload("evt_refund_then_dispute_won_refund", "cus_refund_then_dispute_won", "pi_refund_then_dispute_won", "ch_refund_then_dispute_won", 999));
        postWebhook(chargeDisputePayload("evt_refund_then_dispute_won_closed", "charge.dispute.closed", "cus_refund_then_dispute_won", "pi_refund_then_dispute_won", "ch_refund_then_dispute_won", "won"));

        User updated = userRepository.findById(user.getId()).orElseThrow();
        AiCreditPurchase purchase = aiCreditPurchaseRepository.findByStripePaymentIntentId("pi_refund_then_dispute_won").orElseThrow();
        assertThat(updated.getPurchasedAiCreditsBalance()).isZero();
        assertThat(purchase.getRefundReversalCredits()).isEqualTo(25);
        assertThat(purchase.getDisputeRestoredCredits()).isZero();
        assertThat(purchase.getReversedCredits()).isEqualTo(25);
    }

    @Test
    void refundAfterPurchasedCreditsConsumedCanMakePurchasedBalanceNegative() throws Exception {
        User user = createPurchasedCreditUser("stripe-refund-consumed@example.com", "cus_refund_consumed", AiCreditPack.STARTER_25);
        user.setPurchasedAiCreditsBalance(10);
        userRepository.save(user);
        createCompletedCreditPackPurchase(user, AiCreditPack.STARTER_25, "cs_refund_consumed", "pi_refund_consumed", "ch_refund_consumed");

        postWebhook(chargeRefundedPayload("evt_refund_consumed", "cus_refund_consumed", "pi_refund_consumed", "ch_refund_consumed", 999));

        assertRefunded(user.getId(), "pi_refund_consumed", -15, 25);
    }

    @Test
    void disputeCreatedReversesPurchasedCreditsOnce() throws Exception {
        User user = createPurchasedCreditUser("stripe-dispute-created@example.com", "cus_dispute_created", AiCreditPack.POWER_75);
        createCompletedCreditPackPurchase(user, AiCreditPack.POWER_75, "cs_dispute_created", "pi_dispute_created", "ch_dispute_created");

        postWebhook(chargeDisputePayload("evt_dispute_created", "charge.dispute.created", "cus_dispute_created", "pi_dispute_created", "ch_dispute_created", "needs_response"));

        User updated = userRepository.findById(user.getId()).orElseThrow();
        AiCreditPurchase purchase = aiCreditPurchaseRepository.findByStripePaymentIntentId("pi_dispute_created").orElseThrow();
        assertThat(updated.getPurchasedAiCreditsBalance()).isZero();
        assertThat(purchase.getStatus()).isEqualTo(AiCreditPurchaseStatus.DISPUTED);
        assertThat(purchase.getReversedCredits()).isEqualTo(75);
    }

    @Test
    void disputeWonRestoresPreviouslyReversedCredits() throws Exception {
        User user = createPurchasedCreditUser("stripe-dispute-won@example.com", "cus_dispute_won", AiCreditPack.STARTER_25);
        createCompletedCreditPackPurchase(user, AiCreditPack.STARTER_25, "cs_dispute_won", "pi_dispute_won", "ch_dispute_won");

        postWebhook(chargeDisputePayload("evt_dispute_won_created", "charge.dispute.created", "cus_dispute_won", "pi_dispute_won", "ch_dispute_won", "needs_response"));
        postWebhook(chargeDisputePayload("evt_dispute_won_closed", "charge.dispute.closed", "cus_dispute_won", "pi_dispute_won", "ch_dispute_won", "won"));

        User updated = userRepository.findById(user.getId()).orElseThrow();
        AiCreditPurchase purchase = aiCreditPurchaseRepository.findByStripePaymentIntentId("pi_dispute_won").orElseThrow();
        assertThat(updated.getPurchasedAiCreditsBalance()).isEqualTo(25);
        assertThat(purchase.getStatus()).isEqualTo(AiCreditPurchaseStatus.DISPUTE_WON);
        assertThat(purchase.getReversedCredits()).isZero();
        assertThat(purchase.getDisputeReversalCredits()).isEqualTo(25);
        assertThat(purchase.getDisputeRestoredCredits()).isEqualTo(25);
    }

    @Test
    void disputeLostDoesNotReverseCreditsTwice() throws Exception {
        User user = createPurchasedCreditUser("stripe-dispute-lost@example.com", "cus_dispute_lost", AiCreditPack.ELITE_200);
        createCompletedCreditPackPurchase(user, AiCreditPack.ELITE_200, "cs_dispute_lost", "pi_dispute_lost", "ch_dispute_lost");

        postWebhook(chargeDisputePayload("evt_dispute_lost_created", "charge.dispute.created", "cus_dispute_lost", "pi_dispute_lost", "ch_dispute_lost", "needs_response"));
        postWebhook(chargeDisputePayload("evt_dispute_lost_closed", "charge.dispute.closed", "cus_dispute_lost", "pi_dispute_lost", "ch_dispute_lost", "lost"));

        User updated = userRepository.findById(user.getId()).orElseThrow();
        AiCreditPurchase purchase = aiCreditPurchaseRepository.findByStripePaymentIntentId("pi_dispute_lost").orElseThrow();
        assertThat(updated.getPurchasedAiCreditsBalance()).isZero();
        assertThat(purchase.getStatus()).isEqualTo(AiCreditPurchaseStatus.DISPUTE_LOST);
        assertThat(purchase.getReversedCredits()).isEqualTo(200);
        assertThat(purchase.getDisputeReversalCredits()).isEqualTo(200);
        assertThat(purchase.getDisputeRestoredCredits()).isZero();
    }

    @Test
    void disputeWonThenFullRefundLeavesCreditsReversed() throws Exception {
        User user = createPurchasedCreditUser("stripe-dispute-won-then-refund@example.com", "cus_dispute_won_then_refund", AiCreditPack.STARTER_25);
        createCompletedCreditPackPurchase(user, AiCreditPack.STARTER_25, "cs_dispute_won_then_refund", "pi_dispute_won_then_refund", "ch_dispute_won_then_refund");

        postWebhook(chargeDisputePayload("evt_dispute_won_then_refund_created", "charge.dispute.created", "cus_dispute_won_then_refund", "pi_dispute_won_then_refund", "ch_dispute_won_then_refund", "needs_response"));
        postWebhook(chargeDisputePayload("evt_dispute_won_then_refund_won", "charge.dispute.closed", "cus_dispute_won_then_refund", "pi_dispute_won_then_refund", "ch_dispute_won_then_refund", "won"));
        postWebhook(chargeRefundedPayload("evt_dispute_won_then_refund_refund", "cus_dispute_won_then_refund", "pi_dispute_won_then_refund", "ch_dispute_won_then_refund", 999));

        User updated = userRepository.findById(user.getId()).orElseThrow();
        AiCreditPurchase purchase = aiCreditPurchaseRepository.findByStripePaymentIntentId("pi_dispute_won_then_refund").orElseThrow();
        assertThat(updated.getPurchasedAiCreditsBalance()).isZero();
        assertThat(purchase.getRefundReversalCredits()).isEqualTo(25);
        assertThat(purchase.getDisputeReversalCredits()).isEqualTo(25);
        assertThat(purchase.getDisputeRestoredCredits()).isEqualTo(25);
        assertThat(purchase.getReversedCredits()).isEqualTo(25);
    }

    @Test
    void fullRefundThenDisputeCreatedThenWonLeavesCreditsReversed() throws Exception {
        User user = createPurchasedCreditUser("stripe-refund-dispute-won@example.com", "cus_refund_dispute_won", AiCreditPack.STARTER_25);
        createCompletedCreditPackPurchase(user, AiCreditPack.STARTER_25, "cs_refund_dispute_won", "pi_refund_dispute_won", "ch_refund_dispute_won");

        postWebhook(chargeRefundedPayload("evt_refund_dispute_won_refund", "cus_refund_dispute_won", "pi_refund_dispute_won", "ch_refund_dispute_won", 999));
        postWebhook(chargeDisputePayload("evt_refund_dispute_won_created", "charge.dispute.created", "cus_refund_dispute_won", "pi_refund_dispute_won", "ch_refund_dispute_won", "needs_response"));
        postWebhook(chargeDisputePayload("evt_refund_dispute_won_won", "charge.dispute.closed", "cus_refund_dispute_won", "pi_refund_dispute_won", "ch_refund_dispute_won", "won"));

        User updated = userRepository.findById(user.getId()).orElseThrow();
        AiCreditPurchase purchase = aiCreditPurchaseRepository.findByStripePaymentIntentId("pi_refund_dispute_won").orElseThrow();
        assertThat(updated.getPurchasedAiCreditsBalance()).isZero();
        assertThat(purchase.getRefundReversalCredits()).isEqualTo(25);
        assertThat(purchase.getDisputeReversalCredits()).isEqualTo(25);
        assertThat(purchase.getDisputeRestoredCredits()).isEqualTo(25);
        assertThat(purchase.getReversedCredits()).isEqualTo(25);
    }

    @Test
    void duplicateDisputeCreatedReversesOnce() throws Exception {
        User user = createPurchasedCreditUser("stripe-dispute-created-duplicate@example.com", "cus_dispute_created_duplicate", AiCreditPack.STARTER_25);
        createCompletedCreditPackPurchase(user, AiCreditPack.STARTER_25, "cs_dispute_created_duplicate", "pi_dispute_created_duplicate", "ch_dispute_created_duplicate");

        postWebhook(chargeDisputePayload("evt_dispute_created_duplicate_1", "charge.dispute.created", "cus_dispute_created_duplicate", "pi_dispute_created_duplicate", "ch_dispute_created_duplicate", "needs_response"));
        postWebhook(chargeDisputePayload("evt_dispute_created_duplicate_2", "charge.dispute.created", "cus_dispute_created_duplicate", "pi_dispute_created_duplicate", "ch_dispute_created_duplicate", "needs_response"));

        User updated = userRepository.findById(user.getId()).orElseThrow();
        AiCreditPurchase purchase = aiCreditPurchaseRepository.findByStripePaymentIntentId("pi_dispute_created_duplicate").orElseThrow();
        assertThat(updated.getPurchasedAiCreditsBalance()).isZero();
        assertThat(purchase.getDisputeReversalCredits()).isEqualTo(25);
        assertThat(purchase.getDisputeRestoredCredits()).isZero();
        assertThat(purchase.getReversedCredits()).isEqualTo(25);
    }

    @Test
    void duplicateDisputeWonRestoresOnce() throws Exception {
        User user = createPurchasedCreditUser("stripe-dispute-won-duplicate@example.com", "cus_dispute_won_duplicate", AiCreditPack.STARTER_25);
        createCompletedCreditPackPurchase(user, AiCreditPack.STARTER_25, "cs_dispute_won_duplicate", "pi_dispute_won_duplicate", "ch_dispute_won_duplicate");

        postWebhook(chargeDisputePayload("evt_dispute_won_duplicate_created", "charge.dispute.created", "cus_dispute_won_duplicate", "pi_dispute_won_duplicate", "ch_dispute_won_duplicate", "needs_response"));
        postWebhook(chargeDisputePayload("evt_dispute_won_duplicate_1", "charge.dispute.closed", "cus_dispute_won_duplicate", "pi_dispute_won_duplicate", "ch_dispute_won_duplicate", "won"));
        postWebhook(chargeDisputePayload("evt_dispute_won_duplicate_2", "charge.dispute.closed", "cus_dispute_won_duplicate", "pi_dispute_won_duplicate", "ch_dispute_won_duplicate", "won"));

        User updated = userRepository.findById(user.getId()).orElseThrow();
        AiCreditPurchase purchase = aiCreditPurchaseRepository.findByStripePaymentIntentId("pi_dispute_won_duplicate").orElseThrow();
        assertThat(updated.getPurchasedAiCreditsBalance()).isEqualTo(25);
        assertThat(purchase.getDisputeReversalCredits()).isEqualTo(25);
        assertThat(purchase.getDisputeRestoredCredits()).isEqualTo(25);
        assertThat(purchase.getReversedCredits()).isZero();
    }

    @Test
    void refundAndDisputeEventsConcurrentlyProduceOneEconomicReversal() throws Exception {
        User user = createPurchasedCreditUser("stripe-refund-dispute-concurrent@example.com", "cus_refund_dispute_concurrent", AiCreditPack.STARTER_25);
        createCompletedCreditPackPurchase(user, AiCreditPack.STARTER_25, "cs_refund_dispute_concurrent", "pi_refund_dispute_concurrent", "ch_refund_dispute_concurrent");
        String refundPayload = chargeRefundedPayload("evt_refund_dispute_concurrent_refund", "cus_refund_dispute_concurrent", "pi_refund_dispute_concurrent", "ch_refund_dispute_concurrent", 999);
        String disputePayload = chargeDisputePayload("evt_refund_dispute_concurrent_dispute", "charge.dispute.created", "cus_refund_dispute_concurrent", "pi_refund_dispute_concurrent", "ch_refund_dispute_concurrent", "needs_response");

        runConcurrently(List.of(() -> postWebhook(refundPayload), () -> postWebhook(disputePayload)));

        User updated = userRepository.findById(user.getId()).orElseThrow();
        AiCreditPurchase purchase = aiCreditPurchaseRepository.findByStripePaymentIntentId("pi_refund_dispute_concurrent").orElseThrow();
        assertThat(updated.getPurchasedAiCreditsBalance()).isZero();
        assertThat(purchase.getRefundReversalCredits()).isEqualTo(25);
        assertThat(purchase.getDisputeReversalCredits()).isEqualTo(25);
        assertThat(purchase.getReversedCredits()).isEqualTo(25);
    }

    @Test
    void refundEventForUnknownPurchaseIsNoOp() throws Exception {
        postWebhook(chargeRefundedPayload("evt_refund_unknown", "cus_refund_unknown", "pi_refund_unknown", "ch_refund_unknown", 999));

        assertThat(aiCreditPurchaseRepository.findByStripePaymentIntentId("pi_refund_unknown")).isEmpty();
    }

    @Test
    void refundEventForWrongCustomerIsNoOp() throws Exception {
        User user = createPurchasedCreditUser("stripe-refund-wrong-customer@example.com", "cus_refund_expected", AiCreditPack.STARTER_25);
        createCompletedCreditPackPurchase(user, AiCreditPack.STARTER_25, "cs_refund_wrong_customer", "pi_refund_wrong_customer", "ch_refund_wrong_customer");

        postWebhook(chargeRefundedPayload("evt_refund_wrong_customer", "cus_refund_other", "pi_refund_wrong_customer", "ch_refund_wrong_customer", 999));

        User updated = userRepository.findById(user.getId()).orElseThrow();
        AiCreditPurchase purchase = aiCreditPurchaseRepository.findByStripePaymentIntentId("pi_refund_wrong_customer").orElseThrow();
        assertThat(updated.getPurchasedAiCreditsBalance()).isEqualTo(25);
        assertThat(purchase.getStatus()).isEqualTo(AiCreditPurchaseStatus.COMPLETED);
        assertThat(purchase.getReversedCredits()).isZero();
    }

    @Test
    void samePurchaseReferencedByMultipleStripeEventsDoesNotDoubleReverse() throws Exception {
        User user = createPurchasedCreditUser("stripe-refund-dispute-same-purchase@example.com", "cus_refund_dispute_same", AiCreditPack.POWER_75);
        createCompletedCreditPackPurchase(user, AiCreditPack.POWER_75, "cs_refund_dispute_same", "pi_refund_dispute_same", "ch_refund_dispute_same");

        postWebhook(chargeDisputePayload("evt_refund_dispute_same_created", "charge.dispute.created", "cus_refund_dispute_same", "pi_refund_dispute_same", "ch_refund_dispute_same", "needs_response"));
        postWebhook(chargeRefundedPayload("evt_refund_dispute_same_refund", "cus_refund_dispute_same", "pi_refund_dispute_same", "ch_refund_dispute_same", 2499));

        User updated = userRepository.findById(user.getId()).orElseThrow();
        AiCreditPurchase purchase = aiCreditPurchaseRepository.findByStripePaymentIntentId("pi_refund_dispute_same").orElseThrow();
        assertThat(updated.getPurchasedAiCreditsBalance()).isZero();
        assertThat(purchase.getReversedCredits()).isEqualTo(75);
    }

    private User createPurchasedCreditUser(String email, String customerId, AiCreditPack pack) {
        User user = createProStripeUser(email, customerId);
        user.setPurchasedAiCreditsBalance(pack.credits());
        return userRepository.save(user);
    }

    private AiCreditPurchase createCompletedCreditPackPurchase(
            User user,
            AiCreditPack pack,
            String checkoutSessionId,
            String paymentIntentId,
            String chargeId
    ) {
        return aiCreditPurchaseRepository.save(AiCreditPurchase.builder()
                .userId(user.getId())
                .stripeCheckoutSessionId(checkoutSessionId)
                .stripeCustomerId(user.getStripeCustomerId())
                .stripePaymentIntentId(paymentIntentId)
                .stripeChargeId(chargeId)
                .packId(pack.name())
                .creditsPurchased(pack.credits())
                .amountCents(pack.amountCents())
                .refundedAmountCents(0)
                .reversedCredits(0)
                .refundReversalCredits(0)
                .disputeReversalCredits(0)
                .disputeRestoredCredits(0)
                .currency(pack.currency())
                .status(AiCreditPurchaseStatus.COMPLETED)
                .completedAt(LocalDateTime.now())
                .build());
    }

    private void assertRefunded(Long userId, String paymentIntentId, int expectedBalance, int expectedReversedCredits) {
        User updated = userRepository.findById(userId).orElseThrow();
        AiCreditPurchase purchase = aiCreditPurchaseRepository.findByStripePaymentIntentId(paymentIntentId).orElseThrow();
        assertThat(updated.getPurchasedAiCreditsBalance()).isEqualTo(expectedBalance);
        assertThat(purchase.getStatus()).isEqualTo(AiCreditPurchaseStatus.REFUNDED);
        assertThat(purchase.getReversedCredits()).isEqualTo(expectedReversedCredits);
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

    private List<FeatureUsageEvent> lifecycleEvents(User user, FeatureUsageAction action) {
        return featureUsageRepository.findByUserIdOrderByCreatedAtAscIdAsc(user.getId()).stream()
                .filter(event -> event.getAction() == action)
                .toList();
    }

    private void assertLifecycleEventCount(User user, FeatureKey feature, FeatureUsageAction action, int expected) {
        assertThat(lifecycleEvents(user, action))
                .filteredOn(event -> event.getFeature() == feature)
                .hasSize(expected);
    }

    private void recordUpgradeFunnelPrelude(User user) {
        for (FeatureUsageEvent event : List.of(
                lifecycleEvent(user, FeatureKey.TRADING_DNA, FeatureUsageAction.LOCKED_PREVIEW_VIEWED, FeatureUsageEventType.PAGE_VIEW),
                lifecycleEvent(user, FeatureKey.TRADING_DNA, FeatureUsageAction.UPGRADE_CTA_CLICKED, FeatureUsageEventType.USER_ACTION),
                lifecycleEvent(user, FeatureKey.PLAYBOOK, FeatureUsageAction.BILLING_PAGE_VIEWED, FeatureUsageEventType.PAGE_VIEW)
        )) {
            featureUsageRepository.save(event);
        }
    }

    private FeatureUsageEvent lifecycleEvent(User user, FeatureKey feature, FeatureUsageAction action, FeatureUsageEventType eventType) {
        return FeatureUsageEvent.builder().userId(user.getId()).feature(feature).action(action).eventType(eventType)
                .planAtEvent(user.getPlan().name()).occurredAt(LocalDateTime.now()).createdAt(LocalDateTime.now()).build();
    }

    private User createProStripeUser(String email, String stripeCustomerId) {
        User user = createUser(email);
        user.setPlan(PlanType.PRO);
        user.setSubscriptionStatus(SubscriptionStatus.ACTIVE);
        user.setStripeCustomerId(stripeCustomerId);
        return userRepository.save(user);
    }

    private void postWebhook(String payload) throws Exception {
        mockMvc.perform(post("/api/stripe/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Stripe-Signature", stripeSignature(payload))
                        .content(payload))
                .andExpect(status().isOk());
    }

    private void postWebhookExpect(String payload, ResultMatcher statusMatcher) throws Exception {
        mockMvc.perform(post("/api/stripe/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Stripe-Signature", stripeSignature(payload))
                        .content(payload))
                .andExpect(statusMatcher);
    }

    private void runConcurrently(List<WebhookTask> tasks) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(tasks.size());
        CountDownLatch ready = new CountDownLatch(tasks.size());
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<? extends Future<?>> futures = tasks.stream()
                    .map(task -> executor.submit(() -> {
                        ready.countDown();
                        if (!start.await(5, TimeUnit.SECONDS)) {
                            throw new IllegalStateException("Timed out waiting to start concurrent webhook task");
                        }
                        task.run();
                        return null;
                    }))
                    .toList();
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            for (Future<?> future : futures) {
                future.get(10, TimeUnit.SECONDS);
            }
        } finally {
            executor.shutdownNow();
        }
    }

    private void setCreditPackFulfillmentHooks(Runnable afterClaim, Runnable afterIncrement) {
        ReflectionTestUtils.setField(stripeBillingService, "afterCreditPackFulfillmentClaimed", afterClaim);
        ReflectionTestUtils.setField(stripeBillingService, "afterCreditPackCreditsIncremented", afterIncrement);
    }

    private void resetCreditPackFulfillmentHooks() {
        setCreditPackFulfillmentHooks(() -> {
        }, () -> {
        });
    }

    private void assertCreditPackRejected(String payloadWithPlaceholderUserId, String email, String stripeCustomerId) throws Exception {
        User user = createProStripeUser(email, stripeCustomerId);
        String payload = payloadWithPlaceholderUserId.replace("\"userId\": \"0\"", "\"userId\": \"" + user.getId() + "\"");

        postWebhook(payload);

        User updated = userRepository.findById(user.getId()).orElseThrow();
        assertThat(updated.getPurchasedAiCreditsBalance()).isZero();
    }

    @FunctionalInterface
    private interface WebhookTask {
        void run() throws Exception;
    }

    private void assertValidCreditPack(String email, String customerId, AiCreditPack pack, int expectedCredits) throws Exception {
        User user = createProStripeUser(email, customerId);
        String eventId = "evt_" + pack.name().toLowerCase();
        String sessionId = "cs_" + pack.name().toLowerCase();
        String payload = creditPackCheckoutCompletedPayload(
                user.getId(), eventId, sessionId, pack, customerId, "payment", "paid", pack.currency(), pack.amountCents()
        );

        postWebhook(payload);

        User updated = userRepository.findById(user.getId()).orElseThrow();
        assertThat(updated.getPurchasedAiCreditsBalance()).isEqualTo(expectedCredits);
        assertThat(aiCreditPurchaseRepository.findByStripeCheckoutSessionId(sessionId)).isPresent();
    }

    @Test
    void differentStripeEventsForSameCreditPackSessionGrantCreditsOnlyOnce() throws Exception {
        User user = createProStripeUser("stripe-credit-same-session@example.com", "cus_credit_same_session");
        String firstPayload = creditPackCheckoutCompletedPayload(
                user.getId(), "evt_credit_same_session_1", "cs_credit_same_session", AiCreditPack.POWER_75, "cus_credit_same_session", "payment", "paid", "usd", 2499
        );
        String secondPayload = creditPackCheckoutCompletedPayload(
                user.getId(), "evt_credit_same_session_2", "cs_credit_same_session", AiCreditPack.POWER_75, "cus_credit_same_session", "payment", "paid", "usd", 2499
        );

        postWebhook(firstPayload);
        postWebhook(secondPayload);

        User updated = userRepository.findById(user.getId()).orElseThrow();
        assertThat(updated.getPurchasedAiCreditsBalance()).isEqualTo(75);
        assertThat(aiCreditPurchaseRepository.findByStripeCheckoutSessionId("cs_credit_same_session")).isPresent();
    }

    @Test
    void concurrentDifferentStripeEventsForSameCreditPackSessionGrantCreditsOnlyOnce() throws Exception {
        User user = createProStripeUser("stripe-credit-same-session-concurrent@example.com", "cus_credit_same_session_concurrent");
        String firstPayload = creditPackCheckoutCompletedPayload(
                user.getId(), "evt_credit_same_session_concurrent_1", "cs_credit_same_session_concurrent", AiCreditPack.POWER_75, "cus_credit_same_session_concurrent", "payment", "paid", "usd", 2499
        );
        String secondPayload = creditPackCheckoutCompletedPayload(
                user.getId(), "evt_credit_same_session_concurrent_2", "cs_credit_same_session_concurrent", AiCreditPack.POWER_75, "cus_credit_same_session_concurrent", "payment", "paid", "usd", 2499
        );

        runConcurrently(List.of(() -> postWebhook(firstPayload), () -> postWebhook(secondPayload)));

        User updated = userRepository.findById(user.getId()).orElseThrow();
        assertThat(updated.getPurchasedAiCreditsBalance()).isEqualTo(75);
        assertThat(aiCreditPurchaseRepository.findByStripeCheckoutSessionId("cs_credit_same_session_concurrent")).isPresent();
        assertThat(aiCreditPurchaseRepository.findAll().stream()
                .filter(purchase -> "cs_credit_same_session_concurrent".equals(purchase.getStripeCheckoutSessionId()))
                .count()).isEqualTo(1);
    }

    @Test
    void repeatedSameCreditPackEventGrantsCreditsOnlyOnce() throws Exception {
        User user = createProStripeUser("stripe-credit-same-event@example.com", "cus_credit_same_event");
        String payload = creditPackCheckoutCompletedPayload(
                user.getId(), "evt_credit_same_event", "cs_credit_same_event", AiCreditPack.STARTER_25, "cus_credit_same_event", "payment", "paid", "usd", 999
        );

        postWebhook(payload);
        postWebhook(payload);

        User updated = userRepository.findById(user.getId()).orElseThrow();
        assertThat(updated.getPurchasedAiCreditsBalance()).isEqualTo(25);
        assertThat(aiCreditPurchaseRepository.findByStripeCheckoutSessionId("cs_credit_same_event")).isPresent();
    }

    @Test
    void preexistingCreditPackLedgerClaimCausesZeroAdditionalGrant() throws Exception {
        User user = createProStripeUser("stripe-credit-ledger-claimed@example.com", "cus_credit_ledger_claimed");
        createCompletedCreditPackPurchase(user, AiCreditPack.STARTER_25, "cs_credit_ledger_claimed", "pi_credit_ledger_claimed", null);
        user.setPurchasedAiCreditsBalance(25);
        userRepository.save(user);
        String payload = creditPackCheckoutCompletedPayload(
                user.getId(), "evt_credit_ledger_claimed_retry", "cs_credit_ledger_claimed", AiCreditPack.STARTER_25, "cus_credit_ledger_claimed", "payment", "paid", "usd", 999
        );

        postWebhook(payload);

        User updated = userRepository.findById(user.getId()).orElseThrow();
        assertThat(updated.getPurchasedAiCreditsBalance()).isEqualTo(25);
        assertThat(aiCreditPurchaseRepository.findAll().stream()
                .filter(purchase -> "cs_credit_ledger_claimed".equals(purchase.getStripeCheckoutSessionId()))
                .count()).isEqualTo(1);
    }

    @Test
    void failureAfterCreditPackFulfillmentClaimRollsBackAndRetryGrantsOnce() throws Exception {
        User user = createProStripeUser("stripe-credit-after-claim-failure@example.com", "cus_credit_after_claim_failure");
        String payload = creditPackCheckoutCompletedPayload(
                user.getId(), "evt_credit_after_claim_failure", "cs_credit_after_claim_failure", AiCreditPack.STARTER_25, "cus_credit_after_claim_failure", "payment", "paid", "usd", 999
        );
        setCreditPackFulfillmentHooks(() -> {
            throw new IllegalStateException("after claim test failure");
        }, () -> {
        });

        try {
            postWebhookExpect(payload, status().is5xxServerError());
        } finally {
            resetCreditPackFulfillmentHooks();
        }
        assertThat(userRepository.findById(user.getId()).orElseThrow().getPurchasedAiCreditsBalance()).isZero();
        assertThat(aiCreditPurchaseRepository.findByStripeCheckoutSessionId("cs_credit_after_claim_failure")).isEmpty();

        postWebhook(payload);

        User updated = userRepository.findById(user.getId()).orElseThrow();
        assertThat(updated.getPurchasedAiCreditsBalance()).isEqualTo(25);
        assertThat(aiCreditPurchaseRepository.findByStripeCheckoutSessionId("cs_credit_after_claim_failure")).isPresent();
    }

    @Test
    void failureAfterCreditPackIncrementRollsBackAndRetryGrantsOnce() throws Exception {
        User user = createProStripeUser("stripe-credit-after-increment-failure@example.com", "cus_credit_after_increment_failure");
        String payload = creditPackCheckoutCompletedPayload(
                user.getId(), "evt_credit_after_increment_failure", "cs_credit_after_increment_failure", AiCreditPack.ELITE_200, "cus_credit_after_increment_failure", "payment", "paid", "usd", 4999
        );
        setCreditPackFulfillmentHooks(() -> {
        }, () -> {
            throw new IllegalStateException("after increment test failure");
        });

        try {
            postWebhookExpect(payload, status().is5xxServerError());
        } finally {
            resetCreditPackFulfillmentHooks();
        }
        assertThat(userRepository.findById(user.getId()).orElseThrow().getPurchasedAiCreditsBalance()).isZero();
        assertThat(aiCreditPurchaseRepository.findByStripeCheckoutSessionId("cs_credit_after_increment_failure")).isEmpty();

        postWebhook(payload);

        User updated = userRepository.findById(user.getId()).orElseThrow();
        assertThat(updated.getPurchasedAiCreditsBalance()).isEqualTo(200);
        assertThat(aiCreditPurchaseRepository.findByStripeCheckoutSessionId("cs_credit_after_increment_failure")).isPresent();
    }

    private TradingAccountResponse createAccount(User user, String name) {
        return tradingAccountService.create(
                user,
                new TradingAccountRequest(
                        name,
                        PropFirmType.TOPSTEP,
                        null,
                        null,
                        BigDecimal.valueOf(50_000),
                        null,
                        TradingAccountStatus.ACTIVE
                )
        );
    }

    private String checkoutCompletedPayload(Long userId) {
        return """
                {
                  "id": "evt_checkout_%s",
                  "type": "checkout.session.completed",
                  "created": 1000,
                  "data": {
                    "object": {
                      "id": "cs_test_123",
                      "object": "checkout.session",
                      "mode": "subscription",
                      "customer": "cus_test_123",
                      "subscription": "sub_test_123",
                      "metadata": {
                        "userId": "%s",
                        "userEmail": "stripe@example.com"
                      }
                    }
                  }
                }
                """.formatted(userId, userId);
    }

    private String creditPackCheckoutCompletedPayload(Long userId, String eventId, String sessionId) {
        return creditPackCheckoutCompletedPayload(userId, eventId, sessionId, AiCreditPack.STARTER_25, "cus_credit_pack_123", "payment", "paid", "usd", 999);
    }

    private String creditPackCheckoutCompletedPayload(Long userId, String eventId, String sessionId, AiCreditPack pack, String customerId, String mode, String paymentStatus, String currency, int amountTotal) {
        return creditPackCheckoutCompletedPayload(userId, eventId, sessionId, pack.name(), customerId, mode, paymentStatus, currency, amountTotal);
    }

    private String creditPackCheckoutCompletedPayload(Long userId, String eventId, String sessionId, String packId, String customerId, String mode, String paymentStatus, String currency, int amountTotal) {
        return """
                {
                  "id": "%s",
                  "type": "checkout.session.completed",
                  "created": 1000,
                  "data": {
                    "object": {
                      "id": "%s",
                      "object": "checkout.session",
                      "mode": "%s",
                      "payment_status": "%s",
                      "customer": "%s",
                      "payment_intent": "pi_%s",
                      "amount_total": %s,
                      "currency": "%s",
                      "metadata": {
                        "type": "AI_CREDIT_PACK",
                        "userId": "%s",
                        "packId": "%s",
                        "credits": "9999"
                      }
                    }
                  }
                }
                """.formatted(eventId, sessionId, mode, paymentStatus, customerId, eventId, amountTotal, currency, userId, packId);
    }

    private String subscriptionDeletedPayload() {
        return subscriptionDeletedPayload("evt_deleted", "cus_delete_123", "sub_delete_123");
    }

    private String subscriptionDeletedPayload(String eventId, String customerId, String subscriptionId) {
        return subscriptionDeletedPayload(eventId, 1_000L, customerId, subscriptionId);
    }

    private String subscriptionDeletedPayload(String eventId, long created, String customerId, String subscriptionId) {
        return """
                {
                  "id": "%s",
                  "type": "customer.subscription.deleted",
                  "created": %s,
                  "data": {
                    "object": {
                      "id": "%s",
                      "object": "subscription",
                      "customer": "%s",
                      "status": "canceled"
                    }
                  }
                }
                """.formatted(eventId, created, subscriptionId, customerId);
    }

    private String subscriptionUpdatedPayload() {
        return subscriptionUpdatedPayload("evt_subscription_updated", 1_000L, "cus_update_123", "sub_update_123", "active");
    }

    private String subscriptionUpdatedPayload(String eventId, long created, String customerId, String subscriptionId, String status) {
        return """
                {
                  "id": "%s",
                  "type": "customer.subscription.updated",
                  "created": %s,
                  "data": {
                    "object": {
                      "id": "%s",
                      "object": "subscription",
                      "customer": "%s",
                      "status": "%s",
                      "cancel_at_period_end": false,
                      "cancel_at": null,
                      "canceled_at": null,
                      "metadata": {
                        "userId": "safe-key-only"
                      },
                      "items": {
                        "object": "list",
                        "data": [
                          {
                            "id": "si_update_123",
                            "object": "subscription_item",
                            "current_period_start": 1751328000,
                            "current_period_end": 1754006400
                          }
                        ]
                      }
                    }
                  }
                }
                """.formatted(eventId, created, subscriptionId, customerId, status);
    }

    private String subscriptionCancelAtPeriodEndPayload() {
        return """
                {
                  "id": "evt_subscription_cancel_at_period_end",
                  "type": "customer.subscription.updated",
                  "created": 1000,
                  "data": {
                    "object": {
                      "id": "sub_cancel_period_123",
                      "object": "subscription",
                      "customer": "cus_cancel_period_123",
                      "status": "active",
                      "cancel_at_period_end": true,
                      "cancel_at": 1754006400,
                      "canceled_at": 1751760000,
                      "items": {
                        "object": "list",
                        "data": [
                          {
                            "id": "si_cancel_period_123",
                            "object": "subscription_item",
                            "current_period_start": 1751328000,
                            "current_period_end": 1754006400
                          }
                        ]
                      }
                    }
                  }
                }
                """;
    }

    private String subscriptionUpdatedJulyFirstPayload() {
        return """
                {
                  "id": "evt_subscription_july_first",
                  "type": "customer.subscription.updated",
                  "created": 1000,
                  "data": {
                    "object": {
                      "id": "sub_july_123",
                      "object": "subscription",
                      "customer": "cus_july_123",
                      "status": "active",
                      "cancel_at_period_end": false,
                      "items": {
                        "object": "list",
                        "data": [
                          {
                            "id": "si_july_123",
                            "object": "subscription_item",
                            "current_period_start": 1780272000,
                            "current_period_end": 1782864000
                          }
                        ]
                      }
                    }
                  }
                }
                """;
    }

    private String invoicePaymentPaidPayload() {
        return """
                {
                  "id": "evt_invoice_payment_paid",
                  "type": "invoice_payment.paid",
                  "created": 1000,
                  "data": {
                    "object": {
                      "id": "inpay_test_123",
                      "object": "invoice_payment",
                      "status": "paid",
                      "invoice": {
                        "id": "in_test_123",
                        "object": "invoice",
                        "customer": "cus_invoice_paid_123",
                        "subscription": "sub_invoice_paid_123",
                        "lines": {
                          "object": "list",
                          "data": [
                            {
                              "id": "il_test_123",
                              "period": {
                                "start": 1740873600,
                                "end": 1743465600
                              }
                            }
                          ]
                        }
                      }
                    }
                  }
                }
                """;
    }

    private String invoicePaymentPaidTextualInvoicePayload() {
        return """
                {
                  "id": "evt_invoice_payment_paid_textual",
                  "type": "invoice_payment.paid",
                  "created": 1000,
                  "data": {
                    "object": {
                      "id": "inpay_textual_123",
                      "object": "invoice_payment",
                      "status": "paid",
                      "invoice": "in_textual_123"
                    }
                  }
                }
                """;
    }

    private String invoicePaymentSucceededBadSamePeriodPayload() {
        return """
                {
                  "id": "evt_invoice_bad_same_period",
                  "type": "invoice.payment_succeeded",
                  "created": 1000,
                  "data": {
                    "object": {
                      "id": "in_bad_same_period",
                      "object": "invoice",
                      "customer": "cus_invoice_bad_123",
                      "subscription": "sub_invoice_bad_123",
                      "period_start": 1780304937,
                      "period_end": 1780304937,
                      "lines": {
                        "object": "list",
                        "data": [
                          {
                            "id": "il_bad_same_period",
                            "period": {
                              "start": 1780304937,
                              "end": 1780304937
                            }
                          }
                        ]
                      }
                    }
                  }
                }
                """;
    }

    private String invoicePaymentSucceededWithBadInvoicePeriodPayload() {
        return """
                {
                  "id": "evt_invoice_subscription_period",
                  "type": "invoice.payment_succeeded",
                  "created": 1000,
                  "data": {
                    "object": {
                      "id": "in_subscription_period",
                      "object": "invoice",
                      "customer": "cus_invoice_subscription_123",
                      "subscription": "sub_invoice_subscription_123",
                      "period_start": 1780304937,
                      "period_end": 1780304937,
                      "lines": {
                        "object": "list",
                        "data": [
                          {
                            "id": "il_subscription_period",
                            "period": {
                              "start": 1780304937,
                              "end": 1780304937
                            }
                          }
                        ]
                      }
                    }
                  }
                }
                """;
    }

    private String invoicePaymentFailedPayload(String eventId, long created, String customerId, String subscriptionId) {
        return invoicePayload(eventId, "invoice.payment_failed", created, customerId, subscriptionId);
    }

    private String invoicePaymentSucceededPayload(String eventId, long created, String customerId, String subscriptionId) {
        return invoicePayload(eventId, "invoice.payment_succeeded", created, customerId, subscriptionId);
    }

    private String invoicePayload(String eventId, String type, long created, String customerId, String subscriptionId) {
        return """
                {
                  "id": "%s",
                  "type": "%s",
                  "created": %s,
                  "data": {
                    "object": {
                      "id": "in_%s",
                      "object": "invoice",
                      "customer": "%s",
                      "subscription": "%s",
                      "lines": {
                        "object": "list",
                        "data": [
                          {
                            "id": "il_%s",
                            "period": {
                              "start": 1780272000,
                              "end": 1782864000
                            }
                          }
                        ]
                      }
                    }
                  }
                }
                """.formatted(eventId, type, created, eventId, customerId, subscriptionId, eventId);
    }

    private String chargeRefundedPayload(String eventId, String customerId, String paymentIntentId, String chargeId, int amountRefunded) {
        return """
                {
                  "id": "%s",
                  "type": "charge.refunded",
                  "created": 1000,
                  "data": {
                    "object": {
                      "id": "%s",
                      "object": "charge",
                      "customer": "%s",
                      "payment_intent": "%s",
                      "amount_refunded": %s,
                      "refunded": true
                    }
                  }
                }
                """.formatted(eventId, chargeId, customerId, paymentIntentId, amountRefunded);
    }

    private String chargeDisputePayload(String eventId, String type, String customerId, String paymentIntentId, String chargeId, String status) {
        return """
                {
                  "id": "%s",
                  "type": "%s",
                  "created": 1000,
                  "data": {
                    "object": {
                      "id": "dp_%s",
                      "object": "dispute",
                      "status": "%s",
                      "charge": {
                        "id": "%s",
                        "object": "charge",
                        "customer": "%s",
                        "payment_intent": "%s"
                      }
                    }
                  }
                }
                """.formatted(eventId, type, eventId, status, chargeId, customerId, paymentIntentId);
    }

    private String withLivemode(String payload, boolean livemode) {
        return payload.replaceFirst("\\{", "{\n  \"livemode\": " + livemode + ",");
    }

    private String stripeSignature(String payload) throws Exception {
        long timestamp = Instant.now().getEpochSecond();
        String signedPayload = timestamp + "." + payload;
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec("whsec_test".getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] digest = mac.doFinal(signedPayload.getBytes(StandardCharsets.UTF_8));
        return "t=" + timestamp + ",v1=" + hex(digest);
    }

    private Customer customer(String id, boolean deleted) {
        Customer customer = new Customer();
        customer.setId(id);
        customer.setDeleted(deleted);
        return customer;
    }

    private Session session(String customerId, String url) {
        Session session = new Session();
        session.setCustomer(customerId);
        session.setUrl(url);
        return session;
    }

    private Session stripeCheckoutSession(String id, String customerId, String mode, String status, String url, String subscriptionId) {
        Session session = new Session();
        session.setId(id);
        session.setCustomer(customerId);
        session.setMode(mode);
        session.setStatus(status);
        session.setUrl(url);
        session.setSubscription(subscriptionId);
        return session;
    }

    private StripeCheckoutAttempt saveCheckoutAttempt(User user, String sessionId, String billingCycle, LocalDateTime expiresAt) {
        StripeCheckoutAttempt attempt = StripeCheckoutAttempt.pending(user.getId(), expiresAt);
        attempt.setStripeCheckoutSessionId(sessionId);
        attempt.setBillingCycle(billingCycle);
        return stripeCheckoutAttemptRepository.saveAndFlush(attempt);
    }

    private void assertCrossCycleCheckoutReplacement(String existingCycle, String requestedCycle, String requestedPriceId) throws Exception {
        User user = createUser("stripe-cross-cycle-" + existingCycle + "-to-" + requestedCycle + "@example.com");
        String existingSessionId = "cs_" + existingCycle + "_pending";
        saveCheckoutAttempt(user, existingSessionId, existingCycle, LocalDateTime.now().plusMinutes(1));
        when(stripeApiClient.retrieveCheckoutSession(existingSessionId))
                .thenReturn(stripeCheckoutSession(existingSessionId, "cus_existing", "subscription", "open", "https://checkout.test/old", null));
        when(stripeApiClient.createCustomer(any(User.class))).thenReturn(customer("cus_cross_cycle", false));
        when(stripeApiClient.createCheckoutSession(any(User.class), eq("cus_cross_cycle"), eq(requestedPriceId), eq("http://localhost:5173")))
                .thenReturn(stripeCheckoutSession("cs_" + requestedCycle + "_replacement", "cus_cross_cycle", "subscription", "open", "https://checkout.test/" + requestedCycle, null));

        assertThat(stripeBillingService.createCheckoutSession(user, requestedCycle)).isEqualTo("https://checkout.test/" + requestedCycle);

        verify(stripeApiClient).expireCheckoutSession(existingSessionId);
        verify(stripeApiClient).createCheckoutSession(any(User.class), eq("cus_cross_cycle"), eq(requestedPriceId), eq("http://localhost:5173"));
    }

    private Invoice invoice(String id, String customerId, String subscriptionId) {
        Invoice invoice = new Invoice();
        invoice.setId(id);
        invoice.setCustomer(customerId);
        invoice.setSubscription(subscriptionId);
        return invoice;
    }

    private Subscription subscription(String id, long currentPeriodStart, long currentPeriodEnd) {
        Subscription subscription = new Subscription();
        subscription.setId(id);
        subscription.setStatus("active");
        subscription.setCurrentPeriodStart(currentPeriodStart);
        subscription.setCurrentPeriodEnd(currentPeriodEnd);
        return subscription;
    }

    private Subscription subscription(String id, String customerId, String status) {
        Subscription subscription = subscription(id, 1_780_272_000L, 1_782_864_000L);
        subscription.setCustomer(customerId);
        subscription.setStatus(status);
        return subscription;
    }

    private void prepareValidSubscriptionCheckout(User user, String priceId) throws Exception {
        prepareValidSubscriptionCheckout(user, priceId, "active");
    }

    private void prepareValidSubscriptionCheckout(User user, String priceId, String status) throws Exception {
        user.setStripeCustomerId("cus_test_123");
        userRepository.save(user);
        when(stripeApiClient.retrieveSubscription("sub_test_123"))
                .thenReturn(subscriptionWithPrice("sub_test_123", "cus_test_123", status, priceId));
    }

    private Subscription subscriptionWithPrice(String id, String customerId, String status, String priceId) {
        Subscription subscription = subscription(id, customerId, status);
        Price price = new Price();
        price.setId(priceId);
        SubscriptionItem item = new SubscriptionItem();
        item.setPrice(price);
        SubscriptionItemCollection items = new SubscriptionItemCollection();
        items.setData(List.of(item));
        subscription.setItems(items);
        return subscription;
    }

    private InvalidRequestException noSuchCustomer(String customerId) {
        return new InvalidRequestException("No such customer: " + customerId, null, null, null, 404, null);
    }

    private InvalidRequestException noSuchCheckoutSession(String sessionId) {
        return new InvalidRequestException("No such checkout.session: " + sessionId, null, null, null, 404, null);
    }

    private LocalDateTime dateTime(long epochSeconds) {
        return LocalDateTime.ofInstant(Instant.ofEpochSecond(epochSeconds), ZoneOffset.UTC);
    }

    private String hex(byte[] bytes) {
        StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) {
            builder.append(String.format("%02x", value));
        }
        return builder.toString();
    }
}
