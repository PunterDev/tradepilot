package com.tradepilot.billing;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stripe.Stripe;
import com.stripe.exception.InvalidRequestException;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.Invoice;
import com.stripe.model.Subscription;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.tradepilot.entity.User;
import com.tradepilot.observability.ObservabilityService;
import com.tradepilot.repository.UserRepository;
import com.tradepilot.util.LogSanitizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class StripeBillingService {
    private final StripeProperties stripeProperties;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;
    private final StripeApiClient stripeApiClient;
    private final TransactionTemplate transactionTemplate;
    private final ObservabilityService observabilityService;
    private final ProcessedStripeEventRepository processedStripeEventRepository;
    private final AiCreditPurchaseRepository aiCreditPurchaseRepository;
    private final UsageLimitService usageLimitService;

    @Value("${app.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    public String createCheckoutSession(User user) throws StripeException {
        return createCheckoutSession(user, "monthly");
    }

    public String createCheckoutSession(User user, String billingCycle) throws StripeException {
        long start = System.currentTimeMillis();
        String priceId = checkoutPriceId(billingCycle);
        Stripe.apiKey = stripeProperties.getSecretKey();
        String normalizedBillingCycle = normalizeBillingCycle(billingCycle);
        log.info("Stripe checkout session creation started. userId={}, billingCycle={}", user.getId(), normalizedBillingCycle);

        try {
            String customerId = ensureStripeCustomer(user);
            Session session;
            try {
                session = stripeApiClient.createCheckoutSession(user, customerId, priceId, frontendUrl);
            } catch (InvalidRequestException ex) {
                if (!isNoSuchCustomer(ex)) {
                    throw ex;
                }
                clearStripeCustomer(user);
                customerId = createStripeCustomer(user);
                session = stripeApiClient.createCheckoutSession(user, customerId, priceId, frontendUrl);
            }

            if (StringUtils.hasText(session.getCustomer()) && !session.getCustomer().equals(user.getStripeCustomerId())) {
                user.setStripeCustomerId(session.getCustomer());
                transactionTemplate.executeWithoutResult(status -> userRepository.save(user));
            }
            observabilityService.recordStripeEvent("checkout.session.create", true, System.currentTimeMillis() - start, null);
            log.info("Stripe checkout session creation completed. userId={}, billingCycle={}, durationMs={}",
                    user.getId(), normalizedBillingCycle, System.currentTimeMillis() - start);
            return session.getUrl();
        } catch (RuntimeException | StripeException ex) {
            observabilityService.recordStripeEvent("checkout.session.create", false, System.currentTimeMillis() - start, LogSanitizer.summarizeError(ex.getMessage()));
            log.error("Stripe checkout session creation failed. userId={}, billingCycle={}, durationMs={}",
                    user.getId(), normalizedBillingCycle, System.currentTimeMillis() - start, ex);
            throw ex;
        }
    }

    public String createPortalSession(User user) throws StripeException {
        long start = System.currentTimeMillis();
        requireSecretKey();
        Stripe.apiKey = stripeProperties.getSecretKey();
        log.info("Stripe portal session creation started. userId={}", user.getId());
        try {
            String customerId = ensureStripeCustomer(user);
            try {
                String portalUrl = stripeApiClient.createPortalSession(customerId, frontendUrl);
                observabilityService.recordStripeEvent("billing.portal.create", true, System.currentTimeMillis() - start, null);
                log.info("Stripe portal session creation completed. userId={}, durationMs={}",
                        user.getId(), System.currentTimeMillis() - start);
                return portalUrl;
            } catch (InvalidRequestException ex) {
                if (!isNoSuchCustomer(ex)) {
                    throw ex;
                }
                clearStripeCustomer(user);
                String portalUrl = stripeApiClient.createPortalSession(createStripeCustomer(user), frontendUrl);
                observabilityService.recordStripeEvent("billing.portal.create", true, System.currentTimeMillis() - start, null);
                log.info("Stripe portal session creation completed after customer recovery. userId={}, durationMs={}",
                        user.getId(), System.currentTimeMillis() - start);
                return portalUrl;
            }
        } catch (RuntimeException | StripeException ex) {
            observabilityService.recordStripeEvent("billing.portal.create", false, System.currentTimeMillis() - start, LogSanitizer.summarizeError(ex.getMessage()));
            log.error("Stripe portal session creation failed. userId={}, durationMs={}",
                    user.getId(), System.currentTimeMillis() - start, ex);
            throw ex;
        }
    }

    public String createCreditPackCheckoutSession(User user, AiCreditPack pack) throws StripeException {
        long start = System.currentTimeMillis();
        requireSecretKey();
        if (user.getPlan() != PlanType.PRO) {
            throw new PlanLimitExceededException("Upgrade to Pro to buy AI credit packs.");
        }
        Stripe.apiKey = stripeProperties.getSecretKey();
        log.info("Stripe AI credit pack checkout creation started. userId={}, packId={}, credits={}",
                user.getId(), pack.name(), pack.credits());
        try {
            String customerId = ensureStripeCustomer(user);
            Session session;
            try {
                session = stripeApiClient.createCreditPackCheckoutSession(user, customerId, pack, frontendUrl);
            } catch (InvalidRequestException ex) {
                if (!isNoSuchCustomer(ex)) {
                    throw ex;
                }
                clearStripeCustomer(user);
                customerId = createStripeCustomer(user);
                session = stripeApiClient.createCreditPackCheckoutSession(user, customerId, pack, frontendUrl);
            }
            if (StringUtils.hasText(session.getCustomer()) && !session.getCustomer().equals(user.getStripeCustomerId())) {
                user.setStripeCustomerId(session.getCustomer());
                transactionTemplate.executeWithoutResult(status -> userRepository.save(user));
            }
            observabilityService.recordStripeEvent("checkout.session.create.credit_pack", true, System.currentTimeMillis() - start, null);
            return session.getUrl();
        } catch (RuntimeException | StripeException ex) {
            observabilityService.recordStripeEvent("checkout.session.create.credit_pack", false, System.currentTimeMillis() - start, LogSanitizer.summarizeError(ex.getMessage()));
            log.error("Stripe AI credit pack checkout creation failed. userId={}, packId={}, durationMs={}",
                    user.getId(), pack.name(), System.currentTimeMillis() - start, ex);
            throw ex;
        }
    }

    public void handleWebhook(String payload, String signatureHeader)
            throws SignatureVerificationException, IOException {
        long start = System.currentTimeMillis();
        if (!StringUtils.hasText(stripeProperties.getWebhookSecret())) {
            observabilityService.recordStripeEvent("webhook.unconfigured", false, System.currentTimeMillis() - start, "Stripe webhook secret missing");
            log.warn("Stripe webhook rejected because webhook secret is not configured.");
            throw new StripeNotConfiguredException();
        }

        try {
            Webhook.constructEvent(payload, signatureHeader, stripeProperties.getWebhookSecret());
            JsonNode root = objectMapper.readTree(payload);
            String type = root.path("type").asText();
            String eventId = root.path("id").asText(null);
            if (!StringUtils.hasText(eventId)) {
                log.warn("Stripe webhook rejected because event id is missing. type={}", type);
                throw new IOException("Stripe event id is missing");
            }
            log.info("Stripe webhook processing started. eventId={}, type={}", eventId, type);

            if (processedStripeEventRepository.existsByEventId(eventId)) {
                observabilityService.recordStripeEvent(type + ".duplicate", true, System.currentTimeMillis() - start, null);
                log.warn("Duplicate Stripe webhook ignored. eventId={}, type={}", eventId, type);
                return;
            }

            StripeWebhookCommand command = normalizeWebhookCommand(eventId, type, root.path("data").path("object"));

            transactionTemplate.executeWithoutResult(status -> {
                if (processedStripeEventRepository.existsByEventId(eventId)) {
                    log.warn("Duplicate Stripe webhook ignored inside transaction. eventId={}, type={}", eventId, type);
                    return;
                }
                processedStripeEventRepository.save(ProcessedStripeEvent.of(eventId, type));
                switch (command.type()) {
                    case "checkout.session.completed" -> handleCheckoutCompleted(command.object());
                    case "customer.subscription.updated" -> handleSubscriptionUpdated(command.object());
                    case "customer.subscription.deleted" -> handleSubscriptionDeleted(command.object());
                    case "invoice_payment.paid", "invoice.payment_succeeded" ->
                            handleInvoicePaymentSucceeded(command.eventId(), command.invoice(), command.invoiceSubscriptionPeriod());
                    case "invoice_payment.payment_failed", "invoice.payment_failed" ->
                            handleInvoicePaymentFailed(command.eventId(), command.invoice(), command.invoiceSubscriptionPeriod());
                    default -> {
                        // Ignore Stripe events this app does not need yet.
                        log.warn("Unsupported Stripe webhook event ignored. eventId={}, type={}", eventId, type);
                    }
                }
            });
            observabilityService.recordStripeEvent(type, true, System.currentTimeMillis() - start, null);
            log.info("Stripe webhook processing completed. eventId={}, type={}, durationMs={}",
                    eventId, type, System.currentTimeMillis() - start);
        } catch (SignatureVerificationException ex) {
            observabilityService.recordStripeEvent("webhook", false, System.currentTimeMillis() - start, "Invalid Stripe webhook signature");
            log.warn("Stripe webhook rejected due to invalid signature. durationMs={}", System.currentTimeMillis() - start);
            throw ex;
        } catch (IOException ex) {
            observabilityService.recordStripeEvent("webhook", false, System.currentTimeMillis() - start, LogSanitizer.summarizeError(ex.getMessage()));
            log.warn("Stripe webhook rejected due to invalid payload. durationMs={}, reason={}",
                    System.currentTimeMillis() - start, LogSanitizer.summarizeError(ex.getMessage()));
            throw ex;
        } catch (RuntimeException ex) {
            observabilityService.recordStripeEvent("webhook", false, System.currentTimeMillis() - start, LogSanitizer.summarizeError(ex.getMessage()));
            log.error("Stripe webhook processing failed. durationMs={}", System.currentTimeMillis() - start, ex);
            throw ex;
        }
    }

    private StripeWebhookCommand normalizeWebhookCommand(String eventId, String type, JsonNode object) throws IOException {
        return switch (type) {
            case "invoice_payment.paid", "invoice_payment.payment_failed" -> {
                JsonNode invoice = resolveInvoiceFromPaymentEvent(eventId, type, object);
                yield new StripeWebhookCommand(eventId, type, object, invoice, lookupInvoiceSubscriptionPeriod(invoice, eventId));
            }
            case "invoice.payment_succeeded", "invoice.payment_failed" ->
                    new StripeWebhookCommand(eventId, type, object, object, lookupInvoiceSubscriptionPeriod(object, eventId));
            default -> new StripeWebhookCommand(eventId, type, object, object, null);
        };
    }

    private JsonNode resolveInvoiceFromPaymentEvent(String eventId, String type, JsonNode object) throws IOException {
        JsonNode invoice = invoiceFromPaymentEvent(object);
        if (invoice.isTextual()) {
            String invoiceId = invoice.asText(null);
            if (!StringUtils.hasText(invoiceId)) {
                log.warn("Stripe invoice payment event had a blank invoice id. eventId={}, type={}", eventId, type);
                return invoice;
            }
            try {
                Invoice invoiceModel = stripeApiClient.retrieveInvoice(invoiceId);
                return objectMapper.valueToTree(invoiceModel);
            } catch (StripeException ex) {
                log.warn("Stripe invoice lookup failed for payment event. eventId={}, type={}, invoiceId={}, reason={}",
                        eventId, type, invoiceId, LogSanitizer.summarizeError(ex.getMessage()));
                throw new IOException("Stripe invoice lookup failed", ex);
            }
        }
        return invoice;
    }

    private String ensureStripeCustomer(User user) throws StripeException {
        if (StringUtils.hasText(user.getStripeCustomerId())) {
            try {
                Customer customer = stripeApiClient.retrieveCustomer(user.getStripeCustomerId());
                if (customer != null && !Boolean.TRUE.equals(customer.getDeleted())) {
                    log.trace("Stored Stripe customer verified. userId={}", user.getId());
                    return customer.getId();
                }
            } catch (InvalidRequestException ex) {
                if (!isNoSuchCustomer(ex)) {
                    throw ex;
                }
                log.warn("Stored Stripe customer missing; recreating. userId={}", user.getId());
            }
            clearStripeCustomer(user);
        }

        return createStripeCustomer(user);
    }

    private String createStripeCustomer(User user) throws StripeException {
        Customer customer = stripeApiClient.createCustomer(user);
        user.setStripeCustomerId(customer.getId());
        transactionTemplate.executeWithoutResult(status -> userRepository.save(user));
        log.info("Stripe customer created. userId={}", user.getId());
        return customer.getId();
    }

    private void clearStripeCustomer(User user) {
        user.setStripeCustomerId(null);
        transactionTemplate.executeWithoutResult(status -> userRepository.save(user));
        log.warn("Stored Stripe customer cleared. userId={}", user.getId());
    }

    private boolean isNoSuchCustomer(InvalidRequestException ex) {
        return ex.getMessage() != null && ex.getMessage().contains("No such customer");
    }

    void handleCheckoutCompleted(JsonNode object) {
        if ("AI_CREDIT_PACK".equals(object.path("metadata").path("type").asText())) {
            handleCreditPackCheckoutCompleted(object);
            return;
        }

        Long userId = object.path("metadata").path("userId").asLong(0);
        if (userId == 0) {
            log.warn("Stripe checkout completed without userId metadata.");
            return;
        }

        userRepository.findById(userId).ifPresent(user -> {
            JsonNode subscription = object.path("subscription");
            user.setPlan(PlanType.PRO);
            user.setSubscriptionStatus(SubscriptionStatus.ACTIVE);
            applyStripeIds(user, object.path("customer").asText(null), subscriptionIdFromValue(subscription));
            if (subscription.isObject()) {
                logSubscriptionFields("checkout.session.completed", subscription);
                applySubscriptionPeriod(user, subscription);
                applySubscriptionCancellationMetadata(user, subscription);
            } else {
                applySubscriptionPeriod(user, object);
            }
            userRepository.save(user);
            usageLimitService.invalidateUsageCache(user.getId());
            log.info("Stripe checkout completed upgraded user. userId={}", user.getId());
        });
    }

    void handleCreditPackCheckoutCompleted(JsonNode object) {
        String sessionId = object.path("id").asText(null);
        if (!StringUtils.hasText(sessionId)) {
            log.warn("Stripe AI credit pack checkout completed without session id.");
            return;
        }
        if (aiCreditPurchaseRepository.existsByStripeCheckoutSessionId(sessionId)) {
            log.info("Stripe AI credit pack checkout already applied. sessionId={}", sessionId);
            return;
        }

        JsonNode metadata = object.path("metadata");
        Long userId = metadata.path("userId").asLong(0);
        String packId = metadata.path("packId").asText(null);
        if (userId == 0 || !StringUtils.hasText(packId)) {
            log.warn("Stripe AI credit pack checkout missing metadata. sessionId={}, userId={}, packId={}",
                    sessionId, userId, packId);
            return;
        }

        AiCreditPack pack;
        try {
            pack = AiCreditPack.valueOf(packId);
        } catch (IllegalArgumentException ex) {
            log.warn("Stripe AI credit pack checkout had unsupported pack id. sessionId={}, packId={}", sessionId, packId);
            return;
        }

        userRepository.findByIdForUpdate(userId).ifPresentOrElse(user -> {
            int currentBalance = user.getPurchasedAiCreditsBalance() == null ? 0 : Math.max(0, user.getPurchasedAiCreditsBalance());
            user.setPurchasedAiCreditsBalance(currentBalance + pack.credits());
            userRepository.save(user);

            AiCreditPurchase purchase = AiCreditPurchase.builder()
                    .userId(user.getId())
                    .stripeCheckoutSessionId(sessionId)
                    .stripePaymentIntentId(textField(object, "payment_intent"))
                    .creditsPurchased(pack.credits())
                    .amountCents((int) object.path("amount_total").asLong(pack.amountCents()))
                    .currency(object.path("currency").asText("usd"))
                    .status(AiCreditPurchaseStatus.COMPLETED)
                    .completedAt(LocalDateTime.now())
                    .build();
            aiCreditPurchaseRepository.save(purchase);
            usageLimitService.invalidateUsageCache(user.getId());
            log.info("Stripe AI credit pack applied. userId={}, sessionId={}, packId={}, credits={}, newBalance={}",
                    user.getId(), sessionId, pack.name(), pack.credits(), user.getPurchasedAiCreditsBalance());
        }, () -> log.warn("Stripe AI credit pack checkout could not be mapped to user. sessionId={}, userId={}",
                sessionId, userId));
    }

    void handleSubscriptionUpdated(JsonNode object) {
        logSubscriptionFields("customer.subscription.updated", object);
        findUserForSubscription(object).ifPresent(user -> {
            String status = object.path("status").asText("");
            boolean cancelAtPeriodEnd = object.path("cancel_at_period_end").asBoolean(false);
            SubscriptionStatus subscriptionStatus = mapSubscriptionStatus(status, cancelAtPeriodEnd);
            user.setSubscriptionStatus(subscriptionStatus);
            user.setPlan(subscriptionStatus == SubscriptionStatus.ACTIVE || subscriptionStatus == SubscriptionStatus.TRIALING
                    ? PlanType.PRO
                    : PlanType.FREE);
            applyStripeIds(user, object.path("customer").asText(null), object.path("id").asText(null));
            applySubscriptionPeriod(user, object);
            applySubscriptionCancellationMetadata(user, object);
            userRepository.save(user);
            usageLimitService.invalidateUsageCache(user.getId());
            log.info("Stripe subscription updated. userId={}, status={}, plan={}, cancelAtPeriodEnd={}",
                    user.getId(), subscriptionStatus, user.getPlan(), cancelAtPeriodEnd);
        });
    }

    void handleSubscriptionDeleted(JsonNode object) {
        logSubscriptionFields("customer.subscription.deleted", object);
        findUserForSubscription(object).ifPresent(user -> {
            user.setPlan(PlanType.FREE);
            user.setSubscriptionStatus(SubscriptionStatus.CANCELED);
            applyStripeIds(user, object.path("customer").asText(null), object.path("id").asText(null));
            applySubscriptionPeriod(user, object);
            applySubscriptionCancellationMetadata(user, object);
            userRepository.save(user);
            usageLimitService.invalidateUsageCache(user.getId());
            log.info("Stripe subscription deleted. userId={}", user.getId());
        });
    }

    void handleInvoicePaymentSucceeded(String eventId, JsonNode invoice, Period invoiceSubscriptionPeriod) {
        if (invoice == null || invoice.isMissingNode() || invoice.isNull() || invoice.isTextual()) {
            log.warn("Stripe paid invoice event did not include supported invoice details. eventId={}, invoiceId={}",
                    eventId, invoice == null ? null : invoice.asText(null));
            return;
        }

        findUserForInvoice(invoice).ifPresentOrElse(user -> {
            user.setPlan(PlanType.PRO);
            user.setSubscriptionStatus(SubscriptionStatus.ACTIVE);
            applyStripeIds(user, textField(invoice, "customer"), subscriptionIdFromInvoice(invoice));
            applyInvoicePeriod(user, invoice, eventId, invoiceSubscriptionPeriod);
            clearSubscriptionCancellationMetadata(user);
            userRepository.save(user);
            usageLimitService.invalidateUsageCache(user.getId());
            log.info("Stripe invoice payment confirmed active subscription. userId={}", user.getId());
        }, () -> log.warn("Stripe paid invoice could not be mapped to a TradePilot user. invoiceId={}",
                textField(invoice, "id")));
    }

    void handleInvoicePaymentFailed(String eventId, JsonNode invoice, Period invoiceSubscriptionPeriod) {
        if (invoice.isMissingNode() || invoice.isNull() || invoice.isTextual()) {
            log.warn("Stripe invoice payment failed event did not include expanded invoice details. eventId={}, invoiceId={}",
                    eventId, invoice.asText(null));
            return;
        }

        findUserForInvoice(invoice).ifPresentOrElse(user -> {
            user.setPlan(PlanType.FREE);
            user.setSubscriptionStatus(SubscriptionStatus.PAST_DUE);
            applyStripeIds(user, textField(invoice, "customer"), subscriptionIdFromInvoice(invoice));
            applyInvoicePeriod(user, invoice, eventId, invoiceSubscriptionPeriod);
            userRepository.save(user);
            usageLimitService.invalidateUsageCache(user.getId());
            log.warn("Stripe invoice payment failed; subscription marked past due. userId={}", user.getId());
        }, () -> log.warn("Stripe failed invoice could not be mapped to a TradePilot user. invoiceId={}",
                textField(invoice, "id")));
    }

    private Optional<User> findUserForSubscription(JsonNode object) {
        String subscriptionId = object.path("id").asText(null);
        if (StringUtils.hasText(subscriptionId)) {
            Optional<User> bySubscription = userRepository.findByStripeSubscriptionId(subscriptionId);
            if (bySubscription.isPresent()) {
                return bySubscription;
            }
        }

        String customerId = object.path("customer").asText(null);
        if (StringUtils.hasText(customerId)) {
            return userRepository.findByStripeCustomerId(customerId);
        }
        return Optional.empty();
    }

    private Optional<User> findUserForInvoice(JsonNode invoice) {
        String subscriptionId = subscriptionIdFromInvoice(invoice);
        if (StringUtils.hasText(subscriptionId)) {
            Optional<User> bySubscription = userRepository.findByStripeSubscriptionId(subscriptionId);
            if (bySubscription.isPresent()) {
                return bySubscription;
            }
        }

        String customerId = textField(invoice, "customer");
        if (StringUtils.hasText(customerId)) {
            return userRepository.findByStripeCustomerId(customerId);
        }
        return Optional.empty();
    }

    private void applyStripeIds(User user, String customerId, String subscriptionId) {
        if (StringUtils.hasText(customerId)) {
            user.setStripeCustomerId(customerId);
        }
        if (StringUtils.hasText(subscriptionId)) {
            user.setStripeSubscriptionId(subscriptionId);
        }
    }

    private void applySubscriptionPeriod(User user, JsonNode subscription) {
        long start = subscriptionPeriodStart(subscription);
        long end = subscriptionPeriodEnd(subscription);
        setPeriodStart(user, start);
        setPeriodEnd(user, end);
    }

    private void applyInvoicePeriod(User user, JsonNode invoice, String eventId, Period invoiceSubscriptionPeriod) {
        Optional<Period> period = Optional.ofNullable(invoiceSubscriptionPeriod)
                .filter(value -> isValidPeriod(value.start(), value.end()))
                .or(() -> periodFromInvoiceLines(invoice));
        if (period.isEmpty()) {
            log.warn("Stripe invoice period not updated because no valid subscription or invoice line period was available. eventId={}, invoiceId={}",
                    eventId, textField(invoice, "id"));
            return;
        }
        if (!isValidPeriod(period.get().start(), period.get().end())) {
            log.warn("Stripe invoice period not updated because extracted period is invalid. eventId={}, invoiceId={}, start={}, end={}",
                    eventId, textField(invoice, "id"), period.get().start(), period.get().end());
            return;
        }
        setPeriodStart(user, period.get().start());
        setPeriodEnd(user, period.get().end());
    }

    private void applySubscriptionCancellationMetadata(User user, JsonNode subscription) {
        boolean cancelAtPeriodEnd = subscription.path("cancel_at_period_end").asBoolean(false);
        user.setStripeCancelAtPeriodEnd(cancelAtPeriodEnd);
        user.setStripeCancelAt(epochToLocalDateTime(subscription.path("cancel_at").asLong(0)));
        user.setStripeCanceledAt(epochToLocalDateTime(subscription.path("canceled_at").asLong(0)));
    }

    private void clearSubscriptionCancellationMetadata(User user) {
        user.setStripeCancelAtPeriodEnd(false);
        user.setStripeCancelAt(null);
        user.setStripeCanceledAt(null);
    }

    private Period lookupInvoiceSubscriptionPeriod(JsonNode invoice, String eventId) {
        if (invoice == null || invoice.isMissingNode() || invoice.isNull() || invoice.isTextual()) {
            return null;
        }
        String subscriptionId = subscriptionIdFromInvoice(invoice);
        if (!StringUtils.hasText(subscriptionId)) {
            return null;
        }
        try {
            Subscription subscription = stripeApiClient.retrieveSubscription(subscriptionId);
            if (subscription == null) {
                log.warn("Stripe subscription period lookup returned no subscription. eventId={}, invoiceId={}, subscriptionId={}",
                        eventId, textField(invoice, "id"), subscriptionId);
                return null;
            }
            Period period = new Period(
                    firstPositiveNullable(subscription.getCurrentPeriodStart(), subscriptionPeriodStart(invoice.path("subscription"))),
                    firstPositiveNullable(subscription.getCurrentPeriodEnd(), subscriptionPeriodEnd(invoice.path("subscription")))
            );
            if (isValidPeriod(period.start(), period.end())) {
                return period;
            }
            log.warn("Stripe subscription period from retrieved subscription is invalid. eventId={}, invoiceId={}, subscriptionId={}, start={}, end={}",
                    eventId, textField(invoice, "id"), subscriptionId, period.start(), period.end());
        } catch (StripeException ex) {
            log.warn("Stripe subscription period lookup failed for invoice. eventId={}, invoiceId={}, subscriptionId={}, reason={}",
                    eventId, textField(invoice, "id"), subscriptionId, LogSanitizer.summarizeError(ex.getMessage()));
        }
        return null;
    }

    private Optional<Period> periodFromInvoiceLines(JsonNode invoice) {
        JsonNode linePeriod = invoice.path("lines").path("data").path(0).path("period");
        long start = linePeriod.path("start").asLong(0);
        long end = linePeriod.path("end").asLong(0);
        return isValidPeriod(start, end) ? Optional.of(new Period(start, end)) : Optional.empty();
    }

    private JsonNode invoiceFromPaymentEvent(JsonNode object) {
        JsonNode invoice = object.path("invoice");
        if (!invoice.isMissingNode() && !invoice.isNull()) {
            return invoice;
        }
        return object;
    }

    private String subscriptionIdFromInvoice(JsonNode invoice) {
        String subscription = textField(invoice, "subscription");
        if (StringUtils.hasText(subscription)) {
            return subscription;
        }
        return textField(invoice.path("parent").path("subscription_details"), "subscription");
    }

    private String subscriptionIdFromValue(JsonNode subscription) {
        if (subscription.isObject()) {
            return textField(subscription, "id");
        }
        return subscription.asText(null);
    }

    private long subscriptionPeriodStart(JsonNode subscription) {
        return firstPositive(
                subscription.path("current_period_start").asLong(0),
                subscription.path("items").path("data").path(0).path("current_period_start").asLong(0)
        );
    }

    private long subscriptionPeriodEnd(JsonNode subscription) {
        return firstPositive(
                subscription.path("current_period_end").asLong(0),
                subscription.path("items").path("data").path(0).path("current_period_end").asLong(0),
                subscription.path("cancel_at").asLong(0)
        );
    }

    private String textField(JsonNode object, String fieldName) {
        JsonNode value = object.path(fieldName);
        if (value.isTextual()) {
            return value.asText(null);
        }
        if (value.isObject()) {
            return value.path("id").asText(null);
        }
        return null;
    }

    private long firstPositive(long... values) {
        for (long value : values) {
            if (value > 0) {
                return value;
            }
        }
        return 0;
    }

    private long firstPositiveNullable(Long value, long fallback) {
        return value != null && value > 0 ? value : fallback;
    }

    private boolean isValidPeriod(long start, long end) {
        return start > 0 && end > start;
    }

    private LocalDateTime epochToLocalDateTime(long epochSeconds) {
        return epochSeconds > 0 ? LocalDateTime.ofInstant(Instant.ofEpochSecond(epochSeconds), ZoneOffset.UTC) : null;
    }

    private void setPeriodStart(User user, long epochSeconds) {
        if (epochSeconds > 0) {
            user.setCurrentPeriodStart(epochToLocalDateTime(epochSeconds));
        }
    }

    private void setPeriodEnd(User user, long epochSeconds) {
        if (epochSeconds > 0) {
            user.setCurrentPeriodEnd(epochToLocalDateTime(epochSeconds));
        }
    }

    private SubscriptionStatus mapSubscriptionStatus(String stripeStatus, boolean cancelAtPeriodEnd) {
        if (cancelAtPeriodEnd && ("active".equals(stripeStatus) || "trialing".equals(stripeStatus))) {
            return "trialing".equals(stripeStatus) ? SubscriptionStatus.TRIALING : SubscriptionStatus.ACTIVE;
        }
        return switch (stripeStatus) {
            case "active" -> SubscriptionStatus.ACTIVE;
            case "trialing" -> SubscriptionStatus.TRIALING;
            case "past_due", "unpaid" -> SubscriptionStatus.PAST_DUE;
            case "canceled" -> SubscriptionStatus.CANCELED;
            default -> SubscriptionStatus.NONE;
        };
    }

    private void logSubscriptionFields(String eventType, JsonNode subscription) {
        log.info("Stripe subscription fields received. eventType={}, subscriptionId={}, customerId={}, status={}, currentPeriodStart={}, currentPeriodEnd={}, itemCurrentPeriodStart={}, itemCurrentPeriodEnd={}, cancelAtPeriodEnd={}, cancelAt={}, canceledAt={}, metadataKeys={}",
                eventType,
                textField(subscription, "id"),
                textField(subscription, "customer"),
                subscription.path("status").asText(null),
                subscription.path("current_period_start").asLong(0),
                subscription.path("current_period_end").asLong(0),
                subscription.path("items").path("data").path(0).path("current_period_start").asLong(0),
                subscription.path("items").path("data").path(0).path("current_period_end").asLong(0),
                subscription.path("cancel_at_period_end").asBoolean(false),
                subscription.path("cancel_at").asLong(0),
                subscription.path("canceled_at").asLong(0),
                metadataKeys(subscription.path("metadata")));
    }

    private String metadataKeys(JsonNode metadata) {
        if (metadata == null || !metadata.isObject()) {
            return "[]";
        }
        StringBuilder keys = new StringBuilder("[");
        metadata.fieldNames().forEachRemaining(field -> {
            if (keys.length() > 1) {
                keys.append(", ");
            }
            keys.append(field);
        });
        keys.append("]");
        return keys.toString();
    }

    private record Period(long start, long end) {
    }

    private record StripeWebhookCommand(
            String eventId,
            String type,
            JsonNode object,
            JsonNode invoice,
            Period invoiceSubscriptionPeriod
    ) {
    }

    private void requireCheckoutConfig() {
        requireSecretKey();
        if (!StringUtils.hasText(stripeProperties.getProPriceId())) {
            throw new StripeNotConfiguredException();
        }
    }

    private String checkoutPriceId(String billingCycle) {
        requireSecretKey();
        String normalized = normalizeBillingCycle(billingCycle);
        String priceId = "yearly".equals(normalized)
                ? stripeProperties.getProYearlyPriceId()
                : stripeProperties.getProPriceId();
        if (!StringUtils.hasText(priceId)) {
            throw new StripeNotConfiguredException();
        }
        return priceId;
    }

    private String normalizeBillingCycle(String billingCycle) {
        return "yearly".equalsIgnoreCase(billingCycle) ? "yearly" : "monthly";
    }

    private void requireSecretKey() {
        if (!StringUtils.hasText(stripeProperties.getSecretKey())) {
            throw new StripeNotConfiguredException();
        }
    }
}
