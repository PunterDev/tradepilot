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
import com.tradepilot.featureusage.FeatureKey;
import com.tradepilot.featureusage.FeatureUsageAction;
import com.tradepilot.featureusage.FeatureUsageEventType;
import com.tradepilot.featureusage.FeatureUsageService;
import com.tradepilot.observability.ObservabilityService;
import com.tradepilot.repository.UserRepository;
import com.tradepilot.seoanalytics.SeoAnalyticsService;
import com.tradepilot.util.LogSanitizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Service
public class StripeBillingService {
    @java.lang.SuppressWarnings("all")
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(StripeBillingService.class);
    private final StripeProperties stripeProperties;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;
    private final StripeApiClient stripeApiClient;
    private final Environment environment;
    private final TransactionTemplate transactionTemplate;
    private final ObservabilityService observabilityService;
    private final ProcessedStripeEventRepository processedStripeEventRepository;
    private final StripeSubscriptionStateRepository stripeSubscriptionStateRepository;
    private final StripeCheckoutAttemptRepository stripeCheckoutAttemptRepository;
    private final AiCreditPurchaseRepository aiCreditPurchaseRepository;
    private final UsageLimitService usageLimitService;
    private final SeoAnalyticsService seoAnalyticsService;
    private final FeatureUsageService featureUsageService;
    private static final Duration SUBSCRIPTION_CHECKOUT_CREATION_LOCK_TTL = Duration.ofMinutes(2);
    private Runnable afterCreditPackFulfillmentClaimed = () -> {
    };
    private Runnable afterCreditPackCreditsIncremented = () -> {
    };
    @Value("${app.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    @Transactional(rollbackFor = StripeException.class, noRollbackFor = PlanLimitExceededException.class)
    public String createCheckoutSession(User user) throws StripeException {
        return createCheckoutSession(user, "monthly");
    }

    @Transactional(rollbackFor = StripeException.class, noRollbackFor = PlanLimitExceededException.class)
    public String createCheckoutSession(User user, String billingCycle) throws StripeException {
        long start = System.currentTimeMillis();
        String priceId = checkoutPriceId(billingCycle);
        Stripe.apiKey = stripeProperties.getSecretKey();
        String normalizedBillingCycle = normalizeBillingCycle(billingCycle);
        log.info("Stripe checkout session creation started. userId={}, billingCycle={}", user.getId(), normalizedBillingCycle);
        try {
            User lockedUser = userRepository.findByIdForUpdate(user.getId()).orElse(user);
            preventDuplicateSubscriptionCheckout(lockedUser);
            Optional<String> reusableCheckoutUrl = reconcileExistingSubscriptionCheckoutAttempt(lockedUser, normalizedBillingCycle);
            if (reusableCheckoutUrl.isPresent()) {
                observabilityService.recordStripeEvent("checkout.session.reuse", true, System.currentTimeMillis() - start, null);
                return reusableCheckoutUrl.get();
            }
            StripeCheckoutAttempt checkoutAttempt = claimSubscriptionCheckoutAttempt(lockedUser.getId(), normalizedBillingCycle);
            String customerId = ensureStripeCustomer(lockedUser);
            Session session;
            try {
                session = stripeApiClient.createCheckoutSession(lockedUser, customerId, priceId, frontendUrl);
            } catch (InvalidRequestException ex) {
                if (!isNoSuchCustomer(ex)) {
                    throw ex;
                }
                clearStripeCustomer(lockedUser);
                customerId = createStripeCustomer(lockedUser);
                session = stripeApiClient.createCheckoutSession(lockedUser, customerId, priceId, frontendUrl);
            }
            if (StringUtils.hasText(session.getCustomer()) && !session.getCustomer().equals(lockedUser.getStripeCustomerId())) {
                lockedUser.setStripeCustomerId(session.getCustomer());
                userRepository.save(lockedUser);
            }
            checkoutAttempt.setStripeCheckoutSessionId(session.getId());
            checkoutAttempt.setStatus(StripeCheckoutAttemptStatus.PENDING);
            checkoutAttempt.setExpiresAt(LocalDateTime.now().plus(SUBSCRIPTION_CHECKOUT_CREATION_LOCK_TTL));
            stripeCheckoutAttemptRepository.save(checkoutAttempt);
            trackSubscriptionCheckoutStarted(lockedUser, session.getId(), normalizedBillingCycle);
            observabilityService.recordStripeEvent("checkout.session.create", true, System.currentTimeMillis() - start, null);
            log.info("Stripe checkout session creation completed. userId={}, billingCycle={}, durationMs={}", lockedUser.getId(), normalizedBillingCycle, System.currentTimeMillis() - start);
            return session.getUrl();
        } catch (RuntimeException | StripeException ex) {
            observabilityService.recordStripeEvent("checkout.session.create", false, System.currentTimeMillis() - start, LogSanitizer.summarizeError(ex.getMessage()));
            log.error("Stripe checkout session creation failed. userId={}, billingCycle={}, durationMs={}", user.getId(), normalizedBillingCycle, System.currentTimeMillis() - start, ex);
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
                log.info("Stripe portal session creation completed. userId={}, durationMs={}", user.getId(), System.currentTimeMillis() - start);
                return portalUrl;
            } catch (InvalidRequestException ex) {
                if (!isNoSuchCustomer(ex)) {
                    throw ex;
                }
                clearStripeCustomer(user);
                String portalUrl = stripeApiClient.createPortalSession(createStripeCustomer(user), frontendUrl);
                observabilityService.recordStripeEvent("billing.portal.create", true, System.currentTimeMillis() - start, null);
                log.info("Stripe portal session creation completed after customer recovery. userId={}, durationMs={}", user.getId(), System.currentTimeMillis() - start);
                return portalUrl;
            }
        } catch (RuntimeException | StripeException ex) {
            observabilityService.recordStripeEvent("billing.portal.create", false, System.currentTimeMillis() - start, LogSanitizer.summarizeError(ex.getMessage()));
            log.error("Stripe portal session creation failed. userId={}, durationMs={}", user.getId(), System.currentTimeMillis() - start, ex);
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
        log.info("Stripe AI credit pack checkout creation started. userId={}, packId={}, credits={}", user.getId(), pack.name(), pack.credits());
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
            trackCreditPackCheckoutSelected(user, session.getId(), pack);
            observabilityService.recordStripeEvent("checkout.session.create.credit_pack", true, System.currentTimeMillis() - start, null);
            return session.getUrl();
        } catch (RuntimeException | StripeException ex) {
            observabilityService.recordStripeEvent("checkout.session.create.credit_pack", false, System.currentTimeMillis() - start, LogSanitizer.summarizeError(ex.getMessage()));
            log.error("Stripe AI credit pack checkout creation failed. userId={}, packId={}, durationMs={}", user.getId(), pack.name(), System.currentTimeMillis() - start, ex);
            throw ex;
        }
    }

    public void handleWebhook(String payload, String signatureHeader) throws SignatureVerificationException, IOException {
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
            LocalDateTime stripeEventCreated = stripeEventCreated(root);
            if (!StringUtils.hasText(eventId)) {
                log.warn("Stripe webhook rejected because event id is missing. type={}", type);
                throw new IOException("Stripe event id is missing");
            }
            if (!isWebhookLiveModeAllowed(root)) {
                log.warn("Stripe webhook rejected because production received a test-mode event. eventId={}, type={}", eventId, type);
                throw new IOException("Production Stripe webhook events must be live mode");
            }
            log.info("Stripe webhook processing started. eventId={}, type={}", eventId, type);
            if (processedStripeEventRepository.existsByEventId(eventId)) {
                observabilityService.recordStripeEvent(type + ".duplicate", true, System.currentTimeMillis() - start, null);
                log.warn("Duplicate Stripe webhook ignored. eventId={}, type={}", eventId, type);
                return;
            }
            StripeWebhookCommand command = normalizeWebhookCommand(eventId, type, root.path("data").path("object"));
            try {
                transactionTemplate.executeWithoutResult(status -> {
                    if (processedStripeEventRepository.existsByEventId(eventId)) {
                        log.warn("Duplicate Stripe webhook ignored inside transaction. eventId={}, type={}", eventId, type);
                        return;
                    }
                    processedStripeEventRepository.saveAndFlush(ProcessedStripeEvent.of(eventId, type, stripeEventCreated));
                    if (!claimSubscriptionStateEvent(command, stripeEventCreated)) {
                        return;
                    }
                    switch (command.type()) {
                        case "checkout.session.completed" -> handleCheckoutCompleted(command.object());
                        case "customer.subscription.updated" -> handleSubscriptionUpdated(command.object(), command.currentSubscription());
                        case "customer.subscription.deleted" -> handleSubscriptionDeleted(command.object(), command.currentSubscription());
                        case "invoice_payment.paid", "invoice.payment_succeeded" -> handleInvoicePaymentSucceeded(command.eventId(), command.invoice(), command.invoiceSubscriptionPeriod(), command.currentSubscription());
                        case "invoice_payment.payment_failed", "invoice.payment_failed" -> handleInvoicePaymentFailed(command.eventId(), command.invoice(), command.invoiceSubscriptionPeriod(), command.currentSubscription());
                        case "charge.refunded" -> handleChargeRefunded(command.object());
                        case "charge.dispute.created", "charge.dispute.closed" -> handleChargeDispute(command.object());
                        default -> {
                            // Ignore Stripe events this app does not need yet.
                            log.warn("Unsupported Stripe webhook event ignored. eventId={}, type={}", eventId, type);
                        }
                    }
                });
            } catch (DataIntegrityViolationException ex) {
                if (processedStripeEventRepository.existsByEventId(eventId)) {
                    observabilityService.recordStripeEvent(type + ".duplicate", true, System.currentTimeMillis() - start, null);
                    log.warn("Concurrent duplicate Stripe webhook ignored. eventId={}, type={}", eventId, type);
                    return;
                }
                throw ex;
            }
            observabilityService.recordStripeEvent(type, true, System.currentTimeMillis() - start, null);
            log.info("Stripe webhook processing completed. eventId={}, type={}, durationMs={}", eventId, type, System.currentTimeMillis() - start);
        } catch (SignatureVerificationException ex) {
            observabilityService.recordStripeEvent("webhook", false, System.currentTimeMillis() - start, "Invalid Stripe webhook signature");
            log.warn("Stripe webhook rejected due to invalid signature. durationMs={}", System.currentTimeMillis() - start);
            throw ex;
        } catch (IOException ex) {
            observabilityService.recordStripeEvent("webhook", false, System.currentTimeMillis() - start, LogSanitizer.summarizeError(ex.getMessage()));
            log.warn("Stripe webhook rejected due to invalid payload. durationMs={}, reason={}", System.currentTimeMillis() - start, LogSanitizer.summarizeError(ex.getMessage()));
            throw ex;
        } catch (RuntimeException ex) {
            observabilityService.recordStripeEvent("webhook", false, System.currentTimeMillis() - start, LogSanitizer.summarizeError(ex.getMessage()));
            log.error("Stripe webhook processing failed. durationMs={}", System.currentTimeMillis() - start, ex);
            throw ex;
        }
    }

    private StripeWebhookCommand normalizeWebhookCommand(String eventId, String type, JsonNode object) throws IOException {
        StripeWebhookCommand command = switch (type) {
            case "invoice_payment.paid", "invoice_payment.payment_failed" -> {
                JsonNode invoice = resolveInvoiceFromPaymentEvent(eventId, type, object);
                yield new StripeWebhookCommand(eventId, type, object, invoice, lookupInvoiceSubscriptionPeriod(invoice, eventId), null);
            }
            case "invoice.payment_succeeded", "invoice.payment_failed" -> new StripeWebhookCommand(eventId, type, object, object, lookupInvoiceSubscriptionPeriod(object, eventId), null);
            default -> new StripeWebhookCommand(eventId, type, object, object, null, null);
        };
        return command.withCurrentSubscription(retrieveCurrentSubscriptionForSensitiveEvent(command));
    }

    private boolean isWebhookLiveModeAllowed(JsonNode root) {
        if (!environment.acceptsProfiles(Profiles.of("prod", "production"))) {
            return true;
        }
        return root.path("livemode").asBoolean(false);
    }

    private Subscription retrieveCurrentSubscriptionForSensitiveEvent(StripeWebhookCommand command) {
        String subscriptionId = subscriptionIdForCommand(command);
        if (!isSensitiveSubscriptionEvent(command.type()) || !StringUtils.hasText(subscriptionId)) {
            return null;
        }
        try {
            Subscription subscription = stripeApiClient.retrieveSubscription(subscriptionId);
            if (subscription == null) {
                log.warn("Stripe subscription reconciliation returned no subscription; falling back to signed payload. eventId={}, type={}, subscriptionId={}", command.eventId(), command.type(), subscriptionId);
                return null;
            }
            if (StringUtils.hasText(subscription.getId()) && !subscriptionId.equals(subscription.getId())) {
                log.warn("Stripe subscription reconciliation id mismatch. eventId={}, type={}, expectedSubscriptionId={}, actualSubscriptionId={}", command.eventId(), command.type(), subscriptionId, subscription.getId());
                Subscription rejected = new Subscription();
                rejected.setId(subscriptionId);
                rejected.setCustomer("__stripe_reconciliation_mismatch__");
                return rejected;
            }
            String eventCustomerId = expectedCustomerId(command);
            if (StringUtils.hasText(eventCustomerId) && StringUtils.hasText(subscription.getCustomer()) && !eventCustomerId.equals(subscription.getCustomer())) {
                log.warn("Stripe subscription reconciliation customer mismatch. eventId={}, type={}, subscriptionId={}", command.eventId(), command.type(), subscriptionId);
            }
            return subscription;
        } catch (StripeException ex) {
            log.warn("Stripe subscription reconciliation failed. eventId={}, type={}, subscriptionId={}, reason={}", command.eventId(), command.type(), subscriptionId, LogSanitizer.summarizeError(ex.getMessage()));
            throw new StripeReconciliationException("Stripe subscription reconciliation failed", ex);
        }
    }

    private boolean isSensitiveSubscriptionEvent(String type) {
        return switch (type) {
            case "customer.subscription.updated", "customer.subscription.deleted", "invoice_payment.paid", "invoice.payment_succeeded", "invoice_payment.payment_failed", "invoice.payment_failed" -> true;
            default -> false;
        };
    }

    private String expectedCustomerId(StripeWebhookCommand command) {
        if (command.invoice() != null && !command.invoice().isMissingNode() && !command.invoice().isTextual()) {
            return textField(command.invoice(), "customer");
        }
        return textField(command.object(), "customer");
    }

    private boolean claimSubscriptionStateEvent(StripeWebhookCommand command, LocalDateTime stripeEventCreated) {
        String subscriptionId = subscriptionIdForCommand(command);
        if (!StringUtils.hasText(subscriptionId)) {
            return true;
        }
        if (stripeEventCreated == null) {
            log.warn("Stripe state event missing created timestamp; applying without stale check. eventId={}, type={}, subscriptionId={}", command.eventId(), command.type(), subscriptionId);
            return true;
        }
        Optional<StripeSubscriptionState> existing = stripeSubscriptionStateRepository.findBySubscriptionId(subscriptionId);
        if (existing.isPresent()) {
            StripeSubscriptionState state = existing.get();
            if (state.getLastEventCreated() != null && stripeEventCreated.isBefore(state.getLastEventCreated())) {
                log.warn("Stale Stripe state event ignored. eventId={}, type={}, subscriptionId={}, eventCreated={}, lastEventId={}, lastEventType={}, lastEventCreated={}", command.eventId(), command.type(), subscriptionId, stripeEventCreated, state.getLastEventId(), state.getLastEventType(), state.getLastEventCreated());
                return false;
            }
            state.setLastEventId(command.eventId());
            state.setLastEventType(command.type());
            state.setLastEventCreated(stripeEventCreated);
            stripeSubscriptionStateRepository.save(state);
            return true;
        }
        try {
            stripeSubscriptionStateRepository.saveAndFlush(StripeSubscriptionState.create(subscriptionId, command.eventId(), command.type(), stripeEventCreated));
            return true;
        } catch (DataIntegrityViolationException ex) {
            Optional<StripeSubscriptionState> recovered = stripeSubscriptionStateRepository.findBySubscriptionId(subscriptionId);
            if (recovered.isPresent() && recovered.get().getLastEventCreated() != null && stripeEventCreated.isBefore(recovered.get().getLastEventCreated())) {
                log.warn("Stale Stripe state event ignored after concurrent state insert. eventId={}, type={}, subscriptionId={}", command.eventId(), command.type(), subscriptionId);
                return false;
            }
            throw ex;
        }
    }

    private String subscriptionIdForCommand(StripeWebhookCommand command) {
        if ("checkout.session.completed".equals(command.type()) && !"AI_CREDIT_PACK".equals(command.object().path("metadata").path("type").asText())) {
            return subscriptionIdFromValue(command.object().path("subscription"));
        }
        return switch (command.type()) {
            case "customer.subscription.updated", "customer.subscription.deleted" -> command.object().path("id").asText(null);
            case "invoice_payment.paid", "invoice.payment_succeeded", "invoice_payment.payment_failed", "invoice.payment_failed" -> subscriptionIdFromInvoice(command.invoice());
            default -> null;
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
                log.warn("Stripe invoice lookup failed for payment event. eventId={}, type={}, invoiceId={}, reason={}", eventId, type, invoiceId, LogSanitizer.summarizeError(ex.getMessage()));
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

    private void preventDuplicateSubscriptionCheckout(User user) throws StripeException {
        if (hasStripeEntitlement(user.getSubscriptionStatus())) {
            log.warn("Stripe checkout blocked because user already has local subscription entitlement. userId={}, status={}", user.getId(), user.getSubscriptionStatus());
            throw new PlanLimitExceededException("You already have an active Pro subscription.");
        }
        if (StringUtils.hasText(user.getStripeSubscriptionId())) {
            Subscription subscription;
            try {
                subscription = stripeApiClient.retrieveSubscription(user.getStripeSubscriptionId());
            } catch (InvalidRequestException ex) {
                if (!isNoSuchSubscription(ex)) {
                    throw ex;
                }
                user.setStripeSubscriptionId(null);
                subscription = null;
            }
            if (subscription != null && StringUtils.hasText(subscription.getCustomer()) && StringUtils.hasText(user.getStripeCustomerId()) && !user.getStripeCustomerId().equals(subscription.getCustomer())) {
                log.warn("Stripe checkout blocked because stored subscription belongs to a different customer. userId={}", user.getId());
                throw new PlanLimitExceededException("Your billing state needs support review before checkout.");
            }
            if (subscription != null && hasStripeEntitlement(mapSubscriptionStatus(subscription.getStatus(), Boolean.TRUE.equals(subscription.getCancelAtPeriodEnd())))) {
                applyCurrentSubscriptionState(user, subscription);
                userRepository.save(user);
                usageLimitService.invalidateUsageCache(user.getId());
                log.warn("Stripe checkout blocked because Stripe reports an eligible existing subscription. userId={}, subscriptionId={}", user.getId(), subscription.getId());
                throw new PlanLimitExceededException("You already have an active Pro subscription.");
            }
        }
    }

    private Optional<String> reconcileExistingSubscriptionCheckoutAttempt(User user, String billingCycle) throws StripeException {
        Optional<StripeCheckoutAttempt> existing = stripeCheckoutAttemptRepository.findByUserId(user.getId());
        if (existing.isEmpty()) {
            return Optional.empty();
        }
        StripeCheckoutAttempt attempt = existing.get();
        String sessionId = attempt.getStripeCheckoutSessionId();
        if (!StringUtils.hasText(sessionId)) {
            if (attempt.getStatus() == StripeCheckoutAttemptStatus.PENDING && isCheckoutCreationInProgress(attempt)) {
                log.warn("Stripe checkout creation still in progress. userId={}", user.getId());
                throw new PlanLimitExceededException("Your checkout is being prepared. Please try again shortly.");
            }
            log.info("Stripe checkout attempt recycled because it has no usable session id. userId={}", user.getId());
            return Optional.empty();
        }

        Session session;
        try {
            session = stripeApiClient.retrieveCheckoutSession(sessionId);
        } catch (InvalidRequestException ex) {
            if (!isNoSuchCheckoutSession(ex)) {
                throw ex;
            }
            log.info("Stripe checkout attempt recycled because its session no longer exists. userId={}, sessionId={}", user.getId(), sessionId);
            return Optional.empty();
        }
        if (!isExpectedSubscriptionCheckout(session, user)) {
            expireCheckoutSession(sessionId, user.getId(), "unexpected_session");
            return Optional.empty();
        }

        String sessionStatus = session.getStatus();
        if ("open".equals(sessionStatus)) {
            if (!billingCycle.equals(attempt.getBillingCycle()) || !StringUtils.hasText(session.getUrl())) {
                expireCheckoutSession(sessionId, user.getId(), "cycle_mismatch_or_missing_url");
                return Optional.empty();
            }
            log.info("Stripe checkout session reused. userId={}, sessionId={}, billingCycle={}", user.getId(), sessionId, billingCycle);
            return Optional.of(session.getUrl());
        }
        if ("complete".equals(sessionStatus)) {
            reconcileCompletedSubscriptionCheckoutAttempt(user, attempt, session);
        }
        if ("expired".equals(sessionStatus)) {
            log.info("Stripe checkout attempt recycled because its session expired. userId={}, sessionId={}", user.getId(), sessionId);
            return Optional.empty();
        }
        log.warn("Stripe checkout session has an unsupported status. userId={}, sessionId={}, status={}", user.getId(), sessionId, sessionStatus);
        throw new PlanLimitExceededException("Your existing checkout is still being processed. Please refresh and try again shortly.");
    }

    private StripeCheckoutAttempt claimSubscriptionCheckoutAttempt(Long userId, String billingCycle) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = now.plus(SUBSCRIPTION_CHECKOUT_CREATION_LOCK_TTL);
        Optional<StripeCheckoutAttempt> existing = stripeCheckoutAttemptRepository.findByUserId(userId);
        if (existing.isEmpty()) {
            StripeCheckoutAttempt attempt = StripeCheckoutAttempt.pending(userId, expiresAt);
            attempt.setBillingCycle(billingCycle);
            return stripeCheckoutAttemptRepository.saveAndFlush(attempt);
        }
        StripeCheckoutAttempt attempt = existing.get();
        attempt.setStatus(StripeCheckoutAttemptStatus.PENDING);
        attempt.setExpiresAt(expiresAt);
        attempt.setStripeCheckoutSessionId(null);
        attempt.setBillingCycle(billingCycle);
        return stripeCheckoutAttemptRepository.saveAndFlush(attempt);
    }

    private boolean isCheckoutCreationInProgress(StripeCheckoutAttempt attempt) {
        LocalDateTime updatedAt = attempt.getUpdatedAt() == null ? attempt.getCreatedAt() : attempt.getUpdatedAt();
        LocalDateTime now = LocalDateTime.now();
        return updatedAt != null
                && updatedAt.plus(SUBSCRIPTION_CHECKOUT_CREATION_LOCK_TTL).isAfter(now)
                && attempt.getExpiresAt() != null
                && attempt.getExpiresAt().isAfter(now);
    }

    private boolean isExpectedSubscriptionCheckout(Session session, User user) {
        if (!"subscription".equals(session.getMode())) {
            log.warn("Stripe checkout attempt has an unexpected mode. userId={}, sessionId={}, mode={}", user.getId(), session.getId(), session.getMode());
            return false;
        }
        if (StringUtils.hasText(user.getStripeCustomerId()) && StringUtils.hasText(session.getCustomer()) && !user.getStripeCustomerId().equals(session.getCustomer())) {
            log.warn("Stripe checkout attempt customer mismatch. userId={}, sessionId={}", user.getId(), session.getId());
            throw new PlanLimitExceededException("Your billing state needs support review before checkout.");
        }
        return true;
    }

    private void expireCheckoutSession(String sessionId, Long userId, String reason) throws StripeException {
        stripeApiClient.expireCheckoutSession(sessionId);
        log.info("Stripe checkout session expired before replacement. userId={}, sessionId={}, reason={}", userId, sessionId, reason);
    }

    private void reconcileCompletedSubscriptionCheckoutAttempt(User user, StripeCheckoutAttempt attempt, Session session) throws StripeException {
        attempt.setStatus(StripeCheckoutAttemptStatus.COMPLETED);
        stripeCheckoutAttemptRepository.save(attempt);
        String subscriptionId = session.getSubscription();
        if (StringUtils.hasText(subscriptionId)) {
            Subscription subscription = stripeApiClient.retrieveSubscription(subscriptionId);
            if (subscription != null) {
                if (StringUtils.hasText(subscription.getCustomer()) && StringUtils.hasText(user.getStripeCustomerId()) && !user.getStripeCustomerId().equals(subscription.getCustomer())) {
                    log.warn("Completed Stripe checkout subscription customer mismatch. userId={}, sessionId={}, subscriptionId={}", user.getId(), session.getId(), subscriptionId);
                    throw new PlanLimitExceededException("Your billing state needs support review before checkout.");
                }
                applyCurrentSubscriptionState(user, subscription);
                userRepository.save(user);
                usageLimitService.invalidateUsageCache(user.getId());
            }
        }
        log.info("Completed Stripe checkout detected; duplicate checkout prevented. userId={}, sessionId={}", user.getId(), session.getId());
        throw new PlanLimitExceededException("Your Pro checkout is complete. Please refresh your account.");
    }

    private boolean isNoSuchSubscription(InvalidRequestException ex) {
        return ex.getMessage() != null && ex.getMessage().contains("No such subscription");
    }

    private boolean isNoSuchCheckoutSession(InvalidRequestException ex) {
        String message = ex.getMessage();
        return message != null && message.toLowerCase(Locale.ROOT).contains("no such checkout.session");
    }

    void handleCheckoutCompleted(JsonNode object) {
        if ("AI_CREDIT_PACK".equals(object.path("metadata").path("type").asText())) {
            handleCreditPackCheckoutCompleted(object);
            return;
        }
        if (!"subscription".equals(object.path("mode").asText())) {
            log.warn("Stripe checkout completed ignored because it is not a subscription session. sessionId={}, mode={}", textField(object, "id"), object.path("mode").asText(null));
            return;
        }
        Long userId = object.path("metadata").path("userId").asLong(0);
        if (userId == 0) {
            log.warn("Stripe checkout completed without userId metadata.");
            return;
        }
        String subscriptionId = subscriptionIdFromValue(object.path("subscription"));
        if (!StringUtils.hasText(subscriptionId)) {
            log.warn("Stripe subscription checkout completed without a subscription id. sessionId={}, userId={}", textField(object, "id"), userId);
            return;
        }
        Subscription subscription = retrieveSubscriptionForCheckout(subscriptionId, textField(object, "id"));
        if (subscription == null) {
            return;
        }
        userRepository.findByIdForUpdate(userId).ifPresentOrElse(user -> {
            if (!isValidProSubscriptionCheckout(object, subscription, user)) {
                return;
            }
            applyCurrentSubscriptionState(user, subscription);
            userRepository.save(user);
            stripeCheckoutAttemptRepository.findByUserId(user.getId()).ifPresent(attempt -> {
                attempt.setStatus(StripeCheckoutAttemptStatus.COMPLETED);
                attempt.setStripeCheckoutSessionId(textField(object, "id"));
                stripeCheckoutAttemptRepository.save(attempt);
            });
            usageLimitService.invalidateUsageCache(user.getId());
            seoAnalyticsService.markProConverted(user, LocalDateTime.now());
            trackSubscriptionCompletedAndActivated(user, textField(object, "id"), subscription);
            log.info("Stripe checkout completed upgraded user. userId={}, subscriptionId={}, status={}", user.getId(), subscription.getId(), subscription.getStatus());
        }, () -> log.warn("Stripe subscription checkout completed for unknown user. sessionId={}, userId={}", textField(object, "id"), userId));
    }

    private Subscription retrieveSubscriptionForCheckout(String subscriptionId, String sessionId) {
        try {
            Subscription subscription = stripeApiClient.retrieveSubscription(subscriptionId);
            if (subscription == null || !subscriptionId.equals(subscription.getId())) {
                log.warn("Stripe subscription checkout reconciliation failed identity validation. sessionId={}, expectedSubscriptionId={}, actualSubscriptionId={}", sessionId, subscriptionId, subscription == null ? null : subscription.getId());
                return null;
            }
            return subscription;
        } catch (StripeException ex) {
            log.warn("Stripe subscription checkout reconciliation failed. sessionId={}, subscriptionId={}, reason={}", sessionId, subscriptionId, LogSanitizer.summarizeError(ex.getMessage()));
            throw new StripeReconciliationException("Stripe subscription checkout reconciliation failed", ex);
        }
    }

    private boolean isValidProSubscriptionCheckout(JsonNode session, Subscription subscription, User user) {
        String sessionCustomerId = textField(session, "customer");
        if (!StringUtils.hasText(user.getStripeCustomerId())
                || !user.getStripeCustomerId().equals(sessionCustomerId)
                || !user.getStripeCustomerId().equals(subscription.getCustomer())) {
            log.warn("Stripe subscription checkout customer validation failed. sessionId={}, userId={}, subscriptionId={}", textField(session, "id"), user.getId(), subscription.getId());
            return false;
        }
        if (!hasConfiguredProPrice(subscription)) {
            log.warn("Stripe subscription checkout price validation failed. sessionId={}, userId={}, subscriptionId={}", textField(session, "id"), user.getId(), subscription.getId());
            return false;
        }
        SubscriptionStatus subscriptionStatus = mapSubscriptionStatus(subscription.getStatus(), Boolean.TRUE.equals(subscription.getCancelAtPeriodEnd()));
        if (subscriptionStatus != SubscriptionStatus.ACTIVE && subscriptionStatus != SubscriptionStatus.TRIALING) {
            log.warn("Stripe subscription checkout status validation failed. sessionId={}, userId={}, subscriptionId={}, status={}", textField(session, "id"), user.getId(), subscription.getId(), subscription.getStatus());
            return false;
        }
        return true;
    }

    private boolean hasConfiguredProPrice(Subscription subscription) {
        List<com.stripe.model.SubscriptionItem> items = subscription.getItems() == null ? List.of() : subscription.getItems().getData();
        if (items == null) {
            return false;
        }
        return items.stream()
                .map(com.stripe.model.SubscriptionItem::getPrice)
                .filter(java.util.Objects::nonNull)
                .map(com.stripe.model.Price::getId)
                .anyMatch(this::isConfiguredProPriceId);
    }

    private boolean isConfiguredProPriceId(String priceId) {
        return StringUtils.hasText(priceId)
                && (priceId.equals(stripeProperties.getProPriceId()) || priceId.equals(stripeProperties.getProYearlyPriceId()));
    }

    void handleCreditPackCheckoutCompleted(JsonNode object) {
        String sessionId = object.path("id").asText(null);
        if (!StringUtils.hasText(sessionId)) {
            log.warn("Stripe AI credit pack checkout completed without session id.");
            return;
        }
        JsonNode metadata = object.path("metadata");
        Long userId = metadata.path("userId").asLong(0);
        String packId = metadata.path("packId").asText(null);
        if (userId == 0 || !StringUtils.hasText(packId)) {
            log.warn("Stripe AI credit pack checkout missing metadata. sessionId={}, userId={}, packId={}", sessionId, userId, packId);
            return;
        }
        AiCreditPack pack;
        try {
            pack = AiCreditPack.valueOf(packId);
        } catch (IllegalArgumentException ex) {
            log.warn("Stripe AI credit pack checkout had unsupported pack id. sessionId={}, packId={}", sessionId, packId);
            return;
        }
        if (!isValidCreditPackCheckout(object, pack)) {
            log.warn("Stripe AI credit pack checkout failed catalog validation. sessionId={}, packId={}", sessionId, pack.name());
            return;
        }
        userRepository.findByIdForUpdate(userId).ifPresentOrElse(user -> {
            if (aiCreditPurchaseRepository.existsByStripeCheckoutSessionId(sessionId)) {
                log.info("Stripe AI credit pack checkout already applied. sessionId={}", sessionId);
                return;
            }
            if (!StringUtils.hasText(user.getStripeCustomerId()) || !user.getStripeCustomerId().equals(textField(object, "customer"))) {
                log.warn("Stripe AI credit pack checkout customer mismatch. sessionId={}, userId={}", sessionId, user.getId());
                return;
            }
            AiCreditPurchase purchase = AiCreditPurchase.builder().userId(user.getId()).stripeCheckoutSessionId(sessionId).stripeCustomerId(user.getStripeCustomerId()).stripePaymentIntentId(textField(object, "payment_intent")).packId(pack.name()).creditsPurchased(pack.credits()).amountCents(pack.amountCents()).refundedAmountCents(0).reversedCredits(0).currency(pack.currency()).status(AiCreditPurchaseStatus.COMPLETED).completedAt(LocalDateTime.now()).build();
            try {
                aiCreditPurchaseRepository.saveAndFlush(purchase);
                afterCreditPackFulfillmentClaimed.run();
            } catch (DataIntegrityViolationException ex) {
                if (aiCreditPurchaseRepository.existsByStripeCheckoutSessionId(sessionId)) {
                    log.info("Stripe AI credit pack checkout concurrently applied. sessionId={}", sessionId);
                    return;
                }
                throw ex;
            }
            int currentBalance = user.getPurchasedAiCreditsBalance() == null ? 0 : Math.max(0, user.getPurchasedAiCreditsBalance());
            user.setPurchasedAiCreditsBalance(currentBalance + pack.credits());
            userRepository.save(user);
            afterCreditPackCreditsIncremented.run();
            usageLimitService.invalidateUsageCache(user.getId());
            trackCreditPackCompleted(user, sessionId, pack, "checkout.session.completed");
            log.info("Stripe AI credit pack applied. userId={}, sessionId={}, packId={}, credits={}, newBalance={}", user.getId(), sessionId, pack.name(), pack.credits(), user.getPurchasedAiCreditsBalance());
        }, () -> log.warn("Stripe AI credit pack checkout could not be mapped to user. sessionId={}, userId={}", sessionId, userId));
    }

    void handleChargeRefunded(JsonNode charge) {
        String paymentIntentId = textField(charge, "payment_intent");
        String chargeId = textField(charge, "id");
        String customerId = textField(charge, "customer");
        Optional<AiCreditPurchase> purchase = findPurchaseForChargeEvent(paymentIntentId, chargeId);
        if (purchase.isEmpty()) {
            log.warn("Stripe refund ignored because no credit-pack purchase matched. chargeId={}, paymentIntentId={}", chargeId, paymentIntentId);
            return;
        }
        AiCreditPurchase creditPurchase = purchase.get();
        if (!purchaseCustomerMatches(creditPurchase, customerId)) {
            log.warn("Stripe refund ignored due to customer mismatch. purchaseId={}, chargeId={}", creditPurchase.getId(), chargeId);
            return;
        }
        attachChargeId(creditPurchase, chargeId);
        int refundedAmount = Math.max(0, (int) charge.path("amount_refunded").asLong(0));
        creditPurchase.setRefundedAmountCents(Math.max(nonNull(creditPurchase.getRefundedAmountCents()), refundedAmount));
        if (creditPurchase.getRefundedAmountCents() >= nonNull(creditPurchase.getAmountCents())) {
            applyReasonAwareReversal(creditPurchase, nonNull(creditPurchase.getCreditsPurchased()), nonNull(creditPurchase.getDisputeReversalCredits()), nonNull(creditPurchase.getDisputeRestoredCredits()), AiCreditPurchaseStatus.REFUNDED, "full_refund");
            creditPurchase.setRefundedAt(LocalDateTime.now());
        } else if (creditPurchase.getRefundedAmountCents() > 0 && creditPurchase.getStatus() == AiCreditPurchaseStatus.COMPLETED) {
            creditPurchase.setStatus(AiCreditPurchaseStatus.PARTIALLY_REFUNDED);
            creditPurchase.setRefundedAt(LocalDateTime.now());
            log.warn("Stripe partial refund recorded for manual credit reconciliation. purchaseId={}, refundedAmountCents={}", creditPurchase.getId(), creditPurchase.getRefundedAmountCents());
        }
        aiCreditPurchaseRepository.save(creditPurchase);
    }

    void handleChargeDispute(JsonNode dispute) {
        JsonNode charge = dispute.path("charge");
        String chargeId = textField(dispute, "charge");
        String paymentIntentId = textField(dispute, "payment_intent");
        String customerId = textField(dispute, "customer");
        if (!StringUtils.hasText(paymentIntentId) && charge.isObject()) {
            paymentIntentId = textField(charge, "payment_intent");
        }
        if (!StringUtils.hasText(customerId) && charge.isObject()) {
            customerId = textField(charge, "customer");
        }
        Optional<AiCreditPurchase> purchase = findPurchaseForChargeEvent(paymentIntentId, chargeId);
        if (purchase.isEmpty()) {
            log.warn("Stripe dispute ignored because no credit-pack purchase matched. chargeId={}, paymentIntentId={}", chargeId, paymentIntentId);
            return;
        }
        AiCreditPurchase creditPurchase = purchase.get();
        if (!purchaseCustomerMatches(creditPurchase, customerId)) {
            log.warn("Stripe dispute ignored due to customer mismatch. purchaseId={}, chargeId={}", creditPurchase.getId(), chargeId);
            return;
        }
        attachChargeId(creditPurchase, chargeId);
        String disputeStatus = dispute.path("status").asText("");
        creditPurchase.setDisputeStatus(disputeStatus);
        creditPurchase.setDisputedAt(LocalDateTime.now());
        if ("won".equals(disputeStatus)) {
            restoreDisputeReversal(creditPurchase);
        } else if ("lost".equals(disputeStatus)) {
            applyReasonAwareReversal(creditPurchase, nonNull(creditPurchase.getRefundReversalCredits()), nonNull(creditPurchase.getCreditsPurchased()), nonNull(creditPurchase.getDisputeRestoredCredits()), AiCreditPurchaseStatus.DISPUTE_LOST, "dispute_lost");
        } else {
            applyReasonAwareReversal(creditPurchase, nonNull(creditPurchase.getRefundReversalCredits()), nonNull(creditPurchase.getCreditsPurchased()), nonNull(creditPurchase.getDisputeRestoredCredits()), AiCreditPurchaseStatus.DISPUTED, "dispute_created");
        }
        aiCreditPurchaseRepository.save(creditPurchase);
    }

    private Optional<AiCreditPurchase> findPurchaseForChargeEvent(String paymentIntentId, String chargeId) {
        if (StringUtils.hasText(paymentIntentId)) {
            Optional<AiCreditPurchase> byPaymentIntent = aiCreditPurchaseRepository.findByStripePaymentIntentIdForUpdate(paymentIntentId);
            if (byPaymentIntent.isPresent()) {
                return byPaymentIntent;
            }
        }
        if (StringUtils.hasText(chargeId)) {
            return aiCreditPurchaseRepository.findByStripeChargeIdForUpdate(chargeId);
        }
        return Optional.empty();
    }

    private boolean purchaseCustomerMatches(AiCreditPurchase purchase, String customerId) {
        return !StringUtils.hasText(purchase.getStripeCustomerId())
                || (StringUtils.hasText(customerId) && customerId.equals(purchase.getStripeCustomerId()));
    }

    private void attachChargeId(AiCreditPurchase purchase, String chargeId) {
        if (StringUtils.hasText(chargeId) && !StringUtils.hasText(purchase.getStripeChargeId())) {
            purchase.setStripeChargeId(chargeId);
        }
    }

    private void applyReasonAwareReversal(AiCreditPurchase purchase, int refundReversalCredits, int disputeReversalCredits, int disputeRestoredCredits, AiCreditPurchaseStatus status, String reason) {
        int previousEconomicReversal = economicReversalCredits(purchase);
        purchase.setRefundReversalCredits(Math.max(0, refundReversalCredits));
        purchase.setDisputeReversalCredits(Math.max(0, disputeReversalCredits));
        purchase.setDisputeRestoredCredits(Math.min(Math.max(0, disputeRestoredCredits), nonNull(purchase.getDisputeReversalCredits())));
        int newEconomicReversal = economicReversalCredits(purchase);
        int delta = newEconomicReversal - previousEconomicReversal;
        purchase.setStatus(status);
        if (delta == 0) {
            purchase.setReversedCredits(newEconomicReversal);
            return;
        }
        userRepository.findByIdForUpdate(purchase.getUserId()).ifPresent(user -> {
            int currentBalance = user.getPurchasedAiCreditsBalance() == null ? 0 : user.getPurchasedAiCreditsBalance();
            user.setPurchasedAiCreditsBalance(currentBalance - delta);
            userRepository.save(user);
            usageLimitService.invalidateUsageCache(user.getId());
        });
        purchase.setReversedCredits(newEconomicReversal);
        purchase.setReversedAt(LocalDateTime.now());
        log.warn("Stripe credit-pack reversal applied. purchaseId={}, credits={}, reason={}", purchase.getId(), delta, reason);
    }

    private void restoreDisputeReversal(AiCreditPurchase purchase) {
        int previousEconomicReversal = economicReversalCredits(purchase);
        purchase.setDisputeRestoredCredits(nonNull(purchase.getDisputeReversalCredits()));
        int newEconomicReversal = economicReversalCredits(purchase);
        int restored = previousEconomicReversal - newEconomicReversal;
        purchase.setStatus(nonNull(purchase.getRefundReversalCredits()) > 0 ? AiCreditPurchaseStatus.REFUNDED : AiCreditPurchaseStatus.DISPUTE_WON);
        purchase.setReversedCredits(newEconomicReversal);
        if (restored <= 0) {
            return;
        }
        userRepository.findByIdForUpdate(purchase.getUserId()).ifPresent(user -> {
            int currentBalance = user.getPurchasedAiCreditsBalance() == null ? 0 : user.getPurchasedAiCreditsBalance();
            user.setPurchasedAiCreditsBalance(currentBalance + restored);
            userRepository.save(user);
            usageLimitService.invalidateUsageCache(user.getId());
        });
        purchase.setReversedAt(LocalDateTime.now());
        log.info("Stripe dispute won; dispute-reversed purchased credits restored. purchaseId={}, credits={}", purchase.getId(), restored);
    }

    private int economicReversalCredits(AiCreditPurchase purchase) {
        return Math.max(nonNull(purchase.getRefundReversalCredits()), activeDisputeReversal(purchase));
    }

    private int activeDisputeReversal(AiCreditPurchase purchase) {
        return Math.max(0, nonNull(purchase.getDisputeReversalCredits()) - nonNull(purchase.getDisputeRestoredCredits()));
    }

    private int nonNull(Integer value) {
        return value == null ? 0 : value;
    }

    private boolean isValidCreditPackCheckout(JsonNode object, AiCreditPack pack) {
        return "payment".equals(object.path("mode").asText(""))
                && "paid".equals(object.path("payment_status").asText(""))
                && pack.currency().equalsIgnoreCase(object.path("currency").asText(""))
                && object.path("amount_total").asLong(-1) == pack.amountCents();
    }

    void handleSubscriptionUpdated(JsonNode object, Subscription currentSubscription) {
        logSubscriptionFields("customer.subscription.updated", object);
        findUserForSubscription(object, currentSubscription).ifPresent(user -> {
            if (currentSubscription != null) {
                applyCurrentSubscriptionState(user, currentSubscription);
            } else {
                String status = object.path("status").asText("");
                boolean cancelAtPeriodEnd = object.path("cancel_at_period_end").asBoolean(false);
                SubscriptionStatus subscriptionStatus = mapSubscriptionStatus(status, cancelAtPeriodEnd);
                user.setSubscriptionStatus(subscriptionStatus);
                user.setPlan(hasStripeEntitlement(subscriptionStatus) ? PlanType.PRO : PlanType.FREE);
                applyStripeIds(user, object.path("customer").asText(null), object.path("id").asText(null));
                applySubscriptionPeriod(user, object);
                applySubscriptionCancellationMetadata(user, object);
            }
            userRepository.save(user);
            usageLimitService.invalidateUsageCache(user.getId());
            log.info("Stripe subscription updated. userId={}, status={}, plan={}, sourceOfTruth={}", user.getId(), user.getSubscriptionStatus(), user.getPlan(), currentSubscription != null);
        });
    }

    void handleSubscriptionDeleted(JsonNode object, Subscription currentSubscription) {
        logSubscriptionFields("customer.subscription.deleted", object);
        findUserForSubscription(object, currentSubscription).ifPresent(user -> {
            if (currentSubscription != null) {
                applyCurrentSubscriptionState(user, currentSubscription);
            } else {
                user.setPlan(PlanType.FREE);
                user.setSubscriptionStatus(SubscriptionStatus.CANCELED);
                applyStripeIds(user, object.path("customer").asText(null), object.path("id").asText(null));
                applySubscriptionPeriod(user, object);
                applySubscriptionCancellationMetadata(user, object);
            }
            userRepository.save(user);
            usageLimitService.invalidateUsageCache(user.getId());
            log.info("Stripe subscription deleted. userId={}, status={}, plan={}, sourceOfTruth={}", user.getId(), user.getSubscriptionStatus(), user.getPlan(), currentSubscription != null);
        });
    }

    void handleInvoicePaymentSucceeded(String eventId, JsonNode invoice, Period invoiceSubscriptionPeriod, Subscription currentSubscription) {
        if (invoice == null || invoice.isMissingNode() || invoice.isNull() || invoice.isTextual()) {
            log.warn("Stripe paid invoice event did not include supported invoice details. eventId={}, invoiceId={}", eventId, invoice == null ? null : invoice.asText(null));
            return;
        }
        findUserForInvoice(invoice, currentSubscription).ifPresentOrElse(user -> {
            if (currentSubscription != null) {
                applyCurrentSubscriptionState(user, currentSubscription);
            } else {
                user.setPlan(PlanType.PRO);
                user.setSubscriptionStatus(SubscriptionStatus.ACTIVE);
                applyStripeIds(user, textField(invoice, "customer"), subscriptionIdFromInvoice(invoice));
                applyInvoicePeriod(user, invoice, eventId, invoiceSubscriptionPeriod);
                clearSubscriptionCancellationMetadata(user);
            }
            userRepository.save(user);
            usageLimitService.invalidateUsageCache(user.getId());
            log.info("Stripe invoice payment confirmed subscription state. userId={}, status={}, plan={}, sourceOfTruth={}", user.getId(), user.getSubscriptionStatus(), user.getPlan(), currentSubscription != null);
        }, () -> log.warn("Stripe paid invoice could not be mapped to a TradePilot user. invoiceId={}", textField(invoice, "id")));
    }

    void handleInvoicePaymentFailed(String eventId, JsonNode invoice, Period invoiceSubscriptionPeriod, Subscription currentSubscription) {
        if (invoice.isMissingNode() || invoice.isNull() || invoice.isTextual()) {
            log.warn("Stripe invoice payment failed event did not include expanded invoice details. eventId={}, invoiceId={}", eventId, invoice.asText(null));
            return;
        }
        findUserForInvoice(invoice, currentSubscription).ifPresentOrElse(user -> {
            if (currentSubscription != null) {
                applyCurrentSubscriptionState(user, currentSubscription);
            } else {
                user.setPlan(PlanType.PRO);
                user.setSubscriptionStatus(SubscriptionStatus.PAST_DUE);
                applyStripeIds(user, textField(invoice, "customer"), subscriptionIdFromInvoice(invoice));
                applyInvoicePeriod(user, invoice, eventId, invoiceSubscriptionPeriod);
            }
            userRepository.save(user);
            usageLimitService.invalidateUsageCache(user.getId());
            log.warn("Stripe invoice payment failed; reconciled subscription state. userId={}, status={}, plan={}, sourceOfTruth={}", user.getId(), user.getSubscriptionStatus(), user.getPlan(), currentSubscription != null);
        }, () -> log.warn("Stripe failed invoice could not be mapped to a TradePilot user. invoiceId={}", textField(invoice, "id")));
    }

    private Optional<User> findUserForSubscription(JsonNode object, Subscription currentSubscription) {
        if (currentSubscription != null) {
            return findUserForCurrentSubscription(currentSubscription);
        }
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

    private Optional<User> findUserForInvoice(JsonNode invoice, Subscription currentSubscription) {
        if (currentSubscription != null) {
            return findUserForCurrentSubscription(currentSubscription);
        }
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

    private Optional<User> findUserForCurrentSubscription(Subscription subscription) {
        if (StringUtils.hasText(subscription.getId())) {
            Optional<User> bySubscription = userRepository.findByStripeSubscriptionId(subscription.getId());
            if (bySubscription.isPresent()) {
                if (StringUtils.hasText(subscription.getCustomer()) && StringUtils.hasText(bySubscription.get().getStripeCustomerId()) && !subscription.getCustomer().equals(bySubscription.get().getStripeCustomerId())) {
                    log.warn("Stripe subscription source-of-truth customer mismatch for stored user. userId={}, subscriptionId={}", bySubscription.get().getId(), subscription.getId());
                    return Optional.empty();
                }
                return bySubscription;
            }
        }
        if (StringUtils.hasText(subscription.getCustomer())) {
            return userRepository.findByStripeCustomerId(subscription.getCustomer());
        }
        return Optional.empty();
    }

    private void applyCurrentSubscriptionState(User user, Subscription subscription) {
        SubscriptionStatus subscriptionStatus = mapSubscriptionStatus(subscription.getStatus(), Boolean.TRUE.equals(subscription.getCancelAtPeriodEnd()));
        user.setSubscriptionStatus(subscriptionStatus);
        user.setPlan(hasStripeEntitlement(subscriptionStatus) ? PlanType.PRO : PlanType.FREE);
        applyStripeIds(user, subscription.getCustomer(), subscription.getId());
        setPeriodStart(user, subscription.getCurrentPeriodStart() == null ? 0 : subscription.getCurrentPeriodStart());
        setPeriodEnd(user, firstPositiveNullable(subscription.getCurrentPeriodEnd(), subscription.getCancelAt() == null ? 0 : subscription.getCancelAt()));
        user.setStripeCancelAtPeriodEnd(Boolean.TRUE.equals(subscription.getCancelAtPeriodEnd()));
        user.setStripeCancelAt(epochToLocalDateTime(subscription.getCancelAt() == null ? 0 : subscription.getCancelAt()));
        user.setStripeCanceledAt(epochToLocalDateTime(subscription.getCanceledAt() == null ? 0 : subscription.getCanceledAt()));
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
        Optional<Period> period = Optional.ofNullable(invoiceSubscriptionPeriod).filter(value -> isValidPeriod(value.start(), value.end())).or(() -> periodFromInvoiceLines(invoice));
        if (period.isEmpty()) {
            log.warn("Stripe invoice period not updated because no valid subscription or invoice line period was available. eventId={}, invoiceId={}", eventId, textField(invoice, "id"));
            return;
        }
        if (!isValidPeriod(period.get().start(), period.get().end())) {
            log.warn("Stripe invoice period not updated because extracted period is invalid. eventId={}, invoiceId={}, start={}, end={}", eventId, textField(invoice, "id"), period.get().start(), period.get().end());
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
                log.warn("Stripe subscription period lookup returned no subscription. eventId={}, invoiceId={}, subscriptionId={}", eventId, textField(invoice, "id"), subscriptionId);
                return null;
            }
            Period period = new Period(firstPositiveNullable(subscription.getCurrentPeriodStart(), subscriptionPeriodStart(invoice.path("subscription"))), firstPositiveNullable(subscription.getCurrentPeriodEnd(), subscriptionPeriodEnd(invoice.path("subscription"))));
            if (isValidPeriod(period.start(), period.end())) {
                return period;
            }
            log.warn("Stripe subscription period from retrieved subscription is invalid. eventId={}, invoiceId={}, subscriptionId={}, start={}, end={}", eventId, textField(invoice, "id"), subscriptionId, period.start(), period.end());
        } catch (StripeException ex) {
            log.warn("Stripe subscription period lookup failed for invoice. eventId={}, invoiceId={}, subscriptionId={}, reason={}", eventId, textField(invoice, "id"), subscriptionId, LogSanitizer.summarizeError(ex.getMessage()));
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
        return firstPositive(subscription.path("current_period_start").asLong(0), subscription.path("items").path("data").path(0).path("current_period_start").asLong(0));
    }

    private long subscriptionPeriodEnd(JsonNode subscription) {
        return firstPositive(subscription.path("current_period_end").asLong(0), subscription.path("items").path("data").path(0).path("current_period_end").asLong(0), subscription.path("cancel_at").asLong(0));
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

    private LocalDateTime stripeEventCreated(JsonNode root) {
        return epochToLocalDateTime(root.path("created").asLong(0));
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
        String normalizedStatus = stripeStatus == null ? "" : stripeStatus;
        if (cancelAtPeriodEnd && ("active".equals(normalizedStatus) || "trialing".equals(normalizedStatus))) {
            return "trialing".equals(normalizedStatus) ? SubscriptionStatus.TRIALING : SubscriptionStatus.ACTIVE;
        }
        return switch (normalizedStatus) {
            case "active" -> SubscriptionStatus.ACTIVE;
            case "trialing" -> SubscriptionStatus.TRIALING;
            case "past_due" -> SubscriptionStatus.PAST_DUE;
            case "canceled", "unpaid", "incomplete_expired" -> SubscriptionStatus.CANCELED;
            default -> SubscriptionStatus.NONE;
        };
    }

    private boolean hasStripeEntitlement(SubscriptionStatus status) {
        return status == SubscriptionStatus.ACTIVE || status == SubscriptionStatus.TRIALING || status == SubscriptionStatus.PAST_DUE;
    }

    private void logSubscriptionFields(String eventType, JsonNode subscription) {
        log.info("Stripe subscription fields received. eventType={}, subscriptionId={}, customerId={}, status={}, currentPeriodStart={}, currentPeriodEnd={}, itemCurrentPeriodStart={}, itemCurrentPeriodEnd={}, cancelAtPeriodEnd={}, cancelAt={}, canceledAt={}, metadataKeys={}", eventType, textField(subscription, "id"), textField(subscription, "customer"), subscription.path("status").asText(null), subscription.path("current_period_start").asLong(0), subscription.path("current_period_end").asLong(0), subscription.path("items").path("data").path(0).path("current_period_start").asLong(0), subscription.path("items").path("data").path(0).path("current_period_end").asLong(0), subscription.path("cancel_at_period_end").asBoolean(false), subscription.path("cancel_at").asLong(0), subscription.path("canceled_at").asLong(0), metadataKeys(subscription.path("metadata")));
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


    private record StripeWebhookCommand(String eventId, String type, JsonNode object, JsonNode invoice, Period invoiceSubscriptionPeriod, Subscription currentSubscription) {
        private StripeWebhookCommand withCurrentSubscription(Subscription subscription) {
            return new StripeWebhookCommand(eventId, type, object, invoice, invoiceSubscriptionPeriod, subscription);
        }
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
        String priceId = "yearly".equals(normalized) ? stripeProperties.getProYearlyPriceId() : stripeProperties.getProPriceId();
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

    @java.lang.SuppressWarnings("all")
    private static class StripeReconciliationException extends RuntimeException {
        StripeReconciliationException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    @java.lang.SuppressWarnings("all")
    private void trackSubscriptionCheckoutStarted(User user, String sessionId, String billingCycle) {
        String metadata = lifecycleMetadata(Map.of("billingCycle", billingCycle, "source", "STRIPE", "checkoutType", "SUBSCRIPTION"));
        featureUsageService.trackAuthoritative(user, FeatureKey.TRADING_DNA, FeatureUsageAction.UPGRADE_CTA_CLICKED,
                FeatureUsageEventType.USER_ACTION, "STRIPE_CHECKOUT_SESSION", sessionId, "stripe", metadata);
        featureUsageService.trackAuthoritative(user, FeatureKey.PLAYBOOK, FeatureUsageAction.CHECKOUT_STARTED,
                FeatureUsageEventType.USER_ACTION, "STRIPE_CHECKOUT_SESSION", sessionId, "stripe", metadata);
    }

    private void trackSubscriptionCompletedAndActivated(User user, String sessionId, Subscription subscription) {
        Map<String, String> metadata = Map.of("billingCycle", billingCycleForSubscription(subscription), "source", "STRIPE", "stripeEventType", "checkout.session.completed");
        featureUsageService.trackAuthoritative(user, FeatureKey.PLAYBOOK, FeatureUsageAction.CHECKOUT_COMPLETED,
                FeatureUsageEventType.USER_ACTION, "STRIPE_CHECKOUT_SESSION", sessionId, "stripe", lifecycleMetadata(metadata));
        featureUsageService.trackAuthoritative(user, FeatureKey.PLAYBOOK, FeatureUsageAction.SUBSCRIPTION_ACTIVATED,
                FeatureUsageEventType.USER_ACTION, "STRIPE_SUBSCRIPTION", subscription.getId(), "stripe", lifecycleMetadata(metadata));
    }

    private void trackCreditPackCheckoutSelected(User user, String sessionId, AiCreditPack pack) {
        String metadata = lifecycleMetadata(Map.of("credits", pack.credits(), "source", "STRIPE", "checkoutType", "AI_CREDIT_PACK"));
        featureUsageService.trackAuthoritative(user, FeatureKey.DAILY_COACH, FeatureUsageAction.BUY_CREDITS_CTA_CLICKED,
                FeatureUsageEventType.USER_ACTION, "STRIPE_CHECKOUT_SESSION", sessionId, "stripe", metadata);
        featureUsageService.trackAuthoritative(user, FeatureKey.DAILY_COACH, FeatureUsageAction.CREDIT_PACK_SELECTED,
                FeatureUsageEventType.CREDIT_PURCHASE, "STRIPE_CHECKOUT_SESSION", sessionId, "stripe", metadata);
    }

    private void trackCreditPackCompleted(User user, String sessionId, AiCreditPack pack, String stripeEventType) {
        featureUsageService.trackAuthoritative(user, FeatureKey.DAILY_COACH, FeatureUsageAction.CHECKOUT_COMPLETED,
                FeatureUsageEventType.CREDIT_PURCHASE, "STRIPE_CHECKOUT_SESSION", sessionId, "stripe",
                lifecycleMetadata(Map.of("credits", pack.credits(), "source", "STRIPE", "stripeEventType", stripeEventType)));
    }

    private String billingCycleForSubscription(Subscription subscription) {
        return hasPrice(subscription, stripeProperties.getProYearlyPriceId()) ? "YEARLY" : "MONTHLY";
    }

    private boolean hasPrice(Subscription subscription, String priceId) {
        return StringUtils.hasText(priceId) && subscription.getItems() != null && subscription.getItems().getData() != null
                && subscription.getItems().getData().stream().map(com.stripe.model.SubscriptionItem::getPrice)
                .filter(java.util.Objects::nonNull).anyMatch(price -> priceId.equals(price.getId()));
    }

    private String lifecycleMetadata(Map<String, ?> metadata) {
        try {
            return objectMapper.writeValueAsString(metadata);
        } catch (com.fasterxml.jackson.core.JsonProcessingException ex) {
            throw new IllegalStateException("Unable to serialize Stripe lifecycle telemetry", ex);
        }
    }

    public StripeBillingService(final StripeProperties stripeProperties, final UserRepository userRepository, final ObjectMapper objectMapper, final StripeApiClient stripeApiClient, final Environment environment, final TransactionTemplate transactionTemplate, final ObservabilityService observabilityService, final ProcessedStripeEventRepository processedStripeEventRepository, final StripeSubscriptionStateRepository stripeSubscriptionStateRepository, final StripeCheckoutAttemptRepository stripeCheckoutAttemptRepository, final AiCreditPurchaseRepository aiCreditPurchaseRepository, final UsageLimitService usageLimitService, final SeoAnalyticsService seoAnalyticsService, final FeatureUsageService featureUsageService) {
        this.stripeProperties = stripeProperties;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
        this.stripeApiClient = stripeApiClient;
        this.environment = environment;
        this.transactionTemplate = transactionTemplate;
        this.observabilityService = observabilityService;
        this.processedStripeEventRepository = processedStripeEventRepository;
        this.stripeSubscriptionStateRepository = stripeSubscriptionStateRepository;
        this.stripeCheckoutAttemptRepository = stripeCheckoutAttemptRepository;
        this.aiCreditPurchaseRepository = aiCreditPurchaseRepository;
        this.usageLimitService = usageLimitService;
        this.seoAnalyticsService = seoAnalyticsService;
        this.featureUsageService = featureUsageService;
    }
}
