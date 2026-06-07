package com.tradepilot;

import com.tradepilot.billing.PlanType;
import com.tradepilot.billing.ProcessedStripeEventRepository;
import com.tradepilot.billing.StripeApiClient;
import com.tradepilot.billing.StripeBillingService;
import com.tradepilot.billing.SubscriptionStatus;
import com.stripe.exception.InvalidRequestException;
import com.stripe.model.Customer;
import com.stripe.model.Invoice;
import com.stripe.model.Subscription;
import com.stripe.model.checkout.Session;
import com.tradepilot.dto.admin.AdminPlanChangeRequest;
import com.tradepilot.dto.account.TradingAccountRequest;
import com.tradepilot.dto.account.TradingAccountResponse;
import com.tradepilot.entity.User;
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
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
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
    }

    @Test
    void duplicateWebhookEventIsIgnored() throws Exception {
        User user = createUser("stripe-duplicate@example.com");
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
        when(stripeApiClient.createCustomer(user)).thenReturn(customer("cus_new_missing", false));
        when(stripeApiClient.createCheckoutSession(user, "cus_new_missing", "price_test_pro", "http://localhost:5173"))
                .thenReturn(session("cus_new_missing", "https://checkout.test/missing"));

        String checkoutUrl = stripeBillingService.createCheckoutSession(user);

        User updated = userRepository.findById(user.getId()).orElseThrow();
        assertThat(checkoutUrl).isEqualTo("https://checkout.test/missing");
        assertThat(updated.getStripeCustomerId()).isEqualTo("cus_new_missing");
        verify(stripeApiClient, never()).retrieveCustomer(anyString());
    }

    @Test
    void yearlyCheckoutUsesYearlyPriceId() throws Exception {
        User user = createUser("stripe-yearly-checkout@example.com");
        when(stripeApiClient.createCustomer(user)).thenReturn(customer("cus_yearly", false));
        when(stripeApiClient.createCheckoutSession(user, "cus_yearly", "price_test_pro_yearly", "http://localhost:5173"))
                .thenReturn(session("cus_yearly", "https://checkout.test/yearly"));

        String checkoutUrl = stripeBillingService.createCheckoutSession(user, "yearly");

        assertThat(checkoutUrl).isEqualTo("https://checkout.test/yearly");
        verify(stripeApiClient).createCheckoutSession(user, "cus_yearly", "price_test_pro_yearly", "http://localhost:5173");
        verify(stripeApiClient, never()).createCheckoutSession(user, "cus_yearly", "price_test_pro", "http://localhost:5173");
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

    private TradingAccountResponse createAccount(User user, String name) {
        return tradingAccountService.create(
                user,
                new TradingAccountRequest(
                        name,
                        PropFirmType.TOPSTEP,
                        null,
                        null,
                        BigDecimal.valueOf(50_000),
                        TradingAccountStatus.ACTIVE
                )
        );
    }

    private String checkoutCompletedPayload(Long userId) {
        return """
                {
                  "id": "evt_checkout_%s",
                  "type": "checkout.session.completed",
                  "data": {
                    "object": {
                      "id": "cs_test_123",
                      "object": "checkout.session",
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

    private String subscriptionDeletedPayload() {
        return subscriptionDeletedPayload("evt_deleted", "cus_delete_123", "sub_delete_123");
    }

    private String subscriptionDeletedPayload(String eventId, String customerId, String subscriptionId) {
        return """
                {
                  "id": "%s",
                  "type": "customer.subscription.deleted",
                  "data": {
                    "object": {
                      "id": "%s",
                      "object": "subscription",
                      "customer": "%s",
                      "status": "canceled"
                    }
                  }
                }
                """.formatted(eventId, subscriptionId, customerId);
    }

    private String subscriptionUpdatedPayload() {
        return """
                {
                  "id": "evt_subscription_updated",
                  "type": "customer.subscription.updated",
                  "data": {
                    "object": {
                      "id": "sub_update_123",
                      "object": "subscription",
                      "customer": "cus_update_123",
                      "status": "active",
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
                """;
    }

    private String subscriptionCancelAtPeriodEndPayload() {
        return """
                {
                  "id": "evt_subscription_cancel_at_period_end",
                  "type": "customer.subscription.updated",
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
        subscription.setCurrentPeriodStart(currentPeriodStart);
        subscription.setCurrentPeriodEnd(currentPeriodEnd);
        return subscription;
    }

    private InvalidRequestException noSuchCustomer(String customerId) {
        return new InvalidRequestException("No such customer: " + customerId, null, null, null, 404, null);
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
